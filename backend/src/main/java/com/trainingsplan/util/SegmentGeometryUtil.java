package com.trainingsplan.util;

import java.util.ArrayList;
import java.util.List;

public final class SegmentGeometryUtil {

    private static final double MAX_PLAUSIBLE_GRADE = 35.0;

    private SegmentGeometryUtil() {}

    /** Evenly thins points to at most maxPoints, always keeping first and last. Returns input if already small. */
    public static List<double[]> downsample(List<double[]> points, int maxPoints) {
        if (points == null || points.size() <= maxPoints || maxPoints < 2) {
            return points == null ? new ArrayList<>() : new ArrayList<>(points);
        }
        List<double[]> out = new ArrayList<>(maxPoints);
        double stride = (points.size() - 1) / (double) (maxPoints - 1);
        for (int i = 0; i < maxPoints; i++) {
            out.add(points.get((int) Math.round(i * stride)));
        }
        return out;
    }

    /** Average gradient (%) = net rise / horizontal distance. 0 when distance <= 0. */
    public static double avgGradePct(double distanceM, double netRiseM) {
        if (distanceM <= 0) return 0.0;
        return netRiseM / distanceM * 100.0;
    }

    /**
     * Max sustained gradient (%) over a ~30 m sliding window, to avoid single-point GPS spikes.
     * Points are [lat, lng, ele]. Clamped to [0, 35].
     */
    public static double maxGradePct(List<double[]> points) {
        if (points == null || points.size() < 2) return 0.0;
        final double window = 30.0;
        double max = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            double dist = 0.0;
            double eleStart = points.get(i)[2];
            for (int k = i + 1; k < points.size(); k++) {
                dist += GeoUtils.haversineMeters(points.get(k - 1)[0], points.get(k - 1)[1],
                                                 points.get(k)[0], points.get(k)[1]);
                if (dist >= window) {
                    double grade = (points.get(k)[2] - eleStart) / dist * 100.0;
                    if (grade > max) max = grade;
                    break;
                }
            }
        }
        return Math.min(max, MAX_PLAUSIBLE_GRADE);
    }
}
