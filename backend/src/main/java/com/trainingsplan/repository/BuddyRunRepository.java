package com.trainingsplan.repository;

import com.trainingsplan.entity.BuddyRun;
import com.trainingsplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BuddyRunRepository extends JpaRepository<BuddyRun, Long> {

    List<BuddyRun> findByCreatorOrderByScheduledAtDesc(User creator);

    @Query("SELECT br FROM BuddyRun br WHERE br.status = com.trainingsplan.entity.BuddyRun$Status.OPEN " +
            "AND br.scheduledAt >= :now ORDER BY br.scheduledAt ASC")
    List<BuddyRun> findOpenUpcoming(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT br FROM BuddyRun br JOIN br.participants p " +
            "WHERE p.user = :user AND p.status IN (com.trainingsplan.entity.BuddyRunParticipant$Status.JOINED, " +
            "                                       com.trainingsplan.entity.BuddyRunParticipant$Status.INVITED) " +
            "AND br.scheduledAt >= :from ORDER BY br.scheduledAt ASC")
    List<BuddyRun> findUpcomingForUser(@Param("user") User user, @Param("from") LocalDateTime from);

    @Query("SELECT br FROM BuddyRun br WHERE br.status = com.trainingsplan.entity.BuddyRun$Status.OPEN " +
            "AND br.scheduledAt BETWEEN :from AND :to")
    List<BuddyRun> findOpenBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT br FROM BuddyRun br WHERE br.status IN (com.trainingsplan.entity.BuddyRun$Status.OPEN, " +
            "                                              com.trainingsplan.entity.BuddyRun$Status.CONFIRMED) " +
            "AND br.scheduledAt BETWEEN :from AND :to")
    List<BuddyRun> findActiveBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
