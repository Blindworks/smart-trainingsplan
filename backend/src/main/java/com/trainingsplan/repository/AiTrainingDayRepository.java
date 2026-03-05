package com.trainingsplan.repository;

import com.trainingsplan.entity.AiTrainingDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AiTrainingDayRepository extends JpaRepository<AiTrainingDay, Long> {

    List<AiTrainingDay> findByPlanIdOrderByDayIndexAsc(String planId);

    List<AiTrainingDay> findByPlan_User_IdAndDateBetween(Long userId, LocalDate from, LocalDate to);
}
