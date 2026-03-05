package com.trainingsplan.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AITrainingPlanGenerateRequest(
        @NotNull(message = "userId is required")
        Long userId,
        @NotNull(message = "weekStartDate is required")
        LocalDate weekStartDate
) {}
