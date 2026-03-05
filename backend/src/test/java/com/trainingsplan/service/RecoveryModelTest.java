package com.trainingsplan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecoveryModelTest {

    private RecoveryModel recoveryModel;

    @BeforeEach
    void setUp() {
        recoveryModel = new RecoveryModel();
    }

    @Test
    void calculateRecoveryHours_returns12WhenTrimpBelow40() {
        assertEquals(12, recoveryModel.calculateRecoveryHours(39.9));
    }

    @Test
    void calculateRecoveryHours_returns24WhenTrimpIs40ToBelow80() {
        assertEquals(24, recoveryModel.calculateRecoveryHours(40.0));
        assertEquals(24, recoveryModel.calculateRecoveryHours(79.9));
    }

    @Test
    void calculateRecoveryHours_returns36WhenTrimpIs80ToBelow120() {
        assertEquals(36, recoveryModel.calculateRecoveryHours(80.0));
        assertEquals(36, recoveryModel.calculateRecoveryHours(119.9));
    }

    @Test
    void calculateRecoveryHours_returns48WhenTrimpIs120OrMore() {
        assertEquals(48, recoveryModel.calculateRecoveryHours(120.0));
        assertEquals(48, recoveryModel.calculateRecoveryHours(200.0));
    }
}
