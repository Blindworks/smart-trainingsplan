package com.trainingsplan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FatigueModelTest {

    private FatigueModel fatigueModel;

    @BeforeEach
    void setUp() {
        fatigueModel = new FatigueModel();
    }

    @Test
    void predictFatigueIncrease_usesTrimpOver800() {
        assertEquals(0.1, fatigueModel.predictFatigueIncrease(80.0), 1e-9);
    }

    @Test
    void predictNextFatigue_addsIncreaseWhenWithinBounds() {
        assertEquals(0.5, fatigueModel.predictNextFatigue(0.4, 0.1), 1e-9);
    }

    @Test
    void predictNextFatigue_capsAtUpperBound() {
        assertEquals(1.0, fatigueModel.predictNextFatigue(0.95, 0.1), 1e-9);
    }

    @Test
    void predictNextFatigue_capsAtLowerBound() {
        assertEquals(0.0, fatigueModel.predictNextFatigue(0.05, -0.1), 1e-9);
    }
}