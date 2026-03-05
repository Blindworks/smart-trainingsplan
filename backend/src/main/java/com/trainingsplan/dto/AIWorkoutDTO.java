package com.trainingsplan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trainingsplan.entity.AiTrainingWorkout;
import com.trainingsplan.entity.AiWorkoutType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIWorkoutDTO {

    @NotNull(message = "Workout type is required")
    private AiWorkoutType type;

    @Size(max = 100, message = "Target zone must not exceed 100 characters")
    private String targetZone;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @JsonProperty("structure")
    private String structure;

    public AIWorkoutDTO() {}

    public AIWorkoutDTO(AiTrainingWorkout workout) {
        this.type = workout.getType();
        this.targetZone = workout.getTargetZone();
        this.durationMinutes = workout.getDurationMinutes();
        this.description = workout.getDescription();
        this.structure = workout.getStructureJson();
    }

    public AiWorkoutType getType() { return type; }
    public void setType(AiWorkoutType type) { this.type = type; }

    public String getTargetZone() { return targetZone; }
    public void setTargetZone(String targetZone) { this.targetZone = targetZone; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStructure() { return structure; }
    public void setStructure(String structure) { this.structure = structure; }
}
