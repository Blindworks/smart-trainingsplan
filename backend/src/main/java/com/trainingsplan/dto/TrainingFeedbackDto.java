package com.trainingsplan.dto;

import jakarta.validation.constraints.NotNull;

public class TrainingFeedbackDto {
    @NotNull
    private Boolean isCompleted;
    
    private String completionStatus;

    public TrainingFeedbackDto() {}

    public TrainingFeedbackDto(Boolean isCompleted, String completionStatus) {
        this.isCompleted = isCompleted;
        this.completionStatus = completionStatus;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }
}