package com.trainingsplan.service;

import com.trainingsplan.service.hrzone.HeartRateZoneConfig;
import com.trainingsplan.service.hrzone.ZoneTimeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZoneTimeCalculatorTest {

    private ZoneTimeCalculator calculator;
    /** hrMax = 200 → Z2 = 120–140 bpm, Z3 = 140–160 bpm */
    private HeartRateZoneConfig config200;

    @BeforeEach
    void setUp() {
        calculator = new ZoneTimeCalculator();
        config200  = HeartRateZoneConfig.fromHrMax(200);
    }

    // ─── Basic zone assignment ────────────────────────────────────────────────

    @Test
    void constantHrZ2_regularSampling_allTimeInZ2() {
        // 60 samples, 1 second apart → 59 s of Δt, all in Z2 (130 bpm = 65% of 200)
        List<Integer> time = range(0, 60);
        List<Integer> hr   = repeat(130, 60);

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertFalse(result.isUnknown());
        assertEquals(59.0 / 60.0, result.getZ2Min(), 1e-9);
        assertEquals(0, result.getZ1Min(), 1e-9);
        assertEquals(0, result.getZ3Min(), 1e-9);
        assertEquals(0, result.getZ4Min(), 1e-9);
        assertEquals(0, result.getZ5Min(), 1e-9);
        assertEquals(1.0, result.getHrDataCoverage(), 1e-9);
    }

    @Test
    void constantHrZ3_regularSampling_allTimeInZ3() {
        // 150 bpm = 75% of 200 → Z3
        List<Integer> time = range(0, 121); // 120 s total
        List<Integer> hr   = repeat(150, 121);

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertFalse(result.isUnknown());
        assertEquals(2.0, result.getZ3Min(), 1e-9); // 120 s / 60
        assertEquals(0, result.getZ2Min(), 1e-9);
        assertEquals(1.0, result.getHrDataCoverage(), 1e-9);
    }

    // ─── Irregular sampling ───────────────────────────────────────────────────

    @Test
    void irregularSampling_weightedByDeltaT() {
        // 300 s in Z2, 60 s in Z4 (with irregular gaps)
        // time: 0, 300, 360  →  Δt[0]=300 (Z2), Δt[1]=60 (Z4)
        List<Integer> time = Arrays.asList(0, 300, 360);
        List<Integer> hr   = Arrays.asList(130, 170, 130); // last sample has no Δt
        // hrMax=200: 130 bpm=65%→Z2, 170 bpm=85%→Z4

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertFalse(result.isUnknown());
        assertEquals(300.0 / 60.0, result.getZ2Min(), 1e-9); // 5.0 min
        assertEquals(60.0  / 60.0, result.getZ4Min(), 1e-9); // 1.0 min
        assertEquals(1.0, result.getHrDataCoverage(), 1e-9);
    }

    // ─── Zone boundaries ─────────────────────────────────────────────────────

    @Test
    void hrExactlyAtZoneBoundary_lowerIsIncluded() {
        // 120 bpm = exactly 60% of 200 → lower bound of Z2 (inclusive)
        List<Integer> time = Arrays.asList(0, 60);
        List<Integer> hr   = Arrays.asList(120, 120);

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertEquals(1.0, result.getZ2Min(), 1e-9);
        assertEquals(0,   result.getZ1Min(), 1e-9);
    }

    @Test
    void hrAtHrMax_clampsToZ5() {
        // 200 bpm = 100% of hrMax → clamped, should be in Z5
        List<Integer> time = Arrays.asList(0, 60);
        List<Integer> hr   = Arrays.asList(200, 200);

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertEquals(1.0, result.getZ5Min(), 1e-9);
    }

    @Test
    void hrAboveHrMax_clampedToHrMax_countedInZ5() {
        // 220 bpm > hrMax=200 → clamped to 200, still Z5
        List<Integer> time = Arrays.asList(0, 120);
        List<Integer> hr   = Arrays.asList(220, 220);

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertEquals(2.0, result.getZ5Min(), 1e-9);
    }

    @Test
    void hrBelowZ1Boundary_notCountedInAnyZone_butCoverageStillValid() {
        // 90 bpm = 45% → below Z1 (50%), valid HR but no zone
        // Followed by 130 bpm in Z2
        List<Integer> time = Arrays.asList(0, 60, 120);
        List<Integer> hr   = Arrays.asList(90, 130, 130);

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertEquals(0, result.getZ1Min(), 1e-9);
        assertEquals(60.0 / 60.0, result.getZ2Min(), 1e-9);
        // Both intervals had valid HR → coverage = 1.0
        assertEquals(1.0, result.getHrDataCoverage(), 1e-9);
    }

    // ─── Missing / invalid HR values ─────────────────────────────────────────

    @Test
    void nullHrValues_intervalsSkipped_coverageReduced() {
        // 3 intervals of 60 s each; middle HR is null
        List<Integer> time = Arrays.asList(0, 60, 120, 180);
        List<Integer> hr   = Arrays.asList(130, null, 130, 130);
        // Δt[0]=60 valid Z2, Δt[1]=60 null → skip, Δt[2]=60 valid Z2
        // validDuration=120, total=180 → coverage=2/3

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertFalse(result.isUnknown());
        assertEquals(120.0 / 60.0, result.getZ2Min(), 1e-9);
        assertEquals(120.0 / 180.0, result.getHrDataCoverage(), 1e-6);
    }

    @Test
    void zeroHrValues_treatedAsMissing() {
        List<Integer> time = Arrays.asList(0, 60, 120);
        List<Integer> hr   = Arrays.asList(0, 130, 130);
        // Δt[0]=60 hr=0 → skip; Δt[1]=60 hr=130 → Z2
        // validDuration=60, total=120 → coverage=0.5

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertEquals(60.0 / 60.0, result.getZ2Min(), 1e-9);
        assertEquals(0.5, result.getHrDataCoverage(), 1e-9);
    }

    @Test
    void allHrMissing_coverageZero_allZonesZero() {
        List<Integer> time = Arrays.asList(0, 60, 120, 180);
        List<Integer> hr   = Arrays.asList(null, null, null, null);

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertFalse(result.isUnknown()); // streams are present
        assertEquals(0, result.totalZoneMin(), 1e-9);
        assertEquals(0, result.getHrDataCoverage(), 1e-9);
    }

    // ─── Missing stream (unknown result) ─────────────────────────────────────

    @Test
    void nullTimeStream_returnsUnknown() {
        ZoneTimeResult result = calculator.calculate(null, repeat(130, 5), config200);
        assertTrue(result.isUnknown());
    }

    @Test
    void nullHrStream_returnsUnknown() {
        ZoneTimeResult result = calculator.calculate(range(0, 5), null, config200);
        assertTrue(result.isUnknown());
    }

    @Test
    void singleSample_returnsUnknown() {
        ZoneTimeResult result = calculator.calculate(
                Collections.singletonList(0),
                Collections.singletonList(130),
                config200);
        assertTrue(result.isUnknown());
    }

    @Test
    void streamSizeMismatch_returnsUnknown() {
        ZoneTimeResult result = calculator.calculate(
                Arrays.asList(0, 10, 20),
                Arrays.asList(130, 130),
                config200);
        assertTrue(result.isUnknown());
    }

    @Test
    void emptyStreams_returnsUnknown() {
        ZoneTimeResult result = calculator.calculate(
                Collections.emptyList(),
                Collections.emptyList(),
                config200);
        assertTrue(result.isUnknown());
    }

    // ─── Coverage edge cases ──────────────────────────────────────────────────

    @Test
    void fullCoverage_exactlyOne() {
        List<Integer> time = range(0, 61); // 60 s total
        List<Integer> hr   = repeat(150, 61); // all valid

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertEquals(1.0, result.getHrDataCoverage(), 1e-9);
    }

    @Test
    void hrDataCoverage_neverExceedsOne_evenIfInterpolationGlitch() {
        // If sum of valid Δt somehow exceeds total duration due to floating point,
        // coverage is clamped to 1.0
        List<Integer> time = Arrays.asList(0, 60);
        List<Integer> hr   = Arrays.asList(150, 150);

        ZoneTimeResult result = calculator.calculate(time, hr, config200);

        assertTrue(result.getHrDataCoverage() <= 1.0);
    }

    // ─── HeartRateZoneConfig unit tests ──────────────────────────────────────

    @Test
    void zoneConfig_invalidHrMax_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> HeartRateZoneConfig.fromHrMax(0));
        assertThrows(IllegalArgumentException.class, () -> HeartRateZoneConfig.fromHrMax(-1));
    }

    @Test
    void zoneConfig_hrBelowZ1_returnsNull() {
        assertNull(config200.zoneFor(99));  // 49.5% → below Z1
    }

    @Test
    void zoneConfig_hrZero_returnsNull() {
        assertNull(config200.zoneFor(0));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Returns [start, start+1, ..., start+count-1]. */
    private static List<Integer> range(int start, int count) {
        List<Integer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(start + i);
        return list;
    }

    private static List<Integer> repeat(Integer value, int count) {
        List<Integer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(value);
        return list;
    }
}
