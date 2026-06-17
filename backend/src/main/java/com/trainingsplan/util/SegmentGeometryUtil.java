package com.trainingsplan.util;

import java.util.ArrayList;
import java.util.List;

public final class SegmentGeometryUtil {

    private static final double MAX_PLAUSIBLE_GRADE = 35.0;

    private SegmentGeometryUtil() {}

    /**
     * Total polyline length in metres. Points are at minimum [lat, lng, ...].
     * Returns 0 for null or single-point polylines.
     */
    public static double polylineLengthM(List<double[]> polyline) {
        if (polyline == null || polyline.size() < 2) return 0.0;
        double L = 0;
        for (int i = 1; i < polyline.size(); i++) {
            L += GeoUtils.haversineMeters(polyline.get(i - 1)[0], polyline.get(i - 1)[1],
                                          polyline.get(i)[0],     polyline.get(i)[1]);
        }
        return L;
    }

    /**
     * Projects {@code (lat, lng)} onto the nearest point on the polyline using a local
     * equirectangular approximation per segment (accurate within a few metres for segments
     * up to a few km in mid-latitudes).
     *
     * @return {@code [perpendicularDistanceMeters, progressMeters]} where progress is the
     *         cumulative along-polyline distance at the nearest projection point.
     */
    public static double[] projectOntoPolyline(double lat, double lng, List<double[]> polyline) {
        double bestD = Double.MAX_VALUE, bestProg = 0, cum = 0;
        for (int i = 0; i < polyline.size() - 1; i++) {
            double[] a = polyline.get(i), b = polyline.get(i + 1);
            double mPerLon = Math.cos(Math.toRadians(a[0])) * 111320.0;
            double mPerLat = 110540.0;
            double ox = (lng - a[1]) * mPerLon, oy = (lat - a[0]) * mPerLat;
            double bx = (b[1] - a[1]) * mPerLon, by = (b[0] - a[0]) * mPerLat;
            double segLen2 = bx * bx + by * by;
            double t = segLen2 > 0 ? (ox * bx + oy * by) / segLen2 : 0;
            t = Math.max(0, Math.min(1, t));
            double px = ox - t * bx, py = oy - t * by;
            double d = Math.hypot(px, py);
            double segLen = Math.sqrt(segLen2);
            if (d < bestD) {
                bestD = d;
                bestProg = cum + t * segLen;
            }
            cum += segLen;
        }
        return new double[]{bestD, bestProg};
    }

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
