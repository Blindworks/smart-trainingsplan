package com.trainingsplan.repository;

import com.trainingsplan.entity.BodyMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BodyMetricRepository extends JpaRepository<BodyMetric, Long> {

    Optional<BodyMetric> findTopByUserIdAndMetricTypeOrderByRecordedAtDesc(Long userId, String metricType);

    List<BodyMetric> findByUserIdAndMetricTypeOrderByRecordedAtDesc(Long userId, String metricType);

    List<BodyMetric> findByUserIdOrderByRecordedAtDesc(Long userId);

    Optional<BodyMetric> findByUserIdAndMetricTypeAndSourceActivityId(Long userId, String metricType, Long sourceActivityId);

    @Modifying
    @Query("DELETE FROM BodyMetric b WHERE b.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT b.metricType FROM BodyMetric b WHERE b.user.id = :userId")
    List<String> findDistinctMetricTypesByUserId(@Param("userId") Long userId);
}
