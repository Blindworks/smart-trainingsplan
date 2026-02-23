package com.trainingsplan.repository;

import com.trainingsplan.entity.ActivityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityMetricsRepository extends JpaRepository<ActivityMetrics, Long> {

    Optional<ActivityMetrics> findByCompletedTrainingId(Long completedTrainingId);
}
