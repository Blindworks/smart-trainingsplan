package com.trainingsplan.service;

import com.trainingsplan.util.GeoUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches an uploaded GPS track against a segment defined by a start gate and an end gate.
 * The segment effort is the sub-track between the closest approach to the start gate and the
 * subsequent closest approach to the end gate.
 */
@Service
public class SegmentMatchingService {

    /** A track point must come within this distance (m) of a gate to count as crossing it. */
    private static final double GATE_RADIUS_M = 35.0;

    public SegmentMatchResult match(List<double[]> latLngPoints,
                                    List<Integer> timeSeconds,
                                    List<Double> elevations,
                                    double startLat, double startLng,
                                    double endLat, double endLng) {

        if (latLngPoints == null || latLngPoints.size() < 2
                || timeSeconds == null || timeSeconds.size() != latLngPoints.size()) {
            return SegmentMatchResult.rejected("track_too_short_or_misaligned");
        }

        int entryIdx = closestIndexWithinRadius(latLngPoints, 0, startLat, startLng);
        if (entryIdx < 0) {
            return SegmentMatchResult.rejected("start_gate_not_reached");
        }

        int exitIdx = closestIndexWithinRadius(latLngPoints, entryIdx + 1, endLat, endLng);
        if (exitIdx < 0) {
            return SegmentMatchResult.rejected("end_gate_not_reached_after_start");
        }

        int elapsed = timeSeconds.get(exitIdx) - timeSeconds.get(entryIdx);
        if (elapsed <= 0) {
            return SegmentMatchResult.rejected("non_positive_elapsed_time");
        }

        List<double[]> cropped = new ArrayList<>();
        double distanceM = 0.0;
        int baseTime = timeSeconds.get(entryIdx);
        double[] prev = null;
        for (int i = entryIdx; i <= exitIdx; i++) {
            double lat = latLngPoints.get(i)[0];
            double lng = latLngPoints.get(i)[1];
            double ele = (elevations != null && i < elevations.size() && elevations.get(i) != null)
                    ? elevations.get(i) : 0.0;
            int relSec = timeSeconds.get(i) - baseTime;
            cropped.add(new double[]{lat, lng, ele, relSec});
            if (prev != null) {
                distanceM += GeoUtils.haversineMeters(prev[0], prev[1], lat, lng);
            }
            prev = new double[]{lat, lng};
        }

        double distanceKm = distanceM / 1000.0;
        double avgSpeedKmh = distanceKm / (elapsed / 3600.0);
        int avgPace = distanceKm > 0 ? (int) Math.round(elapsed / distanceKm) : 0;

        return SegmentMatchResult.matched(elapsed, distanceKm, avgSpeedKmh, avgPace, cropped);
    }

    /**
     * Returns the index (>= fromIdx) of the point closest to the gate, or -1 if no point
     * comes within {@link #GATE_RADIUS_M}.
     */
    private int closestIndexWithinRadius(List<double[]> points, int fromIdx, double gateLat, double gateLng) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = fromIdx; i < points.size(); i++) {
            double d = GeoUtils.haversineMeters(points.get(i)[0], points.get(i)[1], gateLat, gateLng);
            if (d <= GATE_RADIUS_M && d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }
}
