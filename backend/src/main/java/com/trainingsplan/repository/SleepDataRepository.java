package com.trainingsplan.repository;

import com.trainingsplan.entity.SleepData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SleepDataRepository extends JpaRepository<SleepData, Long> {

    List<SleepData> findByUserIdOrderByRecordedAtDesc(Long userId);

    Optional<SleepData> findTopByUserIdOrderByRecordedAtDesc(Long userId);

    Optional<SleepData> findByIdAndUserId(Long id, Long userId);

    Optional<SleepData> findByUserIdAndRecordedAt(Long userId, java.time.LocalDate recordedAt);
}
