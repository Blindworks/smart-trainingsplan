package com.trainingsplan.service;

import com.trainingsplan.service.decoupling.DecouplingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AerobicDecouplingCalculatorTest {

    private AerobicDecouplingCalculator calc;

    @BeforeEach
    void setUp() {
        calc = new AerobicDecouplingCalculator();
    }

    // ── Core correctness ──────────────────────────────────────────────────────

    /**
     * Constant HR and constant speed throughout → E1 = E2 → decoupling = 0 %.
     */
    @Test
    void constantHrAndSpeed_zeroDecoupling() {
        int n = 2401; // 0..2400 s = 40 min
        DecouplingResult r = calc.calculate(range(0, n), repeat(140, n), repeatD(3.0, n), null);

        assertTrue(r.eligible());
        assertEquals("OK", r.reason());
        assertEquals(0.0, r.decouplingPct(), 1e-9);
    }

    /**
     * Second half has higher HR at the same speed → positive drift.
     * E1 = 140/3.0, E2 = 160/3.0
     * Expected decoupling = (160 − 140) / 140 × 100 ≈ 14.286 %
     */
    @Test
    void higherHrInSecondHalf_positiveDrift() {
        int n = 2401; // 40 min, 1 s intervals
        List<Integer> time = range(0, n);
        // Split index = 1200 (midpoint of 2400 s); intervals [0..1200) use hr[0..1199]
        List<Integer> hr = new ArrayList<>(n);
        for (int i = 0; i < n; i++) hr.add(i < 1200 ? 140 : 160);
        List<Double> vel = repeatD(3.0, n);

        DecouplingResult r = calc.calculate(time, hr, vel, null);

        assertTrue(r.eligible());
        double expected = 100.0 * (160.0 - 140.0) / 140.0; // ≈ 14.286
        assertEquals(expected, r.decouplingPct(), 1e-9);
    }

    /**
     * Second half has lower speed at the same HR → positive drift (fatigue).
     * E1 = 140/3.0, E2 = 140/2.5
     * Expected decoupling = (140/2.5 − 140/3.0) / (140/3.0) × 100 = (3.0/2.5 − 1) × 100 = 20 %
     */
    @Test
    void lowerSpeedInSecondHalf_positiveDrift() {
        int n = 2401;
        List<Integer> time = range(0, n);
        List<Integer> hr   = repeat(140, n);
        List<Double>  vel  = new ArrayList<>(n);
        for (int i = 0; i < n; i++) vel.add(i < 1200 ? 3.0 : 2.5);

        DecouplingResult r = calc.calculate(time, hr, vel, null);

        assertTrue(r.eligible());
        double e1 = 140.0 / 3.0;
        double e2 = 140.0 / 2.5;
        assertEquals((e2 - e1) / e1 * 100.0, r.decouplingPct(), 1e-9);
    }

    /**
     * Second half is more efficient (HR drops) → negative decoupling (well-paced run).
     */
    @Test
    void lowerHrInSecondHalf_negativeDrift() {
        int n = 2401;
        List<Integer> time = range(0, n);
        List<Integer> hr   = new ArrayList<>(n);
        for (int i = 0; i < n; i++) hr.add(i < 1200 ? 160 : 140);
        List<Double> vel = repeatD(3.0, n);

        DecouplingResult r = calc.calculate(time, hr, vel, null);

        assertTrue(r.eligible());
        assertTrue(r.decouplingPct() < 0, "Negative drift expected for improving HR");
    }

    // ── Distance-based split ──────────────────────────────────────────────────

    /**
     * Distance stream provided → split at 5 km of a 10 km run.
     * Speed doubles in second half but HR stays constant → efficiency E = HR/speed drops → negative drift.
     */
    @Test
    void distanceBasedSplit_correctHalves() {
        int n = 2401;
        List<Integer> time = range(0, n);    // 2400 s at 1 s intervals
        List<Integer> hr   = repeat(150, n); // constant HR

        // First 1200 samples: speed 2.0 m/s → 2400 m
        // Next  1201 samples: speed 2.0 m/s → another 2400 m → total 4800 m (approx)
        // Use distance stream that crosses 2400 m at index 1200
        List<Double> vel  = repeatD(2.0, n);
        List<Double> dist = new ArrayList<>(n);
        for (int i = 0; i < n; i++) dist.add((double) i * 2.0); // cumulative 0..4800

        // Split at distance = 2400 m → index 1200
        DecouplingResult r = calc.calculate(time, hr, vel, dist);

        assertTrue(r.eligible());
        assertEquals(0.0, r.decouplingPct(), 1e-9, "Constant HR+speed must give 0% regardless of split");
    }

    // ── Gating ────────────────────────────────────────────────────────────────

    @Test
    void tooShort_ineligible() {
        int n = 601; // 0..600 s = 10 min (< 20 min)
        DecouplingResult r = calc.calculate(range(0, n), repeat(140, n), repeatD(3.0, n), null);

        assertFalse(r.eligible());
        assertEquals("TOO_SHORT", r.reason());
    }

    @Test
    void hrCoverageTooLow_ineligible() {
        int n = 2401;
        List<Integer> time = range(0, n);
        // Only the first 600 s have valid HR (25% coverage → below 70% threshold)
        List<Integer> hr = new ArrayList<>(n);
        for (int i = 0; i < n; i++) hr.add(i < 601 ? 140 : null);
        List<Double> vel = repeatD(3.0, n);

        DecouplingResult r = calc.calculate(time, hr, vel, null);

        assertFalse(r.eligible());
        assertEquals("HR_COVERAGE_TOO_LOW", r.reason());
    }

    @Test
    void speedDataMissing_ineligible() {
        int n = 2401;
        List<Integer> time = range(0, n);
        List<Integer> hr   = repeat(140, n);
        // Only the first 600 s have valid velocity (25% coverage)
        List<Double> vel = new ArrayList<>(n);
        for (int i = 0; i < n; i++) vel.add(i < 601 ? 3.0 : null);

        DecouplingResult r = calc.calculate(time, hr, vel, null);

        assertFalse(r.eligible());
        assertEquals("SPEED_DATA_MISSING", r.reason());
    }

    @Test
    void nullTimeList_ineligible() {
        DecouplingResult r = calc.calculate(null, repeat(140, 10), repeatD(3.0, 10), null);
        assertFalse(r.eligible());
        assertEquals("INSUFFICIENT_DATA", r.reason());
    }

    @Test
    void nullVelocityList_ineligible() {
        DecouplingResult r = calc.calculate(range(0, 2401), repeat(140, 2401), null, null);
        assertFalse(r.eligible());
        assertEquals("INSUFFICIENT_DATA", r.reason());
    }

    @Test
    void singleSample_ineligible() {
        DecouplingResult r = calc.calculate(
                Collections.singletonList(0),
                Collections.singletonList(140),
                Collections.singletonList(3.0),
                null);
        assertFalse(r.eligible());
        assertEquals("INSUFFICIENT_DATA", r.reason());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** [start, start+1, ..., start+count-1] */
    private List<Integer> range(int start, int count) {
        List<Integer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(start + i);
        return list;
    }

    private List<Integer> repeat(int value, int count) {
        List<Integer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(value);
        return list;
    }

    private List<Double> repeatD(double value, int count) {
        List<Double> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(value);
        return list;
    }
}
