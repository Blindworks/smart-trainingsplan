package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.AthleteStateDTO;
import com.trainingsplan.dto.Workout;
import org.springframework.stereotype.Service;
import pacr.training.simulation.dto.WeekSimulationResultDTO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class WeekSimulationService {

    private static final String DEFAULT_GOAL = "Improve aerobic running fitness";
    private static final int DEFAULT_WEEKS_TO_RACE = 8;

    private final WeekSimulationEngine weekSimulationEngine;
    private final WeekRiskAnalyzer weekRiskAnalyzer;
    private final AthleteStateService athleteStateService;

    public WeekSimulationService(
            WeekSimulationEngine weekSimulationEngine,
            WeekRiskAnalyzer weekRiskAnalyzer,
            AthleteStateService athleteStateService
    ) {
        this.weekSimulationEngine = weekSimulationEngine;
        this.weekRiskAnalyzer = weekRiskAnalyzer;
        this.athleteStateService = athleteStateService;
    }

    public WeekSimulationResultDTO simulateTrainingWeek(String userId, List<Workout> workouts) {
        return simulateTrainingWeek(parseNumericUserId(userId), workouts);
    }

    public WeekSimulationResultDTO simulateTrainingWeek(UUID userId, List<Workout> workouts) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        throw new IllegalArgumentException(
                "UUID user IDs are not supported by the current data model. Use numeric user id."
        );
    }

    public WeekSimulationResultDTO simulateTrainingWeek(Long userId, List<Workout> workouts) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        List<Workout> safeWorkouts = workouts == null ? List.of() : workouts;

        AthleteStateDTO athleteStateDTO = athleteStateService.getAthleteState(userId);
        AthleteState initialState = mapAthleteState(athleteStateDTO);

        WeekSimulationResultDTO simulation = weekSimulationEngine.simulateWeek(safeWorkouts, initialState);
        List<String> riskFlags = weekRiskAnalyzer.analyzeWeek(safeWorkouts, simulation);

        attachRiskFlags(simulation, riskFlags);
        return simulation;
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

    private void attachRiskFlags(WeekSimulationResultDTO simulation, List<String> additionalFlags) {
        List<String> baseFlags = simulation.getRiskFlags() != null ? simulation.getRiskFlags() : List.of();
        List<String> safeAdditionalFlags = additionalFlags != null ? additionalFlags : List.of();

        LinkedHashSet<String> merged = new LinkedHashSet<>(baseFlags);
        merged.addAll(safeAdditionalFlags);
        simulation.setRiskFlags(new ArrayList<>(merged));
    }

    private AthleteState mapAthleteState(AthleteStateDTO dto) {
        double fitness = 0.0;
        if (dto.getTrimpMetrics() != null) {
            fitness = clamp(dto.getTrimpMetrics().getRolling28DayAverage(), 0.0, 100.0);
        }

        return new AthleteState(
                DEFAULT_GOAL,
                DEFAULT_WEEKS_TO_RACE,
                dto.getFatigueScore(),
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
