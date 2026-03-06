package com.trainingsplan.service;

import com.trainingsplan.entity.Training;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.repository.TrainingPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainingService {

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    public List<Training> findAll() {
        return trainingRepository.findAll();
    }

    public Training findById(Long id) {
        return trainingRepository.findById(id).orElse(null);
    }

    public List<Training> findByTrainingPlanId(Long planId) {
        return trainingRepository.findByTrainingPlan_Id(planId);
    }

    public Training save(Training training) {
        return trainingRepository.save(training);
    }

    @Transactional
    public Training update(Long id, Training incoming) {
        Training existing = trainingRepository.findById(id).orElseThrow();

        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setTrainingType(incoming.getTrainingType());
        existing.setIntensityLevel(incoming.getIntensityLevel());
        existing.setWeekNumber(incoming.getWeekNumber());
        existing.setDayOfWeek(incoming.getDayOfWeek());
        existing.setDurationMinutes(incoming.getDurationMinutes());
        existing.setWorkPace(incoming.getWorkPace());
        existing.setWorkTimeSeconds(incoming.getWorkTimeSeconds());
        existing.setWorkDistanceMeters(incoming.getWorkDistanceMeters());
        existing.setRecoveryPace(incoming.getRecoveryPace());
        existing.setRecoveryTimeSeconds(incoming.getRecoveryTimeSeconds());
        existing.setRecoveryDistanceMeters(incoming.getRecoveryDistanceMeters());

        return existing; // managed entity → Hibernate schreibt bei Transaktionsende automatisch
    }

    public void deleteById(Long id) {
        trainingRepository.deleteById(id);
    }
}
