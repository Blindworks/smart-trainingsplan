package com.trainingsplan.service;

import com.trainingsplan.dto.BuddyRunCreateRequest;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuddyRunService {

    private final BuddyRunRepository buddyRunRepository;
    private final BuddyRunParticipantRepository participantRepository;
    private final CommunityRouteRepository communityRouteRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    public BuddyRunService(BuddyRunRepository buddyRunRepository,
                           BuddyRunParticipantRepository participantRepository,
                           CommunityRouteRepository communityRouteRepository,
                           FriendshipRepository friendshipRepository,
                           NotificationService notificationService) {
        this.buddyRunRepository = buddyRunRepository;
        this.participantRepository = participantRepository;
        this.communityRouteRepository = communityRouteRepository;
        this.friendshipRepository = friendshipRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public BuddyRun createBuddyRun(User creator, BuddyRunCreateRequest req) {
        if (req.title == null || req.title.isBlank()) throw new IllegalArgumentException("title required");
        if (req.scheduledAt == null) throw new IllegalArgumentException("scheduledAt required");
        if (req.scheduledAt.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("scheduledAt must be in the future");
        if (req.meetingPointName == null || req.meetingPointName.isBlank()) throw new IllegalArgumentException("meetingPointName required");

        BuddyRun br = new BuddyRun();
        br.setCreator(creator);
        br.setTitle(req.title.trim());
        br.setDescription(req.description);
        br.setScheduledAt(req.scheduledAt);
        br.setMeetingPointName(req.meetingPointName.trim());
        br.setMeetingLatitude(req.meetingLatitude);
        br.setMeetingLongitude(req.meetingLongitude);
        if (req.communityRouteId != null) {
            br.setCommunityRoute(communityRouteRepository.findById(req.communityRouteId)
                    .orElseThrow(() -> new EntityNotFoundException("Route not found")));
        }
        br.setDistanceKm(req.distanceKm);
        br.setExpectedDurationMinutes(req.expectedDurationMinutes);
        br.setTargetPaceMinSecPerKm(req.targetPaceMinSecPerKm);
        br.setTargetPaceMaxSecPerKm(req.targetPaceMaxSecPerKm);
        br.setMaxParticipants(req.maxParticipants);
        if (req.visibility != null) {
            try {
                br.setVisibility(BuddyRun.Visibility.valueOf(req.visibility));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid visibility");
            }
        }

        br = buddyRunRepository.save(br);

        BuddyRunParticipant creatorPart = new BuddyRunParticipant();
        creatorPart.setBuddyRun(br);
        creatorPart.setUser(creator);
        creatorPart.setStatus(BuddyRunParticipant.Status.JOINED);
        creatorPart.setRole(BuddyRunParticipant.Role.CREATOR);
        creatorPart.setRespondedAt(LocalDateTime.now());
        participantRepository.save(creatorPart);
        br.getParticipants().add(creatorPart);
        return br;
    }

    @Transactional(readOnly = true)
    public BuddyRun getById(Long id) {
        return buddyRunRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BuddyRun not found"));
    }

    public boolean canUserSee(BuddyRun br, User user) {
        if (br.getCreator() != null && br.getCreator().getId().equals(user.getId())) return true;
        boolean isParticipant = br.getParticipants().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()));
        if (isParticipant) return true;
        switch (br.getVisibility()) {
            case PUBLIC_NEARBY: return true;
            case FRIENDS_ONLY: return friendshipRepository.areAcceptedFriends(br.getCreator().getId(), user.getId());
            case PRIVATE_INVITE: return false;
            default: return false;
        }
    }

    @Transactional
    public BuddyRunParticipant joinBuddyRun(Long buddyRunId, User user) {
        BuddyRun br = getById(buddyRunId);
        if (br.getStatus() != BuddyRun.Status.OPEN && br.getStatus() != BuddyRun.Status.CONFIRMED) {
            throw new IllegalStateException("Buddy run is not open");
        }
        if (!canUserSee(br, user)) throw new IllegalStateException("Not allowed to join this run");

        Optional<BuddyRunParticipant> existing = participantRepository.findByBuddyRunAndUser(br, user);
        BuddyRunParticipant p;
        if (existing.isPresent()) {
            p = existing.get();
            if (p.getStatus() == BuddyRunParticipant.Status.JOINED) return p;
            p.setStatus(BuddyRunParticipant.Status.JOINED);
            p.setRespondedAt(LocalDateTime.now());
        } else {
            checkCapacity(br);
            p = new BuddyRunParticipant();
            p.setBuddyRun(br);
            p.setUser(user);
            p.setStatus(BuddyRunParticipant.Status.JOINED);
            p.setRole(BuddyRunParticipant.Role.PARTICIPANT);
            p.setRespondedAt(LocalDateTime.now());
        }
        p = participantRepository.save(p);
        if (br.getCreator() != null && !br.getCreator().getId().equals(user.getId())) {
            notificationService.notify(br.getCreator(), UserNotification.Type.BUDDY_JOIN,
                    user.getUsername() + " ist deinem Buddy-Run beigetreten",
                    br.getTitle(), "/buddy/" + br.getId(), br.getId());
        }
        return p;
    }

    private void checkCapacity(BuddyRun br) {
        if (br.getMaxParticipants() == null) return;
        long joined = participantRepository.countByBuddyRunAndStatus(br, BuddyRunParticipant.Status.JOINED);
        if (joined >= br.getMaxParticipants()) {
            throw new IllegalStateException("Buddy run is full");
        }
    }

    @Transactional
    public BuddyRunParticipant inviteUser(Long buddyRunId, User creator, Long targetUserId, User targetUser) {
        BuddyRun br = getById(buddyRunId);
        if (!Objects.equals(br.getCreator().getId(), creator.getId())) {
            throw new IllegalStateException("Only creator can invite");
        }
        if (targetUser.getId().equals(creator.getId())) throw new IllegalArgumentException("Cannot invite self");

        Optional<BuddyRunParticipant> existing = participantRepository.findByBuddyRunAndUser(br, targetUser);
        if (existing.isPresent()
                && existing.get().getStatus() != BuddyRunParticipant.Status.DECLINED
                && existing.get().getStatus() != BuddyRunParticipant.Status.WITHDRAWN) {
            return existing.get();
        }

        BuddyRunParticipant p = existing.orElseGet(BuddyRunParticipant::new);
        p.setBuddyRun(br);
        p.setUser(targetUser);
        p.setStatus(BuddyRunParticipant.Status.INVITED);
        p.setRole(BuddyRunParticipant.Role.PARTICIPANT);
        p.setInvitedAt(LocalDateTime.now());
        p.setRespondedAt(null);
        p = participantRepository.save(p);

        notificationService.notify(targetUser, UserNotification.Type.BUDDY_INVITE,
                creator.getUsername() + " hat dich zu einem Buddy-Run eingeladen",
                br.getTitle(), "/buddy/" + br.getId(), br.getId());

        return p;
    }

    @Transactional
    public BuddyRunParticipant respondToInvite(Long buddyRunId, User user, boolean accept) {
        BuddyRun br = getById(buddyRunId);
        BuddyRunParticipant p = participantRepository.findByBuddyRunAndUser(br, user)
                .orElseThrow(() -> new EntityNotFoundException("No invitation for this user"));
        if (p.getStatus() != BuddyRunParticipant.Status.INVITED) {
            throw new IllegalStateException("Not in INVITED state");
        }
        if (accept) {
            checkCapacity(br);
            p.setStatus(BuddyRunParticipant.Status.JOINED);
        } else {
            p.setStatus(BuddyRunParticipant.Status.DECLINED);
        }
        p.setRespondedAt(LocalDateTime.now());
        p = participantRepository.save(p);

        if (br.getCreator() != null) {
            UserNotification.Type t = accept ? UserNotification.Type.BUDDY_JOIN : UserNotification.Type.BUDDY_WITHDRAW;
            String title = (accept ? "akzeptiert" : "abgelehnt");
            notificationService.notify(br.getCreator(), t,
                    user.getUsername() + " hat deine Einladung " + title,
                    br.getTitle(), "/buddy/" + br.getId(), br.getId());
        }
        return p;
    }

    @Transactional
    public void withdraw(Long buddyRunId, User user) {
        BuddyRun br = getById(buddyRunId);
        BuddyRunParticipant p = participantRepository.findByBuddyRunAndUser(br, user)
                .orElseThrow(() -> new EntityNotFoundException("Not participating"));
        if (p.getRole() == BuddyRunParticipant.Role.CREATOR) {
            throw new IllegalStateException("Creator cannot withdraw; cancel the run instead");
        }
        p.setStatus(BuddyRunParticipant.Status.WITHDRAWN);
        p.setRespondedAt(LocalDateTime.now());
        participantRepository.save(p);
        if (br.getCreator() != null) {
            notificationService.notify(br.getCreator(), UserNotification.Type.BUDDY_WITHDRAW,
                    user.getUsername() + " hat sich vom Buddy-Run abgemeldet",
                    br.getTitle(), "/buddy/" + br.getId(), br.getId());
        }
    }

    @Transactional
    public void cancelBuddyRun(Long buddyRunId, User actor) {
        BuddyRun br = getById(buddyRunId);
        if (!Objects.equals(br.getCreator().getId(), actor.getId())) {
            throw new IllegalStateException("Only creator can cancel");
        }
        br.setStatus(BuddyRun.Status.CANCELLED);
        buddyRunRepository.save(br);

        for (BuddyRunParticipant p : br.getParticipants()) {
            if (p.getUser() == null) continue;
            if (p.getUser().getId().equals(actor.getId())) continue;
            if (p.getStatus() == BuddyRunParticipant.Status.JOINED || p.getStatus() == BuddyRunParticipant.Status.INVITED) {
                notificationService.notify(p.getUser(), UserNotification.Type.BUDDY_CANCELLED,
                        "Buddy-Run wurde abgesagt",
                        br.getTitle(), "/buddy/" + br.getId(), br.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<BuddyRun> findOpenRunsForUser(User user, Double lat, Double lon, Double radiusKm) {
        List<BuddyRun> open = buddyRunRepository.findOpenUpcoming(LocalDateTime.now());
        Set<Long> friendIds = friendshipRepository.findAcceptedFriendships(user.getId()).stream()
                .map(f -> f.getRequester().getId().equals(user.getId()) ? f.getAddressee().getId() : f.getRequester().getId())
                .collect(Collectors.toSet());

        double radius = radiusKm != null ? radiusKm : 25.0;
        Double effLat = lat != null ? lat : user.getLatitude();
        Double effLon = lon != null ? lon : user.getLongitude();

        List<BuddyRun> result = new ArrayList<>();
        for (BuddyRun br : open) {
            if (br.getCreator() == null) continue;
            Long creatorId = br.getCreator().getId();
            boolean isCreator = creatorId.equals(user.getId());
            boolean alreadyParticipating = br.getParticipants().stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()));
            if (isCreator || alreadyParticipating) continue;

            switch (br.getVisibility()) {
                case PRIVATE_INVITE:
                    continue;
                case FRIENDS_ONLY:
                    if (!friendIds.contains(creatorId)) continue;
                    break;
                case PUBLIC_NEARBY:
                    if (effLat != null && effLon != null && br.getMeetingLatitude() != null && br.getMeetingLongitude() != null) {
                        double dist = haversineKm(effLat, effLon, br.getMeetingLatitude(), br.getMeetingLongitude());
                        if (dist > radius) continue;
                    }
                    break;
            }
            result.add(br);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<BuddyRun> findUpcomingForUser(User user) {
        return buddyRunRepository.findUpcomingForUser(user, LocalDateTime.now().minusHours(2));
    }

    @Transactional(readOnly = true)
    public List<BuddyRun> findCreatedByUser(User user) {
        return buddyRunRepository.findByCreatorOrderByScheduledAtDesc(user);
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
