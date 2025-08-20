package com.trainingsplan.repository;

import com.trainingsplan.entity.TrainingWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrainingWeekRepository extends JpaRepository<TrainingWeek, Long> {
    List<TrainingWeek> findByCompetitionId(Long competitionId);
    
    List<TrainingWeek> findByCompetitionIdOrderByWeekNumber(Long competitionId);
    
    @Query("SELECT tw FROM TrainingWeek tw WHERE tw.competition.id = :competitionId AND tw.startDate <= :date AND tw.endDate >= :date")
    TrainingWeek findByCompetitionIdAndDate(Long competitionId, LocalDate date);
    
    @Query("SELECT tw FROM TrainingWeek tw WHERE tw.competition.id = :competitionId AND tw.weekNumber = :weekNumber")
    TrainingWeek findByCompetitionIdAndWeekNumber(Long competitionId, Integer weekNumber);
    
    List<TrainingWeek> findByIsModifiedTrue();
}