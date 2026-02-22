package com.trainingsplan.repository;

import com.trainingsplan.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrainingRepository extends JpaRepository<Training, Long> {
    List<Training> findByTrainingWeekId(Long trainingWeekId);

    List<Training> findByTrainingPlan_Id(Long trainingPlanId);

    List<Training> findByTrainingDate(LocalDate date);

    List<Training> findByTrainingDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Training t WHERE t.trainingWeek.competition.id = :competitionId")
    List<Training> findByCompetitionId(@Param("competitionId") Long competitionId);

    @Query("SELECT t FROM Training t WHERE t.trainingWeek.competition.id = :competitionId AND t.trainingDate = :date")
    List<Training> findByCompetitionIdAndDate(@Param("competitionId") Long competitionId, @Param("date") LocalDate date);

    @Query("SELECT t FROM Training t WHERE t.trainingPlan.id = :planId AND t.trainingWeek IS NULL")
    List<Training> findByTrainingPlanIdAndTrainingWeekIsNull(@Param("planId") Long planId);
}