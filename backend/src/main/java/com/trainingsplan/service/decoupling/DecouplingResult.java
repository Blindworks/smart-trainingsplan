package com.trainingsplan.service.decoupling;

/**
 * Result of aerobic decoupling / HR drift calculation.
 *
 * <p><b>Sign convention</b>: positive decoupling means the second half was <em>less</em>
 * efficient than the first (HR drifted up relative to running speed, or speed dropped while HR
 * held steady).
 *
 * <p><b>Formula</b>: E = avgHR / avgSpeed for each half (higher = less efficient).<br>
 * Decoupling% = (E₂ − E₁) / E₁ × 100.<br>
 * Positive → second half less efficient → positive aerobic drift.
 */
public record DecouplingResult(
        boolean eligible,
        Double decouplingPct,  // null when ineligible
        String reason          // "OK" when eligible; otherwise: TOO_SHORT, HR_COVERAGE_TOO_LOW,
                               // SPEED_DATA_MISSING, INSUFFICIENT_DATA, CALCULATION_ERROR
) {
    public static DecouplingResult ineligible(String reason) {
        return new DecouplingResult(false, null, reason);
    }

    public static DecouplingResult of(double decouplingPct) {
        return new DecouplingResult(true, decouplingPct, "OK");
    }
}
