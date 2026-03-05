package com.trainingsplan.repository;

import com.trainingsplan.entity.AiTrainingWorkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiTrainingWorkoutRepository extends JpaRepository<AiTrainingWorkout, Long> {

    List<AiTrainingWorkout> findByDayId(Long dayId);
}
