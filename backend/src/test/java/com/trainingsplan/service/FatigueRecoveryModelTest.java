package com.trainingsplan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FatigueRecoveryModelTest {

    private FatigueRecoveryModel fatigueRecoveryModel;

    @BeforeEach
    void setUp() {
        fatigueRecoveryModel = new FatigueRecoveryModel();
    }

    @Test
    void applyDailyRecovery_subtractsFiveHundredthsAboveMinimum() {
        assertEquals(0.55, fatigueRecoveryModel.applyDailyRecovery(0.60), 1e-9);
    }

    @Test
    void applyDailyRecovery_capsAtMinimumWhenDroppingBelowFloor() {
        assertEquals(0.30, fatigueRecoveryModel.applyDailyRecovery(0.32), 1e-9);
    }

    @Test
    void applyDailyRecovery_keepsMinimumWhenAlreadyBelowFloor() {
        assertEquals(0.30, fatigueRecoveryModel.applyDailyRecovery(0.20), 1e-9);
    }
}
