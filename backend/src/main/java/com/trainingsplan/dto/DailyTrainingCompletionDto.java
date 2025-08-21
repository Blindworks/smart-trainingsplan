package com.trainingsplan.dto;

import java.time.LocalDate;
import java.util.List;

public class DailyTrainingCompletionDto {
    private LocalDate date;
    private int plannedTrainingsCount;
    private int completedTrainingsCount;
    private double completionPercentage;
    private List<String> plannedTrainingNames;
    private List<String> completedTrainingSports;
    
    public DailyTrainingCompletionDto() {}
    
    public DailyTrainingCompletionDto(LocalDate date, int plannedTrainingsCount, int completedTrainingsCount, 
                                    List<String> plannedTrainingNames, List<String> completedTrainingSports) {
        this.date = date;
        this.plannedTrainingsCount = plannedTrainingsCount;
        this.completedTrainingsCount = completedTrainingsCount;
        this.plannedTrainingNames = plannedTrainingNames;
        this.completedTrainingSports = completedTrainingSports;
        this.completionPercentage = calculateCompletionPercentage();
    }
    
    private double calculateCompletionPercentage() {
        if (plannedTrainingsCount == 0) {
            return completedTrainingsCount > 0 ? 100.0 : 0.0;
        }
        return Math.min(100.0, (double) completedTrainingsCount / plannedTrainingsCount * 100.0);
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public int getPlannedTrainingsCount() {
        return plannedTrainingsCount;
    }
    
    public void setPlannedTrainingsCount(int plannedTrainingsCount) {
        this.plannedTrainingsCount = plannedTrainingsCount;
        this.completionPercentage = calculateCompletionPercentage();
    }
    
    public int getCompletedTrainingsCount() {
        return completedTrainingsCount;
    }
    
    public void setCompletedTrainingsCount(int completedTrainingsCount) {
        this.completedTrainingsCount = completedTrainingsCount;
        this.completionPercentage = calculateCompletionPercentage();
    }
    
    public double getCompletionPercentage() {
        return Math.round(completionPercentage * 100.0) / 100.0;
    }
    
    public List<String> getPlannedTrainingNames() {
        return plannedTrainingNames;
    }
    
    public void setPlannedTrainingNames(List<String> plannedTrainingNames) {
        this.plannedTrainingNames = plannedTrainingNames;
    }
    
    public List<String> getCompletedTrainingSports() {
        return completedTrainingSports;
    }
    
    public void setCompletedTrainingSports(List<String> completedTrainingSports) {
        this.completedTrainingSports = completedTrainingSports;
    }
}