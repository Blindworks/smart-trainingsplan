package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.Workout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pacr.training.simulation.dto.WeekSimulationResultDTO;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeekSimulationEngineTest {

    private WeekSimulationEngine weekSimulationEngine;

    @BeforeEach
    void setUp() {
        TrainingImpactEngine trainingImpactEngine = new TrainingImpactEngine(
                new TrainingLoadCalculator(),
                new FatigueModel(),
                new RecoveryModel(),
                new InjuryRiskModel()
        );
        weekSimulationEngine = new WeekSimulationEngine(trainingImpactEngine, new WeekRiskAnalyzer());
    }

    @Test
    void simulateWeek_appliesWorkoutThenRestRecovery() {
        LocalDate startDate = LocalDate.of(2026, 3, 2);
        Workout workout = new Workout(startDate, "Threshold Z4", null, 20, null, null);
        AthleteState initialState = new AthleteState("Goal", 8, 0.6, 0.5, 1.1, 120, Collections.emptyList());

        WeekSimulationResultDTO result = weekSimulationEngine.simulateWeek(List.of(workout), initialState);

        assertEquals(7, result.getFatigueTimeline().size());
        assertEquals(startDate, result.getFatigueTimeline().get(0).getDate());
        assertEquals(0.7, result.getFatigueTimeline().get(0).getFatigue(), 1e-9);
        assertEquals(0.4, result.getFatigueTimeline().get(6).getFatigue(), 1e-9);
        assertEquals(0.7, result.getPeakFatigue(), 1e-9);
        assertEquals(0, result.getRiskFlags().size());
    }

    @Test
    void simulateWeek_clampsFatigueToZero() {
        AthleteState initialState = new AthleteState("Goal", 8, 0.02, 0.5, 1.1, 120, Collections.emptyList());

        WeekSimulationResultDTO result = weekSimulationEngine.simulateWeek(Collections.emptyList(), initialState);

        assertEquals(0.0, result.getFatigueTimeline().get(0).getFatigue(), 1e-9);
        assertEquals(0.0, result.getFatigueTimeline().get(6).getFatigue(), 1e-9);
        assertEquals(0.02, result.getPeakFatigue(), 1e-9);
    }

    @Test
    void simulateWeek_clampsFatigueToOneAndCreatesRiskFlag() {
        LocalDate startDate = LocalDate.of(2026, 3, 2);
        Workout workout = new Workout(startDate, "Intervals Z5", null, 300, null, null);
        AthleteState initialState = new AthleteState("Goal", 8, 0.9, 0.5, 1.1, 120, Collections.emptyList());

        WeekSimulationResultDTO result = weekSimulationEngine.simulateWeek(List.of(workout), initialState);

        assertEquals(1.0, result.getFatigueTimeline().get(0).getFatigue(), 1e-9);
        assertEquals(1.0, result.getPeakFatigue(), 1e-9);
        assertEquals(2, result.getRiskFlags().size());
        assertTrue(result.getRiskFlags().contains(WeekRiskAnalyzer.OVERTRAINING_RISK));
    }

    @Test
    void simulateWeek_throwsWhenInitialStateIsNull() {
        assertThrows(IllegalArgumentException.class, () -> weekSimulationEngine.simulateWeek(Collections.emptyList(), null));
    }
}
