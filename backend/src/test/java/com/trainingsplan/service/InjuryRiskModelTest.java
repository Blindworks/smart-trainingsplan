package com.trainingsplan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pacr.training.simulation.dto.WorkoutImpactDTO.InjuryRisk;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InjuryRiskModelTest {

    private InjuryRiskModel injuryRiskModel;

    @BeforeEach
    void setUp() {
        injuryRiskModel = new InjuryRiskModel();
    }

    @Test
    void predictRisk_highWhenAbove085() {
        assertEquals(InjuryRisk.HIGH, injuryRiskModel.predictRisk(0.851));
    }

    @Test
    void predictRisk_mediumWhenAbove075AndNotAbove085() {
        assertEquals(InjuryRisk.MEDIUM, injuryRiskModel.predictRisk(0.80));
    }

    @Test
    void predictRisk_mediumAtBoundary085() {
        assertEquals(InjuryRisk.MEDIUM, injuryRiskModel.predictRisk(0.85));
    }

    @Test
    void predictRisk_lowAtBoundary075() {
        assertEquals(InjuryRisk.LOW, injuryRiskModel.predictRisk(0.75));
    }

    @Test
    void predictRisk_lowWhenBelowOrEqual075() {
        assertEquals(InjuryRisk.LOW, injuryRiskModel.predictRisk(0.40));
    }
}
