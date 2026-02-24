package com.trainingsplan.repository;

import com.trainingsplan.entity.BloodPressure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodPressureRepository extends JpaRepository<BloodPressure, Long> {

    List<BloodPressure> findByUserIdOrderByMeasuredAtDesc(Long userId);

    Optional<BloodPressure> findTopByUserIdOrderByMeasuredAtDesc(Long userId);

    Optional<BloodPressure> findByIdAndUserId(Long id, Long userId);
}
