package com.trainingsplan.service;

import com.trainingsplan.service.decoupling.DecouplingResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calculates aerobic decoupling (HR drift) for a running activity.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Gate: reject if duration &lt; 20 min, HR coverage &lt; 70 %, or speed coverage &lt; 70 %.</li>
 *   <li>Split into two equal halves by cumulative distance (preferred) or elapsed time.</li>
 *   <li>For each half compute time-weighted avgHR and avgSpeed over intervals where
 *       both HR and speed are valid (non-null, &gt; 0).</li>
 *   <li>Efficiency E = avgHR / avgSpeed (higher = less efficient).</li>
 *   <li>Decoupling% = (E₂ − E₁) / E₁ × 100 — positive → worse second half.</li>
 * </ol>
 */
@Service
public class AerobicDecouplingCalculator {

    private static final int    MIN_DURATION_SECONDS = 20 * 60;
    private static final double MIN_HR_COVERAGE      = 0.70;
    private static final double MIN_SPEED_COVERAGE   = 0.70;

    /**
     * @param timeSeconds  elapsed seconds from activity start; monotonically non-decreasing
     * @param heartRates   HR in bpm, parallel to timeSeconds; null entries = missing sensor data
     * @param velocities   speed in m/s, parallel to timeSeconds; null entries = missing sensor data
     * @param distances    cumulative metres from activity start, parallel to timeSeconds;
     *                     pass {@code null} to fall back to time-based half-split
     * @return {@link DecouplingResult}; never null
     */
    public DecouplingResult calculate(List<Integer> timeSeconds,
                                      List<Integer> heartRates,
                                      List<Double>  velocities,
                                      List<Double>  distances) {
        if (timeSeconds == null || timeSeconds.size() < 2) {
            return DecouplingResult.ineligible("INSUFFICIENT_DATA");
        }
        if (heartRates == null || velocities == null
                || heartRates.size() != timeSeconds.size()
                || velocities.size() != timeSeconds.size()) {
            return DecouplingResult.ineligible("INSUFFICIENT_DATA");
        }

        int n             = timeSeconds.size();
        int totalDuration = timeSeconds.get(n - 1) - timeSeconds.get(0);

        if (totalDuration < MIN_DURATION_SECONDS) {
            return DecouplingResult.ineligible("TOO_SHORT");
        }

        double hrCoverage = computeHrCoverage(timeSeconds, heartRates, totalDuration);
        if (hrCoverage < MIN_HR_COVERAGE) {
            return DecouplingResult.ineligible("HR_COVERAGE_TOO_LOW");
        }

        double speedCoverage = computeSpeedCoverage(timeSeconds, velocities, totalDuration);
        if (speedCoverage < MIN_SPEED_COVERAGE) {
            return DecouplingResult.ineligible("SPEED_DATA_MISSING");
        }

        int splitIdx = findSplitIndex(timeSeconds, distances);

        double e1 = computeEfficiency(timeSeconds, heartRates, velocities, 0,        splitIdx);
        double e2 = computeEfficiency(timeSeconds, heartRates, velocities, splitIdx, n - 1);

        if (Double.isNaN(e1) || Double.isNaN(e2) || e1 == 0) {
            return DecouplingResult.ineligible("CALCULATION_ERROR");
        }

        double decoupling = (e2 - e1) / e1 * 100.0;
        return DecouplingResult.of(decoupling);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private double computeHrCoverage(List<Integer> time, List<Integer> hr, int totalDuration) {
        if (totalDuration <= 0) return 0;
        double valid = 0;
        for (int i = 0; i < time.size() - 1; i++) {
            int dt = time.get(i + 1) - time.get(i);
            if (dt <= 0) continue;
            Integer v = hr.get(i);
            if (v != null && v > 0) valid += dt;
        }
        return Math.min(1.0, valid / totalDuration);
    }

    private double computeSpeedCoverage(List<Integer> time, List<Double> vel, int totalDuration) {
        if (totalDuration <= 0) return 0;
        double valid = 0;
        for (int i = 0; i < time.size() - 1; i++) {
            int dt = time.get(i + 1) - time.get(i);
            if (dt <= 0) continue;
            Double v = vel.get(i);
            if (v != null && v > 0) valid += dt;
        }
        return Math.min(1.0, valid / totalDuration);
    }

    /**
     * Returns the sample index that divides the activity into two equal halves.
     * Prefers distance-based split (more accurate on hilly terrain);
     * falls back to time-based split.
     */
    private int findSplitIndex(List<Integer> time, List<Double> distances) {
        int n = time.size();

        // Distance-based split (preferred)
        if (distances != null && distances.size() == n) {
            Double totalDist = null;
            for (int i = n - 1; i >= 0; i--) {
                Double d = distances.get(i);
                if (d != null && d > 0) { totalDist = d; break; }
            }
            if (totalDist != null) {
                double half = totalDist / 2.0;
                for (int i = 1; i < n; i++) {
                    Double d = distances.get(i);
                    if (d != null && d >= half) return i;
                }
            }
        }

        // Time-based split
        int startTime = time.get(0);
        int mid       = startTime + (time.get(n - 1) - startTime) / 2;
        for (int i = 1; i < n; i++) {
            if (time.get(i) >= mid) return i;
        }
        return n / 2;
    }

    /**
     * Time-weighted E = avgHR / avgSpeed over intervals [fromIdx..toIdx).
     * Only intervals where both HR and speed are valid (non-null, &gt; 0) are counted.
     *
     * @param toIdx exclusive upper bound of interval <em>start</em> index
     *              (accesses {@code time.get(toIdx)}, so must be &lt; list size)
     * @return E, or {@link Double#NaN} when no valid data
     */
    private double computeEfficiency(List<Integer> time, List<Integer> hr, List<Double> vel,
                                     int fromIdx, int toIdx) {
        double sumHr = 0, sumSpeed = 0, totalDt = 0;
        for (int i = fromIdx; i < toIdx; i++) {
            int    dt = time.get(i + 1) - time.get(i);
            if (dt <= 0) continue;
            Integer h = hr.get(i);
            Double  v = vel.get(i);
            if (h == null || h <= 0 || v == null || v <= 0) continue;
            sumHr    += (double) h * dt;
            sumSpeed += v * dt;
            totalDt  += dt;
        }
        if (totalDt == 0 || sumSpeed == 0) return Double.NaN;
        return (sumHr / totalDt) / (sumSpeed / totalDt);
    }
}
