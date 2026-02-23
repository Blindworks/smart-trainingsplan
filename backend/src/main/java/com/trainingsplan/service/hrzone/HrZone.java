package com.trainingsplan.service.hrzone;

/**
 * Heart rate zones expressed as fractions of a reference heart rate (hrMax for v1).
 * Lower bound is inclusive, upper bound is exclusive.
 * Z5 upper is set to 1.01 so that a clamped HR == hrMax (pct = 1.0) falls into Z5.
 *
 * Later: add LTHR-based variant by introducing a separate ZoneStrategy interface.
 */
public enum HrZone {
    Z1(0.50, 0.60),
    Z2(0.60, 0.70),
    Z3(0.70, 0.80),
    Z4(0.80, 0.90),
    Z5(0.90, 1.01); // upper = 1.01 so pct == 1.0 is included

    private final double lowerPct;
    private final double upperPct;

    HrZone(double lowerPct, double upperPct) {
        this.lowerPct = lowerPct;
        this.upperPct = upperPct;
    }

    public double getLowerPct() { return lowerPct; }
    public double getUpperPct() { return upperPct; }
}
