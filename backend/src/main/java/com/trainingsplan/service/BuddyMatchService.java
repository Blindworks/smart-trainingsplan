package com.trainingsplan.service;

import com.trainingsplan.dto.BuddySuggestionDto;
import com.trainingsplan.entity.BuddyRun;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserBuddyPreferences;
import com.trainingsplan.repository.BuddyRunRepository;
import com.trainingsplan.repository.FriendshipRepository;
import com.trainingsplan.repository.UserBuddyPreferencesRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuddyMatchService {

    private final BuddyRunRepository buddyRunRepository;
    private final UserBuddyPreferencesRepository preferencesRepository;
    private final FriendshipRepository friendshipRepository;

    public BuddyMatchService(BuddyRunRepository buddyRunRepository,
                             UserBuddyPreferencesRepository preferencesRepository,
                             FriendshipRepository friendshipRepository) {
        this.buddyRunRepository = buddyRunRepository;
        this.preferencesRepository = preferencesRepository;
        this.friendshipRepository = friendshipRepository;
    }

    /**
     * Hauptflow: Schlaegt dem aktuellen User passende Buddies vor — basierend auf
     * seinen eigenen Buddy-Preferences (Radius, Pace-Toleranz, Verfuegbarkeit) und
     * den Preferences der Kandidaten (die auch `buddyDiscoverable=true` haben muessen).
     * Kein bestehender BuddyRun noetig.
     */
    @Transactional(readOnly = true)
    public List<BuddySuggestionDto> suggestBuddiesForUser(User me, UserBuddyPreferences myPrefs) {
        List<UserBuddyPreferences> candidates = preferencesRepository.findByBuddyDiscoverableTrue();

        Integer myPace = effectivePaceSecPerKm(me, myPrefs);
        double radius = myPrefs != null ? myPrefs.getSearchRadiusKm() : 25.0;
        int tolerancePct = myPrefs != null ? myPrefs.getPaceTolerancePercent() : 20;

        List<BuddySuggestionDto> result = new ArrayList<>();
        for (UserBuddyPreferences pref : candidates) {
            User candidate = pref.getUser();
            if (candidate == null) continue;
            if (candidate.getId().equals(me.getId())) continue;

            // Distance
            Double distanceKm = null;
            if (me.getLatitude() != null && me.getLongitude() != null
                    && candidate.getLatitude() != null && candidate.getLongitude() != null) {
                distanceKm = BuddyRunService.haversineKm(me.getLatitude(), me.getLongitude(),
                        candidate.getLatitude(), candidate.getLongitude());
                if (distanceKm > radius) continue;
            }

            // Pace match: compare both paces with combined tolerance
            Integer candidatePace = effectivePaceSecPerKm(candidate, pref);
            Double paceScore = computeBuddyPaceScore(myPace, candidatePace, tolerancePct);
            if (paceScore != null && paceScore <= 0.0) continue;

            // Availability overlap (best-effort: only filter when BOTH set weekdays)
            if (myPrefs != null && !availabilityOverlap(myPrefs, pref)) continue;

            BuddySuggestionDto dto = new BuddySuggestionDto();
            dto.userId = candidate.getId();
            dto.username = candidate.getUsername();
            dto.profileImageFilename = candidate.getProfileImageFilename();
            dto.city = candidate.getAddressCity();
            dto.distanceKm = distanceKm;
            dto.userPaceSecPerKm = candidatePace;
            dto.paceMatchScore = paceScore != null ? paceScore : 0.5;
            dto.friendshipStatus = friendshipRepository.areAcceptedFriends(me.getId(), candidate.getId())
                    ? "FRIENDS" : "NONE";
            result.add(dto);
        }

        result.sort(Comparator.comparingDouble((BuddySuggestionDto d) -> d.paceMatchScore == null ? 0 : -d.paceMatchScore)
                .thenComparingDouble(d -> d.distanceKm == null ? Double.MAX_VALUE : d.distanceKm));
        return result.stream().limit(30).collect(Collectors.toList());
    }

    private static Double computeBuddyPaceScore(Integer myPace, Integer otherPace, int tolerancePct) {
        if (myPace == null || otherPace == null) return null;
        double tol = myPace * (tolerancePct / 100.0);
        double diff = Math.abs(myPace - otherPace);
        if (diff > tol) return 0.0;
        return 1.0 - (diff / tol);
    }

    private static boolean availabilityOverlap(UserBuddyPreferences a, UserBuddyPreferences b) {
        if (a.getAvailableWeekdays() == null || a.getAvailableWeekdays().isBlank()) return true;
        if (b.getAvailableWeekdays() == null || b.getAvailableWeekdays().isBlank()) return true;
        Set<String> daysA = Arrays.stream(a.getAvailableWeekdays().split(","))
                .map(s -> s.trim().toUpperCase()).collect(Collectors.toSet());
        for (String d : b.getAvailableWeekdays().split(",")) {
            if (daysA.contains(d.trim().toUpperCase())) return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<BuddySuggestionDto> suggestBuddies(Long buddyRunId) {
        BuddyRun run = buddyRunRepository.findById(buddyRunId)
                .orElseThrow(() -> new EntityNotFoundException("BuddyRun not found"));
        List<UserBuddyPreferences> candidates = preferencesRepository.findByBuddyDiscoverableTrue();

        Set<Long> existingParticipantIds = run.getParticipants().stream()
                .filter(p -> p.getUser() != null)
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());

        Long creatorId = run.getCreator() != null ? run.getCreator().getId() : null;

        List<BuddySuggestionDto> result = new ArrayList<>();
        for (UserBuddyPreferences pref : candidates) {
            User candidate = pref.getUser();
            if (candidate == null) continue;
            if (creatorId != null && candidate.getId().equals(creatorId)) continue;
            if (existingParticipantIds.contains(candidate.getId())) continue;

            Double distanceKm = null;
            if (run.getMeetingLatitude() != null && run.getMeetingLongitude() != null
                    && candidate.getLatitude() != null && candidate.getLongitude() != null) {
                distanceKm = BuddyRunService.haversineKm(run.getMeetingLatitude(), run.getMeetingLongitude(),
                        candidate.getLatitude(), candidate.getLongitude());
                if (distanceKm > pref.getSearchRadiusKm()) continue;
            }

            Integer candidatePace = userPaceSecPerKm(candidate);
            Double paceScore = computePaceScore(candidatePace, run.getTargetPaceMinSecPerKm(), run.getTargetPaceMaxSecPerKm(),
                    pref.getPaceTolerancePercent());
            if (paceScore == null || paceScore <= 0.0) continue;

            if (!isAvailable(pref, run.getScheduledAt())) continue;

            BuddySuggestionDto dto = new BuddySuggestionDto();
            dto.userId = candidate.getId();
            dto.username = candidate.getUsername();
            dto.profileImageFilename = candidate.getProfileImageFilename();
            dto.city = candidate.getAddressCity();
            dto.distanceKm = distanceKm;
            dto.userPaceSecPerKm = candidatePace;
            dto.paceMatchScore = paceScore;
            if (creatorId != null) {
                if (friendshipRepository.areAcceptedFriends(creatorId, candidate.getId())) {
                    dto.friendshipStatus = "FRIENDS";
                } else {
                    dto.friendshipStatus = "NONE";
                }
            }
            result.add(dto);
        }

        result.sort(Comparator.comparingDouble((BuddySuggestionDto d) -> d.paceMatchScore == null ? 0 : -d.paceMatchScore)
                .thenComparingDouble(d -> d.distanceKm == null ? Double.MAX_VALUE : d.distanceKm));
        return result.stream().limit(20).collect(Collectors.toList());
    }

    /** Buddy-spezifische Pace falls gesetzt, sonst die globale User-Pace. */
    public static Integer effectivePaceSecPerKm(User u, UserBuddyPreferences prefs) {
        if (prefs != null && prefs.getTargetPaceSecPerKm() != null) {
            return prefs.getTargetPaceSecPerKm();
        }
        return userPaceSecPerKm(u);
    }

    public static Integer userPaceSecPerKm(User u) {
        if (u.getPaceRefTimeSeconds() == null || u.getPaceRefDistanceM() == null || u.getPaceRefDistanceM() <= 0) {
            return null;
        }
        double distKm = u.getPaceRefDistanceM() / 1000.0;
        return (int) Math.round(u.getPaceRefTimeSeconds() / distKm);
    }

    private static Double computePaceScore(Integer userPace, Integer min, Integer max, int tolerancePct) {
        if (userPace == null) return null;
        if (min == null && max == null) return 0.5;
        int lo = min != null ? min : (max != null ? max - 30 : userPace);
        int hi = max != null ? max : (min != null ? min + 30 : userPace);
        int center = (lo + hi) / 2;
        int half = Math.max(1, (hi - lo) / 2);
        double tol = center * (tolerancePct / 100.0);
        double allowed = half + tol;
        double diff = Math.abs(userPace - center);
        if (diff > allowed) return 0.0;
        return 1.0 - (diff / allowed);
    }

    private static boolean isAvailable(UserBuddyPreferences pref, LocalDateTime when) {
        if (pref.getAvailableWeekdays() == null || pref.getAvailableWeekdays().isBlank()) {
            // no restriction set => assume available
        } else {
            String[] tokens = pref.getAvailableWeekdays().split(",");
            String dayToken = switch (when.getDayOfWeek()) {
                case MONDAY -> "MO"; case TUESDAY -> "DI"; case WEDNESDAY -> "MI";
                case THURSDAY -> "DO"; case FRIDAY -> "FR"; case SATURDAY -> "SA"; case SUNDAY -> "SO";
            };
            boolean dayOk = false;
            for (String t : tokens) if (t.trim().equalsIgnoreCase(dayToken)) { dayOk = true; break; }
            if (!dayOk) return false;
        }
        if (pref.getAvailableTimeRanges() != null && !pref.getAvailableTimeRanges().isBlank()) {
            int minute = when.getHour() * 60 + when.getMinute();
            for (String range : pref.getAvailableTimeRanges().split(",")) {
                String[] parts = range.trim().split("-");
                if (parts.length != 2) continue;
                int from = parseMinutes(parts[0]);
                int to = parseMinutes(parts[1]);
                if (from >= 0 && to >= 0 && minute >= from && minute <= to) return true;
            }
            return false;
        }
        return true;
    }

    private static int parseMinutes(String hhmm) {
        try {
            String[] p = hhmm.trim().split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) { return -1; }
    }
}
