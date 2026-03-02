package com.trainingsplan.repository;

import com.trainingsplan.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingRepository extends JpaRepository<Training, Long> {
    List<Training> findByTrainingPlan_Id(Long trainingPlanId);
}
