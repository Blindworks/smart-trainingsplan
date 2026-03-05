package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.Workout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pacr.training.simulation.dto.WorkoutImpactDTO;
import pacr.training.simulation.dto.WorkoutImpactDTO.InjuryRisk;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainingImpactEngineTest {

    private TrainingImpactEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TrainingImpactEngine(
                new TrainingLoadCalculator(),
                new FatigueModel(),
                new RecoveryModel(),
                new InjuryRiskModel()
        );
    }

    @Test
    void predictImpact_calculatesExpectedImpact() {
        Workout workout = new Workout(LocalDate.now(), "Threshold Z4", null, 20, null, null);
        AthleteState state = new AthleteState("Goal", 10, 0.7, 0.6, 1.2, 120, Collections.emptyList());

        WorkoutImpactDTO impact = engine.predictImpact(workout, state);

        assertEquals(80.0, impact.getPredictedTRIMP(), 1e-9);
        assertEquals(0.1, impact.getFatigueIncrease(), 1e-9);
        assertEquals(0.8, impact.getPredictedFatigue(), 1e-9);
        assertEquals(36, impact.getRecoveryHours());
        assertEquals(InjuryRisk.MEDIUM, impact.getInjuryRisk());
    }

    @Test
    void predictImpact_throwsWhenStateIsNull() {
        Workout workout = new Workout(LocalDate.now(), "Easy Z1 run", null, 30, null, null);

        assertThrows(IllegalArgumentException.class, () -> engine.predictImpact(workout, null));
    }
}
