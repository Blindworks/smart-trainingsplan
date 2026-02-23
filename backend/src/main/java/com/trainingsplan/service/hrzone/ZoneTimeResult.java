package com.trainingsplan.service.hrzone;

/**
 * Result of a zone-time calculation.
 * When {@link #isUnknown()} is true, all numeric fields are 0/undefined
 * (e.g. no hrMax configured, streams missing or too short).
 */
public class ZoneTimeResult {

    private final boolean unknown;
    private final double z1Min;
    private final double z2Min;
    private final double z3Min;
    private final double z4Min;
    private final double z5Min;
    /** Fraction of total activity duration with valid HR data (0.0–1.0). */
    private final double hrDataCoverage;

    private ZoneTimeResult() {
        this.unknown = true;
        this.z1Min = 0; this.z2Min = 0; this.z3Min = 0;
        this.z4Min = 0; this.z5Min = 0;
        this.hrDataCoverage = 0;
    }

    public ZoneTimeResult(double z1Min, double z2Min, double z3Min,
                          double z4Min, double z5Min, double hrDataCoverage) {
        this.unknown = false;
        this.z1Min = z1Min; this.z2Min = z2Min; this.z3Min = z3Min;
        this.z4Min = z4Min; this.z5Min = z5Min;
        this.hrDataCoverage = hrDataCoverage;
    }

    public static ZoneTimeResult unknown() { return new ZoneTimeResult(); }

    public boolean isUnknown()      { return unknown; }
    public double getZ1Min()        { return z1Min; }
    public double getZ2Min()        { return z2Min; }
    public double getZ3Min()        { return z3Min; }
    public double getZ4Min()        { return z4Min; }
    public double getZ5Min()        { return z5Min; }
    public double getHrDataCoverage() { return hrDataCoverage; }

    /** Total zone minutes (Z1..Z5 combined). */
    public double totalZoneMin() { return z1Min + z2Min + z3Min + z4Min + z5Min; }
}
