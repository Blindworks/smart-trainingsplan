package com.trainingsplan.repository;

import com.trainingsplan.entity.ActivityMetrics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityMetricsRepository extends JpaRepository<ActivityMetrics, Long> {

    Optional<ActivityMetrics> findByCompletedTrainingId(Long completedTrainingId);

    /** Returns the most recent activities with eligible decoupling data for the given user. */
    @Query("SELECT am FROM ActivityMetrics am " +
           "JOIN FETCH am.completedTraining ct " +
           "WHERE ct.user.id = :userId AND am.decouplingEligible = true " +
           "ORDER BY ct.trainingDate DESC, ct.id DESC")
    List<ActivityMetrics> findEligibleDecouplingByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT SUM(am.strain21) FROM ActivityMetrics am " +
           "JOIN am.completedTraining ct " +
           "WHERE ct.user.id = :userId AND ct.trainingDate = :date AND am.strain21 IS NOT NULL")
    Double sumStrain21ByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT SUM(am.trimp) FROM ActivityMetrics am " +
           "JOIN am.completedTraining ct " +
           "WHERE ct.user.id = :userId AND ct.trainingDate = :date AND am.trimp IS NOT NULL")
    Double sumTrimpByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
