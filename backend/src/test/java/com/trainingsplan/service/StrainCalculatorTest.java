package com.trainingsplan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrainCalculatorTest {

    private StrainCalculator calculator;

    @BeforeEach
    void setUp() {
        // Instantiate directly — field initializer sets k=120.0 without Spring context
        calculator = new StrainCalculator();
    }

    // ─── rawLoad ──────────────────────────────────────────────────────────────

    @Test
    void rawLoad_60minZ2_equals120() {
        double result = calculator.rawLoad(0, 60, 0, 0, 0);
        assertEquals(120.0, result, 1e-9);
    }

    @Test
    void rawLoad_10minZ5_equals80() {
        double result = calculator.rawLoad(0, 0, 0, 0, 10);
        assertEquals(80.0, result, 1e-9); // 10 * weight(Z5=8)
    }

    @Test
    void rawLoad_allZeros_zero() {
        assertEquals(0.0, calculator.rawLoad(0, 0, 0, 0, 0), 1e-9);
    }

    @Test
    void rawLoad_mixedZones_correctWeightedSum() {
        // Z1=10*1 + Z2=20*2 + Z3=5*3 + Z4=3*5 + Z5=2*8 = 10+40+15+15+16 = 96
        double result = calculator.rawLoad(10, 20, 5, 3, 2);
        assertEquals(96.0, result, 1e-9);
    }

    // ─── strain21 ─────────────────────────────────────────────────────────────

    @Test
    void strain21_zeroLoad_zero() {
        assertEquals(0.0, calculator.strain21(0.0), 1e-9);
    }

    @Test
    void strain21_60minZ2_plausibleAndExact() {
        // rawLoad=120, k=120 → strain21 = 21*(1-exp(-1)) ≈ 13.27
        double rawLoad = calculator.rawLoad(0, 60, 0, 0, 0);
        double strain = calculator.strain21(rawLoad);
        assertEquals(21.0 * (1.0 - Math.exp(-1.0)), strain, 1e-9);
        assertTrue(strain > 10.0 && strain < 21.0, "60 min Z2 should give a mid-range strain");
    }

    @Test
    void strain21_10minZ5_highContribution() {
        // rawLoad=80, k=120 → strain21 = 21*(1-exp(-80/120)) ≈ 10.2
        double rawLoad = calculator.rawLoad(0, 0, 0, 0, 10);
        double strain = calculator.strain21(rawLoad);
        assertEquals(21.0 * (1.0 - Math.exp(-80.0 / 120.0)), strain, 1e-9);
        assertTrue(strain > 8.0, "10 min Z5 should produce significant strain");
    }

    @Test
    void strain21_neverExceeds21() {
        // Extreme load: 1000 min in Z5 — mathematically < 21, may equal 21.0 in floating point
        double rawLoad = calculator.rawLoad(0, 0, 0, 0, 1000);
        assertTrue(calculator.strain21(rawLoad) <= 21.0);
    }

    @Test
    void strain21_monotonelyIncreasing() {
        double s1 = calculator.strain21(calculator.rawLoad(0, 30, 0, 0, 0));
        double s2 = calculator.strain21(calculator.rawLoad(0, 60, 0, 0, 0));
        assertTrue(s2 > s1, "More load should produce more strain");
    }

    @Test
    void strain21_z5HigherThanZ1_sameMinutes() {
        // Z5 has higher weight, so same duration should yield higher strain
        double strainZ1 = calculator.strain21(calculator.rawLoad(60, 0, 0, 0, 0));
        double strainZ5 = calculator.strain21(calculator.rawLoad(0, 0, 0, 0, 60));
        assertTrue(strainZ5 > strainZ1, "Z5 should contribute more strain than Z1 for same minutes");
    }
}
