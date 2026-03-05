package com.trainingsplan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.trainingsplan.entity.AiTrainingPlan;
import com.trainingsplan.entity.AiTrainingPlanStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AITrainingPlanDTO {

    private String id;

    @NotNull(message = "Week start date is required")
    private LocalDate weekStartDate;

    private LocalDateTime createdAt;

    private String modelName;

    private String modelVersion;

    private AiTrainingPlanStatus status;

    private JsonNode weekSummary;

    @Valid
    @NotEmpty(message = "A plan must contain at least one day")
    private List<AITrainingDayDTO> days;

    public AITrainingPlanDTO() {}

    public AITrainingPlanDTO(AiTrainingPlan plan) {
        this.id = plan.getId();
        this.weekStartDate = plan.getWeekStartDate();
        this.createdAt = plan.getCreatedAt();
        this.modelName = plan.getModelName();
        this.modelVersion = plan.getModelVersion();
        this.status = plan.getStatus();
        this.days = plan.getDays().stream()
                .map(AITrainingDayDTO::new)
                .collect(Collectors.toList());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public AiTrainingPlanStatus getStatus() { return status; }
    public void setStatus(AiTrainingPlanStatus status) { this.status = status; }

    public JsonNode getWeekSummary() { return weekSummary; }
    public void setWeekSummary(JsonNode weekSummary) { this.weekSummary = weekSummary; }

    public List<AITrainingDayDTO> getDays() { return days; }
    public void setDays(List<AITrainingDayDTO> days) { this.days = days; }
}
