package com.trainingsplan.service;

import com.trainingsplan.service.trimp.TRIMPQuality;
import com.trainingsplan.service.trimp.TRIMPResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TRIMPCalculatorTest {

    private TRIMPCalculator calculator;

    /** hrRest=60, hrMax=200 → hrRange=140 */
    private static final int HR_REST = 60;
    private static final int HR_MAX  = 200;
    private static final double K    = TRIMPCalculator.K_MALE;

    @BeforeEach
    void setUp() {
        calculator = new TRIMPCalculator();
    }

    // ─── Basic formula correctness ────────────────────────────────────────────

    @Test
    void singleInterval_correctTRIMP() {
        // 60 s at HR=130: ΔHR=(130-60)/140=0.5, dt=1 min
        // TRIMP = 1 * 0.5 * exp(1.92 * 0.5) = 0.5 * exp(0.96)
        List<Integer> time = Arrays.asList(0, 60);
        List<Integer> hr   = Arrays.asList(130, 130);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        double deltaHR = (130 - HR_REST) / (double)(HR_MAX - HR_REST);
        double expected = 1.0 * deltaHR * Math.exp(K * deltaHR);
        assertEquals(expected, result.trimp(), 1e-9);
    }

    @Test
    void hrAtRest_contributesTRIMPOfZero() {
        // HR == hrRest → ΔHR = 0 → TRIMP contribution = 0
        List<Integer> time = Arrays.asList(0, 600);
        List<Integer> hr   = Arrays.asList(HR_REST, HR_REST);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertEquals(0.0, result.trimp(), 1e-9);
    }

    @Test
    void hrBelowRest_clampedToZero_noNegativeContribution() {
        // HR below hrRest → clamped ΔHR=0 → TRIMP=0
        List<Integer> time = Arrays.asList(0, 120);
        List<Integer> hr   = Arrays.asList(50, 50); // 50 < hrRest=60

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertEquals(0.0, result.trimp(), 1e-9);
    }

    @Test
    void hrAtMax_clampedToOne_correctTRIMP() {
        // HR = hrMax → ΔHR clamped to 1.0
        // TRIMP = dt_min * 1.0 * exp(k * 1.0)
        List<Integer> time = Arrays.asList(0, 60);
        List<Integer> hr   = Arrays.asList(HR_MAX, HR_MAX);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        double expected = 1.0 * 1.0 * Math.exp(K * 1.0);
        assertEquals(expected, result.trimp(), 1e-9);
    }

    @Test
    void hrAboveMax_clampedToOne_sameAsTRIMPatMax() {
        // HR > hrMax → ΔHR clamped to 1.0
        List<Integer> time = Arrays.asList(0, 60);
        List<Integer> hr   = Arrays.asList(220, 220);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        double expected = 1.0 * 1.0 * Math.exp(K * 1.0);
        assertEquals(expected, result.trimp(), 1e-9);
    }

    // ─── Irregular sampling ───────────────────────────────────────────────────

    @Test
    void irregularSampling_weightedByDeltaT() {
        // Two intervals: 300 s at HR=130, then 60 s at HR=170
        List<Integer> time = Arrays.asList(0, 300, 360);
        List<Integer> hr   = Arrays.asList(130, 170, 130); // last sample contributes no time

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);

        double dhr130 = (130 - HR_REST) / (double)(HR_MAX - HR_REST);
        double dhr170 = (170 - HR_REST) / (double)(HR_MAX - HR_REST);
        double expected = (300.0 / 60) * dhr130 * Math.exp(K * dhr130)
                        + (60.0  / 60) * dhr170 * Math.exp(K * dhr170);
        assertEquals(expected, result.trimp(), 1e-9);
    }

    @Test
    void multipleIntervals_sumIsAdditive() {
        // 4 x 60 s at same HR → 4× single-interval TRIMP
        List<Integer> time = range(0, 5, 60); // 0,60,120,180,240
        List<Integer> hr   = repeat(150, 5);

        TRIMPResult single = calculator.calculate(Arrays.asList(0, 60), Arrays.asList(150, 150), HR_REST, HR_MAX, K);
        TRIMPResult multi  = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(single);
        assertNotNull(multi);
        assertEquals(4 * single.trimp(), multi.trimp(), 1e-9);
    }

    // ─── Gender coefficient ───────────────────────────────────────────────────

    @Test
    void kForGender_female_returns1_67() {
        assertEquals(TRIMPCalculator.K_FEMALE, TRIMPCalculator.kForGender("FEMALE"), 1e-9);
        assertEquals(TRIMPCalculator.K_FEMALE, TRIMPCalculator.kForGender("female"), 1e-9);
    }

    @Test
    void kForGender_maleAndNull_returns1_92() {
        assertEquals(TRIMPCalculator.K_MALE, TRIMPCalculator.kForGender("MALE"), 1e-9);
        assertEquals(TRIMPCalculator.K_MALE, TRIMPCalculator.kForGender(null),   1e-9);
        assertEquals(TRIMPCalculator.K_MALE, TRIMPCalculator.kForGender(""),     1e-9);
    }

    @Test
    void femaleK_producesLowerTrimpThanMale_forSameStream() {
        List<Integer> time = Arrays.asList(0, 3600);
        List<Integer> hr   = Arrays.asList(150, 150);

        TRIMPResult male   = calculator.calculate(time, hr, HR_REST, HR_MAX, K_MALE);
        TRIMPResult female = calculator.calculate(time, hr, HR_REST, HR_MAX, K_FEMALE);

        assertNotNull(male);
        assertNotNull(female);
        assertTrue(female.trimp() < male.trimp(),
                "Female k=1.67 should produce lower TRIMP than male k=1.92 for HR > hrRest");
    }

    // ─── Quality flag ─────────────────────────────────────────────────────────

    @Test
    void fullHrCoverage_qualityOK() {
        List<Integer> time = range(0, 61, 1);  // 60 samples, 1 s apart
        List<Integer> hr   = repeat(150, 61);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertEquals(TRIMPQuality.OK, result.quality());
        assertEquals(1.0, result.hrCoverage(), 1e-9);
    }

    @Test
    void hrCoverageBelow60Percent_qualityLow() {
        // 3 intervals: first valid (60 s), next two invalid (120 s) → coverage = 60/180 ≈ 0.33
        List<Integer> time = Arrays.asList(0, 60, 120, 180);
        List<Integer> hr   = Arrays.asList(150, null, null, 150);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertEquals(TRIMPQuality.LOW, result.quality());
        assertTrue(result.hrCoverage() < 0.6);
    }

    @Test
    void hrCoverageExactly60Percent_qualityOK() {
        // 3 intervals of 60 s each; first and third valid (120 s), middle invalid (60 s)
        // coverage = 120/180 ≈ 0.667 → OK
        List<Integer> time = Arrays.asList(0, 60, 120, 180);
        List<Integer> hr   = Arrays.asList(150, null, 150, 150);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertEquals(TRIMPQuality.OK, result.quality());
    }

    @Test
    void hrCoverageJustBelow60_qualityLow() {
        // 5 intervals of 60 s; 2 valid, 3 invalid → coverage = 120/300 = 0.4 → LOW
        List<Integer> time = Arrays.asList(0, 60, 120, 180, 240, 300);
        List<Integer> hr   = Arrays.asList(150, 150, null, null, null, 150);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertEquals(TRIMPQuality.LOW, result.quality());
    }

    // ─── Missing / invalid HR values ─────────────────────────────────────────

    @Test
    void nullHrValues_skipped_trimpStillCorrect() {
        // Only Δt[0]=60 is valid; Δt[1]=60 is skipped (null HR)
        List<Integer> time = Arrays.asList(0, 60, 120);
        List<Integer> hr   = Arrays.asList(150, null, 150);

        TRIMPResult result  = calculator.calculate(time, hr, HR_REST, HR_MAX, K);
        TRIMPResult singleInterval = calculator.calculate(Arrays.asList(0, 60), Arrays.asList(150, 150), HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertNotNull(singleInterval);
        assertEquals(singleInterval.trimp(), result.trimp(), 1e-9);
    }

    @Test
    void zeroHrValues_treatedAsMissing() {
        List<Integer> time = Arrays.asList(0, 60, 120);
        List<Integer> hr   = Arrays.asList(0, 150, 150);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);
        TRIMPResult singleInterval = calculator.calculate(Arrays.asList(0, 60), Arrays.asList(150, 150), HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertNotNull(singleInterval);
        assertEquals(singleInterval.trimp(), result.trimp(), 1e-9);
    }

    @Test
    void allHrMissing_trimpZero_qualityLow() {
        List<Integer> time = Arrays.asList(0, 60, 120, 180);
        List<Integer> hr   = Arrays.asList(null, null, null, null);

        TRIMPResult result = calculator.calculate(time, hr, HR_REST, HR_MAX, K);

        assertNotNull(result);
        assertEquals(0.0, result.trimp(), 1e-9);
        assertEquals(TRIMPQuality.LOW, result.quality());
        assertEquals(0.0, result.hrCoverage(), 1e-9);
    }

    // ─── Invalid input → null ─────────────────────────────────────────────────

    @Test
    void nullTimeStream_returnsNull() {
        assertNull(calculator.calculate(null, repeat(130, 5), HR_REST, HR_MAX, K));
    }

    @Test
    void nullHrStream_returnsNull() {
        assertNull(calculator.calculate(range(0, 5, 1), null, HR_REST, HR_MAX, K));
    }

    @Test
    void singleSample_returnsNull() {
        assertNull(calculator.calculate(
                Collections.singletonList(0),
                Collections.singletonList(130),
                HR_REST, HR_MAX, K));
    }

    @Test
    void streamSizeMismatch_returnsNull() {
        assertNull(calculator.calculate(
                Arrays.asList(0, 60, 120),
                Arrays.asList(130, 130),
                HR_REST, HR_MAX, K));
    }

    @Test
    void emptyStreams_returnsNull() {
        assertNull(calculator.calculate(
                Collections.emptyList(),
                Collections.emptyList(),
                HR_REST, HR_MAX, K));
    }

    @Test
    void hrRestEqualToHrMax_returnsNull() {
        // hrRange = 0 → invalid config
        assertNull(calculator.calculate(
                Arrays.asList(0, 60),
                Arrays.asList(130, 130),
                HR_MAX, HR_MAX, K));
    }

    @Test
    void hrRestGreaterThanHrMax_returnsNull() {
        assertNull(calculator.calculate(
                Arrays.asList(0, 60),
                Arrays.asList(130, 130),
                250, HR_MAX, K));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static final double K_MALE   = TRIMPCalculator.K_MALE;
    private static final double K_FEMALE = TRIMPCalculator.K_FEMALE;

    /** Returns [start, start+step, start+2*step, ...] with count elements. */
    private static List<Integer> range(int start, int count, int step) {
        List<Integer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(start + i * step);
        return list;
    }

    private static List<Integer> repeat(Integer value, int count) {
        List<Integer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(value);
        return list;
    }
}
