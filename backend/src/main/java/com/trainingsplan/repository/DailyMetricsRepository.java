package com.trainingsplan.repository;

import com.trainingsplan.entity.DailyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMetricsRepository extends JpaRepository<DailyMetrics, Long> {

    Optional<DailyMetrics> findByUserIdAndDate(Long userId, LocalDate date);

    List<DailyMetrics> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
