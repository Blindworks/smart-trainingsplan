package com.trainingsplan.dto;

import com.trainingsplan.entity.TrainingPlan;

public class TrainingPlanDto {

    private Long id;
    private String name;
    private String description;
    private boolean isTemplate;
    private Integer trainingCount;
    private Long competitionId;
    private String uploadDate;

    public TrainingPlanDto() {}

    public TrainingPlanDto(TrainingPlan plan) {
        this.id = plan.getId();
        this.name = plan.getName();
        this.description = plan.getDescription();
        this.isTemplate = plan.isTemplate();
        this.trainingCount = plan.getTrainingCount();
        this.competitionId = plan.getCompetition() != null ? plan.getCompetition().getId() : null;
        this.uploadDate = plan.getUploadDate() != null ? plan.getUploadDate().toString() : null;
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

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean isTemplate) {
        this.isTemplate = isTemplate;
    }

    public Integer getTrainingCount() {
        return trainingCount;
    }

    public void setTrainingCount(Integer trainingCount) {
        this.trainingCount = trainingCount;
    }

    public Long getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Long competitionId) {
        this.competitionId = competitionId;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }
}
