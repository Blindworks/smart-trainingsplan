package com.trainingsplan.dto;

import com.trainingsplan.entity.TrainingPlan;

public class TrainingPlanDto {

    private Long id;
    private String name;
    private String description;
    private Integer trainingCount;
    private String uploadDate;
    private String targetTime;
    private String prerequisites;

    public TrainingPlanDto() {}

    public TrainingPlanDto(TrainingPlan plan) {
        this.id = plan.getId();
        this.name = plan.getName();
        this.description = plan.getDescription();
        this.trainingCount = plan.getTrainingCount();
        this.uploadDate = plan.getUploadDate() != null ? plan.getUploadDate().toString() : null;
        this.targetTime = plan.getTargetTime();
        this.prerequisites = plan.getPrerequisites();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTrainingCount() {
        return trainingCount;
    }

    public void setTrainingCount(Integer trainingCount) {
        this.trainingCount = trainingCount;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(String targetTime) {
        this.targetTime = targetTime;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }
}
