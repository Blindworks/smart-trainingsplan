package com.trainingsplan.repository;

import com.trainingsplan.entity.AiTrainingPlan;
import com.trainingsplan.entity.AiTrainingPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiTrainingPlanRepository extends JpaRepository<AiTrainingPlan, String> {

    List<AiTrainingPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AiTrainingPlan> findByUserIdAndStatus(Long userId, AiTrainingPlanStatus status);
}
