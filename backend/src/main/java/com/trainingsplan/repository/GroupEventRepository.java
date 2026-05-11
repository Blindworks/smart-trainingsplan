package com.trainingsplan.repository;

import com.trainingsplan.entity.GroupEvent;
import com.trainingsplan.entity.GroupEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GroupEventRepository extends JpaRepository<GroupEvent, Long> {

    List<GroupEvent> findByTrainerIdOrderByEventDateDesc(Long trainerId);

    List<GroupEvent> findByStatusAndEventDateGreaterThanEqualOrderByEventDateAsc(GroupEventStatus status, LocalDate date);

    @Query("SELECT ge FROM GroupEvent ge WHERE ge.status = :status " +
           "AND ge.eventDate >= :date " +
           "AND ge.latitude BETWEEN :minLat AND :maxLat " +
           "AND ge.longitude BETWEEN :minLon AND :maxLon " +
           "ORDER BY ge.eventDate ASC")
    List<GroupEvent> findNearbyPublished(@Param("status") GroupEventStatus status,
                                        @Param("date") LocalDate date,
                                        @Param("minLat") double minLat,
                                        @Param("maxLat") double maxLat,
                                        @Param("minLon") double minLon,
                                        @Param("maxLon") double maxLon);

    // Recurring events: find all published recurring events whose series start is before range end
    @Query("SELECT ge FROM GroupEvent ge WHERE ge.status = :status AND ge.rrule IS NOT NULL " +
           "AND ge.eventDate <= :rangeEnd " +
           "AND (ge.recurrenceEndDate IS NULL OR ge.recurrenceEndDate >= :rangeStart)")
    List<GroupEvent> findPublishedRecurringInRange(@Param("status") GroupEventStatus status,
                                                   @Param("rangeStart") LocalDate rangeStart,
                                                   @Param("rangeEnd") LocalDate rangeEnd);

    // Nearby recurring events
    @Query("SELECT ge FROM GroupEvent ge WHERE ge.status = :status AND ge.rrule IS NOT NULL " +
           "AND ge.eventDate <= :rangeEnd " +
           "AND (ge.recurrenceEndDate IS NULL OR ge.recurrenceEndDate >= :rangeStart) " +
           "AND ge.latitude BETWEEN :minLat AND :maxLat " +
           "AND ge.longitude BETWEEN :minLon AND :maxLon")
    List<GroupEvent> findNearbyPublishedRecurringInRange(@Param("status") GroupEventStatus status,
                                                         @Param("rangeStart") LocalDate rangeStart,
                                                         @Param("rangeEnd") LocalDate rangeEnd,
                                                         @Param("minLat") double minLat,
                                                         @Param("maxLat") double maxLat,
                                                         @Param("minLon") double minLon,
                                                         @Param("maxLon") double maxLon);

    /**
     * Upcoming one-off events linked to a Run Club where the given user holds an ACTIVE
     * membership. Recurring events are returned with their series start date; expansion
     * happens in the service layer.
     */
    @Query("SELECT ge FROM GroupEvent ge WHERE ge.status = :status " +
           "AND ge.runClub IS NOT NULL " +
           "AND ge.eventDate >= :date " +
           "AND ge.runClub.id IN (SELECT m.club.id FROM RunClubMembership m " +
           "WHERE m.user.id = :userId AND m.status = com.trainingsplan.entity.RunClubMembershipStatus.ACTIVE) " +
           "ORDER BY ge.eventDate ASC")
    List<GroupEvent> findUpcomingForMember(@Param("status") GroupEventStatus status,
                                           @Param("date") LocalDate date,
                                           @Param("userId") Long userId);

    /**
     * Recurring events linked to a Run Club where the given user holds an ACTIVE
     * membership and whose series overlaps the given range.
     */
    @Query("SELECT ge FROM GroupEvent ge WHERE ge.status = :status AND ge.rrule IS NOT NULL " +
           "AND ge.runClub IS NOT NULL " +
           "AND ge.eventDate <= :rangeEnd " +
           "AND (ge.recurrenceEndDate IS NULL OR ge.recurrenceEndDate >= :rangeStart) " +
           "AND ge.runClub.id IN (SELECT m.club.id FROM RunClubMembership m " +
           "WHERE m.user.id = :userId AND m.status = com.trainingsplan.entity.RunClubMembershipStatus.ACTIVE)")
    List<GroupEvent> findRecurringForMemberInRange(@Param("status") GroupEventStatus status,
                                                   @Param("rangeStart") LocalDate rangeStart,
                                                   @Param("rangeEnd") LocalDate rangeEnd,
                                                   @Param("userId") Long userId);
}
