package com.trainingsplan.service;

import com.trainingsplan.entity.Training;
import com.trainingsplan.entity.TrainingWeek;
import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.repository.TrainingWeekRepository;
import com.trainingsplan.repository.TrainingPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class TrainingService {

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private TrainingWeekRepository trainingWeekRepository;

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    public List<Training> findAll() {
        return trainingRepository.findAll();
    }

    public Training findById(Long id) {
        return trainingRepository.findById(id).orElse(null);
    }

    public List<Training> findByTrainingWeekId(Long weekId) {
        return trainingRepository.findByTrainingWeekId(weekId);
    }

    public List<Training> findByDate(LocalDate date) {
        return trainingRepository.findByTrainingDate(date);
    }

    public List<Training> findByCompetitionIdAndDate(Long competitionId, LocalDate date) {
        return trainingRepository.findByCompetitionIdAndDate(competitionId, date);
    }

    public Training save(Training training) {
        return trainingRepository.save(training);
    }

    public void deleteById(Long id) {
        trainingRepository.deleteById(id);
    }

    public Training updateTrainingFeedback(Long trainingId, Boolean isCompleted, String completionStatus) {
        Training training = findById(trainingId);
        if (training != null) {
            training.setIsCompleted(isCompleted);
            training.setCompletionStatus(completionStatus);
            
            if (!isCompleted) {
                adjustWeeklyTraining(training);
            }
            
            return save(training);
        }
        return null;
    }

    public List<Training> generateMixedTraining(Long competitionId, List<Long> planIds, LocalDate date) {
        List<Training> mixedTrainings = new ArrayList<>();
        Random random = new Random();

        for (Long planId : planIds) {
            TrainingPlan plan = trainingPlanRepository.findById(planId).orElse(null);
            if (plan != null) {
                List<Training> planTrainings = plan.getTrainings().stream()
                    .filter(t -> t.getTrainingDate().equals(date))
                    .toList();

                if (!planTrainings.isEmpty()) {
                    Training selectedTraining = planTrainings.get(random.nextInt(planTrainings.size()));
                    
                    Training mixedTraining = new Training();
                    mixedTraining.setName("Mixed: " + selectedTraining.getName());
                    mixedTraining.setTrainingDescription(selectedTraining.getTrainingDescription());
                    mixedTraining.setTrainingDate(date);
                    mixedTraining.setTrainingType(selectedTraining.getTrainingType());
                    mixedTraining.setIntensityLevel(selectedTraining.getIntensityLevel());
                    mixedTraining.setStartTime(selectedTraining.getStartTime());
                    mixedTraining.setDurationMinutes(selectedTraining.getDurationMinutes());
                    
                    mixedTrainings.add(mixedTraining);
                }
            }
        }

        return mixedTrainings;
    }

    private void adjustWeeklyTraining(Training missedTraining) {
        TrainingWeek week = missedTraining.getTrainingWeek();
        if (week != null) {
            week.setIsModified(true);
            trainingWeekRepository.save(week);
            
            List<Training> weekTrainings = findByTrainingWeekId(week.getId());
            for (Training training : weekTrainings) {
                if (!training.getId().equals(missedTraining.getId()) && !training.getIsCompleted()) {
                    if ("high".equals(missedTraining.getIntensityLevel())) {
                        training.setIntensityLevel("medium");
                    } else if ("medium".equals(missedTraining.getIntensityLevel())) {
                        training.setIntensityLevel("low");
                    }
                    save(training);
                }
            }
        }
    }
}