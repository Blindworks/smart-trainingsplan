package com.trainingsplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.SegmentChallengeDto;
import com.trainingsplan.dto.SegmentEffortResultDto;
import com.trainingsplan.dto.SegmentLeaderboardEntryDto;
import com.trainingsplan.dto.SegmentTrackDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.SegmentChallengeRepository;
import com.trainingsplan.repository.SegmentEffortRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SegmentChallengeService {

    private static final int MAX_PUBLIC_SUBMITS_PER_IP_PER_HOUR = 20;
    // Plausibility ceilings for the segment effort average speed.
    private static final double MAX_RIDE_KMH = 90.0;
    private static final double MAX_RUN_KMH = 32.0;

    private final SegmentChallengeRepository challengeRepository;
    private final SegmentEffortRepository effortRepository;
    private final GpxParsingService gpxParsingService;
    private final SegmentMatchingService matchingService;
    private final ObjectMapper objectMapper;

    public SegmentChallengeService(SegmentChallengeRepository challengeRepository,
                                   SegmentEffortRepository effortRepository,
                                   GpxParsingService gpxParsingService,
                                   SegmentMatchingService matchingService,
                                   ObjectMapper objectMapper) {
        this.challengeRepository = challengeRepository;
        this.effortRepository = effortRepository;
        this.gpxParsingService = gpxParsingService;
        this.matchingService = matchingService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SegmentChallengeDto getChallenge(String slug) {
        SegmentChallenge c = requireChallenge(slug);
        long rideCount = effortRepository.countByChallengeIdAndActivityTypeAndStatus(
                c.getId(), ActivityType.RIDE, EffortStatus.VALID);
        long runCount = effortRepository.countByChallengeIdAndActivityTypeAndStatus(
                c.getId(), ActivityType.RUN, EffortStatus.VALID);
        return new SegmentChallengeDto(c.getId(), c.getSlug(), c.getName(), c.getSubtitle(),
                c.getEventDate(), c.getDistanceM(), c.getElevationGainM(), c.getAvgGradePct(),
                c.getMaxGradePct(), c.getPolylineJson(), c.getTerrainAssetRef(), rideCount, runCount);
    }

    @Transactional(readOnly = true)
    public List<SegmentLeaderboardEntryDto> getLeaderboard(String slug, ActivityType type) {
        SegmentChallenge c = requireChallenge(slug);
        List<SegmentEffort> efforts = effortRepository
                .findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                        c.getId(), type, EffortStatus.VALID);
        return buildLeaderboard(efforts);
    }

    @Transactional(readOnly = true)
    public SegmentTrackDto getEffortTrack(Long effortId) {
        SegmentEffort e = effortRepository.findById(effortId)
                .orElseThrow(() -> new IllegalArgumentException("effort_not_found"));
        return new SegmentTrackDto(e.getId(), e.getActivityType().name(), e.getTrackJson());
    }

    @Transactional
    public SegmentEffortResultDto submitPublicEffort(String slug, ActivityType type, String displayName,
                                                     byte[] fileBytes, String originalFilename, String clientIp) {
        SegmentChallenge c = requireChallenge(slug);

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("display_name_required");
        }
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".gpx")) {
            throw new IllegalArgumentException("only_gpx_supported");
        }
        String ipHash = hashIp(clientIp);
        if (effortRepository.countByChallengeIdAndIpHashAndCreatedAtAfter(
                c.getId(), ipHash, LocalDateTime.now().minusHours(1)) >= MAX_PUBLIC_SUBMITS_PER_IP_PER_HOUR) {
            throw new IllegalStateException("rate_limit_exceeded");
        }

        SegmentMatchResult match = matchTrack(c, fileBytes);
        if (!match.isMatched()) {
            throw new IllegalArgumentException(match.getRejectionReason());
        }
        double maxKmh = type == ActivityType.RIDE ? MAX_RIDE_KMH : MAX_RUN_KMH;
        if (match.getAvgSpeedKmh() > maxKmh) {
            throw new IllegalArgumentException("implausible_speed");
        }

        SegmentEffort e = new SegmentEffort();
        e.setChallenge(c);
        e.setActivityType(type);
        e.setKind(EffortKind.PUBLIC);
        e.setCategory(EffortCategory.COMMUNITY);
        e.setDisplayName(displayName.trim());
        e.setElapsedSeconds(match.getElapsedSeconds());
        e.setAvgSpeedKmh(match.getAvgSpeedKmh());
        e.setAvgPaceSecondsPerKm(match.getAvgPaceSecondsPerKm());
        e.setTrackJson(serializeTrack(match.getCroppedTrack()));
        e.setSourceFormat("GPX");
        e.setStatus(EffortStatus.VALID);
        e.setEditToken(UUID.randomUUID().toString());
        e.setIpHash(ipHash);
        e.setCreatedAt(LocalDateTime.now());
        effortRepository.save(e);

        return buildResult(c, type, e);
    }

    @Transactional
    public Long addReferenceEffort(String slug, ActivityType type, String displayName,
                                   EffortCategory category, byte[] fileBytes, String originalFilename,
                                   Integer knownTimeSecondsOverride) {
        SegmentChallenge c = requireChallenge(slug);
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("display_name_required");
        }
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".gpx")) {
            throw new IllegalArgumentException("only_gpx_supported");
        }
        SegmentMatchResult match = matchTrack(c, fileBytes);
        if (!match.isMatched()) {
            throw new IllegalArgumentException(match.getRejectionReason());
        }

        SegmentEffort e = new SegmentEffort();
        e.setChallenge(c);
        e.setActivityType(type);
        e.setKind(EffortKind.REFERENCE);
        e.setCategory(category);
        e.setDisplayName(displayName.trim());
        e.setElapsedSeconds(knownTimeSecondsOverride != null ? knownTimeSecondsOverride : match.getElapsedSeconds());
        e.setAvgSpeedKmh(match.getAvgSpeedKmh());
        e.setAvgPaceSecondsPerKm(match.getAvgPaceSecondsPerKm());
        e.setTrackJson(serializeTrack(match.getCroppedTrack()));
        e.setSourceFormat("GPX");
        e.setStatus(EffortStatus.VALID);
        e.setCreatedAt(LocalDateTime.now());
        effortRepository.save(e);
        return e.getId();
    }

    @Transactional
    public void claimEffort(Long effortId, String editToken, Long userId) {
        SegmentEffort e = effortRepository.findById(effortId)
                .orElseThrow(() -> new IllegalArgumentException("effort_not_found"));
        if (e.getEditToken() == null || !e.getEditToken().equals(editToken)) {
            throw new IllegalArgumentException("invalid_edit_token");
        }
        e.setClaimedByUserId(userId);
        effortRepository.save(e);
    }

    @Transactional
    public void setEffortStatus(Long effortId, EffortStatus status) {
        SegmentEffort e = effortRepository.findById(effortId)
                .orElseThrow(() -> new IllegalArgumentException("effort_not_found"));
        e.setStatus(status);
        effortRepository.save(e);
    }

    // ---- internal helpers -------------------------------------------------

    private SegmentChallenge requireChallenge(String slug) {
        return challengeRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("challenge_not_found"));
    }

    private SegmentMatchResult matchTrack(SegmentChallenge c, byte[] fileBytes) {
        try {
            ParsedActivityData data = gpxParsingService.parse(fileBytes);
            return matchingService.match(data.latLngPoints, data.timeSeconds, data.elevations,
                    c.getStartLat(), c.getStartLng(), c.getEndLat(), c.getEndLng());
        } catch (Exception ex) {
            return SegmentMatchResult.rejected("unparseable_file");
        }
    }

    private SegmentEffortResultDto buildResult(SegmentChallenge c, ActivityType type, SegmentEffort saved) {
        List<SegmentEffort> efforts = effortRepository
                .findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                        c.getId(), type, EffortStatus.VALID);
        long total = efforts.size();
        int rank = 1;
        int slower = 0;
        for (SegmentEffort e : efforts) {
            if (e.getElapsedSeconds() < saved.getElapsedSeconds()) rank++;
            if (e.getElapsedSeconds() > saved.getElapsedSeconds()) slower++;
        }
        Integer leaderTime = efforts.isEmpty() ? null : efforts.get(0).getElapsedSeconds();
        int gap = leaderTime == null ? 0 : saved.getElapsedSeconds() - leaderTime;
        double percentileBeaten = total > 1 ? (slower * 100.0) / (total - 1) : 100.0;

        return new SegmentEffortResultDto(saved.getId(), saved.getEditToken(), rank, total,
                saved.getElapsedSeconds(), formatElapsed(saved.getElapsedSeconds()), gap,
                Math.round(percentileBeaten * 10) / 10.0, saved.getStatus().name());
    }

    private String serializeTrack(List<double[]> track) {
        try {
            return objectMapper.writeValueAsString(track);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String hashIp(String ip) {
        String raw = (ip == null ? "unknown" : ip) + "|heartbreak-salt";
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    // ---- pure leaderboard helpers (kept from Task 10) ---------------------

    public static List<SegmentLeaderboardEntryDto> buildLeaderboard(List<SegmentEffort> efforts) {
        List<SegmentEffort> sorted = new ArrayList<>(efforts);
        sorted.sort(Comparator.comparingInt(SegmentEffort::getElapsedSeconds));

        List<SegmentLeaderboardEntryDto> out = new ArrayList<>(sorted.size());
        Integer leaderTime = sorted.isEmpty() ? null : sorted.get(0).getElapsedSeconds();
        int rank = 0;
        for (SegmentEffort e : sorted) {
            rank++;
            int gap = leaderTime == null ? 0 : e.getElapsedSeconds() - leaderTime;
            out.add(new SegmentLeaderboardEntryDto(
                    e.getId(),
                    rank,
                    e.getDisplayName(),
                    e.getKind() != null ? e.getKind().name() : null,
                    e.getCategory() != null ? e.getCategory().name() : null,
                    e.getElapsedSeconds(),
                    formatElapsed(e.getElapsedSeconds()),
                    gap,
                    e.getAvgSpeedKmh(),
                    e.getAvgPaceSecondsPerKm(),
                    e.getKind() == EffortKind.REFERENCE
            ));
        }
        return out;
    }

    public static String formatElapsed(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return m + ":" + (s < 10 ? "0" + s : Integer.toString(s));
    }
}
