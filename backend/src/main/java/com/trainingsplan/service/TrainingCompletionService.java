package com.trainingsplan.service;

import com.trainingsplan.dto.DailyTrainingCompletionDto;
import com.trainingsplan.entity.Training;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingCompletionService {
    
    @Autowired
    private TrainingRepository trainingRepository;
    
    @Autowired
    private CompletedTrainingRepository completedTrainingRepository;
    
    public DailyTrainingCompletionDto getDailyTrainingCompletion(LocalDate date) {
        List<Training> plannedTrainings = trainingRepository.findByTrainingDate(date);
        List<CompletedTraining> completedTrainings = completedTrainingRepository.findByTrainingDateOrderByUploadDateDesc(date);
        
        List<String> plannedTrainingNames = plannedTrainings.stream()
                .map(Training::getName)
                .collect(Collectors.toList());
        
        List<String> completedTrainingSports = completedTrainings.stream()
                .map(ct -> ct.getSport() != null ? ct.getSport() : "Unknown")
                .collect(Collectors.toList());
        
        return new DailyTrainingCompletionDto(
                date,
                plannedTrainings.size(),
                completedTrainings.size(),
                plannedTrainingNames,
                completedTrainingSports
        );
    }
    
    public List<DailyTrainingCompletionDto> getWeeklyTrainingCompletion(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(this::getDailyTrainingCompletion)
                .collect(Collectors.toList());
    }
    
    public DailyTrainingCompletionDto getTodayTrainingCompletion() {
        return getDailyTrainingCompletion(LocalDate.now());
    }
}