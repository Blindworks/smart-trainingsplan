package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.AthleteStateDTO;
import com.trainingsplan.dto.Workout;
import org.springframework.stereotype.Service;
import pacr.training.simulation.dto.WorkoutImpactDTO;

@Service
public class TrainingImpactService {

    private static final String DEFAULT_GOAL = "Improve aerobic running fitness";
    private static final int DEFAULT_WEEKS_TO_RACE = 8;
    private static final double FATIGUE_SCORE_SCALE = 100.0;

    private final AthleteStateService athleteStateService;
    private final TrainingImpactEngine trainingImpactEngine;

    public TrainingImpactService(AthleteStateService athleteStateService, TrainingImpactEngine trainingImpactEngine) {
        this.athleteStateService = athleteStateService;
        this.trainingImpactEngine = trainingImpactEngine;
    }

    public WorkoutImpactDTO predictImpact(String userId, Workout workout) {
        if (workout == null) {
            throw new IllegalArgumentException("workout is required");
        }

        Long numericUserId = parseNumericUserId(userId);
        AthleteStateDTO athleteStateDto = athleteStateService.getAthleteState(numericUserId);
        AthleteState athleteState = mapAthleteState(athleteStateDto);

        return trainingImpactEngine.predictImpact(workout, athleteState);
    }

    private Long parseNumericUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("userId must be a numeric id", ex);
        }
    }

    private AthleteState mapAthleteState(AthleteStateDTO dto) {
        double fitness = 0.0;
        if (dto.getTrimpMetrics() != null) {
            fitness = clamp(dto.getTrimpMetrics().getRolling28DayAverage(), 0.0, 100.0);
        }
        double fatigue = clamp(dto.getFatigueScore() / FATIGUE_SCORE_SCALE, 0.0, 1.0);

        return new AthleteState(
                DEFAULT_GOAL,
                DEFAULT_WEEKS_TO_RACE,
                fatigue,
                fitness,
                dto.getEfficiencyScore(),
                dto.getLongRunCapacityMinutes(),
                dto.getRunningZones()
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
