package com.trainingsplan.repository;

import com.trainingsplan.entity.TrainingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {
    List<TrainingPlan> findByCompetitionId(Long competitionId);
    
    List<TrainingPlan> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT tp FROM TrainingPlan tp WHERE tp.competition.id = :competitionId")
    List<TrainingPlan> findAllByCompetitionId(Long competitionId);
}