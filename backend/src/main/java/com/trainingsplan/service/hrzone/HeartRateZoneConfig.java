package com.trainingsplan.service.hrzone;

/**
 * Immutable zone configuration based on hrMax (v1).
 * Encapsulates zone boundary logic so a future LTHR variant can be added
 * by providing a different implementation without changing callers.
 */
public class HeartRateZoneConfig {

    private final int hrMax;

    public HeartRateZoneConfig(int hrMax) {
        if (hrMax <= 0) throw new IllegalArgumentException("hrMax must be > 0, got " + hrMax);
        this.hrMax = hrMax;
    }

    public static HeartRateZoneConfig fromHrMax(int hrMax) {
        return new HeartRateZoneConfig(hrMax);
    }

    public int getHrMax() { return hrMax; }

    /**
     * Classifies the given HR value into a zone.
     * <ul>
     *   <li>HR &le; 0 → {@code null} (invalid / missing)</li>
     *   <li>HR &gt; hrMax → clamped to hrMax before classification</li>
     *   <li>HR &lt; 50 % hrMax → {@code null} (below Z1)</li>
     * </ul>
     */
    public HrZone zoneFor(int hr) {
        if (hr <= 0) return null;
        int clamped = Math.min(hr, hrMax);
        double pct = (double) clamped / hrMax;
        for (HrZone zone : HrZone.values()) {
            if (pct >= zone.getLowerPct() && pct < zone.getUpperPct()) {
                return zone;
            }
        }
        return null; // below Z1 lower bound (< 50 %)
    }
}
