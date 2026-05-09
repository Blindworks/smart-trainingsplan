package com.trainingsplan.service.runclub;

import com.trainingsplan.dto.runclub.RunClubStatsDto;
import com.trainingsplan.entity.RunClub;
import com.trainingsplan.entity.RunClubMemberRole;
import com.trainingsplan.entity.RunClubMembership;
import com.trainingsplan.entity.RunClubMembershipStatus;
import com.trainingsplan.repository.GroupEventRepository;
import com.trainingsplan.repository.RunClubMembershipRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RunClubStatsService {

    private static final Logger log = LoggerFactory.getLogger(RunClubStatsService.class);

    @PersistenceContext
    private EntityManager em;

    private final RunClubMembershipRepository membershipRepository;
    private final RunClubService runClubService;
    private final GroupEventRepository groupEventRepository;

    public RunClubStatsService(RunClubMembershipRepository membershipRepository,
                               RunClubService runClubService,
                               GroupEventRepository groupEventRepository) {
        this.membershipRepository = membershipRepository;
        this.runClubService = runClubService;
        this.groupEventRepository = groupEventRepository;
    }

    public RunClubStatsDto computeStats(Long clubId) {
        RunClub club = runClubService.requireClub(clubId);

        int memberCount = (int) membershipRepository.countByClubAndStatus(
                club, RunClubMembershipStatus.ACTIVE);

        List<Long> activeMemberIds = membershipRepository
                .findByClubIdAndStatus(clubId, RunClubMembershipStatus.ACTIVE)
                .stream()
                .map(m -> m.getUser().getId())
                .toList();

        if (activeMemberIds.isEmpty()) {
            return new RunClubStatsDto(memberCount, 0, 0, 0, 0, 0);
        }

        // Lifetime km across all activities of active members
        Double lifetimeKm = querySumKm(activeMemberIds, null, null);
        Double last30dKm = querySumKm(activeMemberIds, LocalDate.now().minusDays(30), LocalDate.now());
        Long totalActivities = queryActivityCount(activeMemberIds);

        // Event stats: sum km of GroupEvents linked to this club that are in the past
        double totalEventKm = computeEventKm(clubId, activeMemberIds);
        int totalEventCount = (int) groupEventRepository.findAll().stream()
                .filter(e -> e.getRunClub() != null && e.getRunClub().getId().equals(clubId)
                        && !e.getEventDate().isAfter(LocalDate.now()))
                .count();

        return new RunClubStatsDto(
                memberCount,
                lifetimeKm != null ? lifetimeKm : 0.0,
                last30dKm != null ? last30dKm : 0.0,
                totalActivities != null ? totalActivities.intValue() : 0,
                totalEventKm,
                totalEventCount
        );
    }

    private Double querySumKm(List<Long> userIds, LocalDate from, LocalDate to) {
        if (userIds.isEmpty()) return 0.0;
        try {
            String jpql;
            if (from != null && to != null) {
                jpql = "SELECT COALESCE(SUM(ct.distanceKm), 0) FROM CompletedTraining ct " +
                       "WHERE ct.user.id IN :userIds " +
                       "AND ct.trainingDate >= :from AND ct.trainingDate <= :to " +
                       "AND ct.distanceKm IS NOT NULL";
                return (Double) em.createQuery(jpql)
                        .setParameter("userIds", userIds)
                        .setParameter("from", from)
                        .setParameter("to", to)
                        .getSingleResult();
            } else {
                jpql = "SELECT COALESCE(SUM(ct.distanceKm), 0) FROM CompletedTraining ct " +
                       "WHERE ct.user.id IN :userIds AND ct.distanceKm IS NOT NULL";
                return (Double) em.createQuery(jpql)
                        .setParameter("userIds", userIds)
                        .getSingleResult();
            }
        } catch (Exception e) {
            log.warn("Error computing club km stats: {}", e.getMessage());
            return 0.0;
        }
    }

    private Long queryActivityCount(List<Long> userIds) {
        if (userIds.isEmpty()) return 0L;
        try {
            return (Long) em.createQuery(
                    "SELECT COUNT(ct) FROM CompletedTraining ct WHERE ct.user.id IN :userIds")
                    .setParameter("userIds", userIds)
                    .getSingleResult();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Sums km from members' activities on event days for this club's GroupEvents.
     * This is an approximation: any activity by a member on an event day counts.
     */
    private double computeEventKm(Long clubId, List<Long> activeMemberIds) {
        if (activeMemberIds.isEmpty()) return 0.0;
        try {
            // Get all event dates for this club's past GroupEvents
            List<LocalDate> eventDates = groupEventRepository.findAll().stream()
                    .filter(e -> e.getRunClub() != null && e.getRunClub().getId().equals(clubId)
                            && !e.getEventDate().isAfter(LocalDate.now()))
                    .map(e -> e.getEventDate())
                    .distinct()
                    .toList();

            if (eventDates.isEmpty()) return 0.0;

            Double result = (Double) em.createQuery(
                    "SELECT COALESCE(SUM(ct.distanceKm), 0) FROM CompletedTraining ct " +
                    "WHERE ct.user.id IN :userIds " +
                    "AND ct.trainingDate IN :eventDates " +
                    "AND ct.distanceKm IS NOT NULL")
                    .setParameter("userIds", activeMemberIds)
                    .setParameter("eventDates", eventDates)
                    .getSingleResult();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            log.warn("Error computing event km stats: {}", e.getMessage());
            return 0.0;
        }
    }
}
