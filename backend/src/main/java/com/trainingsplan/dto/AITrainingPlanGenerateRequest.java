package com.trainingsplan.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AITrainingPlanGenerateRequest(
        @NotBlank(message = "userId is required")
        String userId,
        @NotNull(message = "weekStart is required")
        @JsonAlias("weekStartDate")
        LocalDate weekStart
) {}
