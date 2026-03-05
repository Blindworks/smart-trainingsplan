package com.trainingsplan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trainingsplan.entity.AiTrainingDay;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AITrainingDayDTO {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Valid
    private List<AIWorkoutDTO> workouts;

    public AITrainingDayDTO() {}

    public AITrainingDayDTO(AiTrainingDay day) {
        this.date = day.getDate();
        this.workouts = day.getWorkouts().stream()
                .map(AIWorkoutDTO::new)
                .collect(Collectors.toList());
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<AIWorkoutDTO> getWorkouts() { return workouts; }
    public void setWorkouts(List<AIWorkoutDTO> workouts) { this.workouts = workouts; }
}
