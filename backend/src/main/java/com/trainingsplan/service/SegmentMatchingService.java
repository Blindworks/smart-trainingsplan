package com.trainingsplan.service;

import com.trainingsplan.util.GeoUtils;
import com.trainingsplan.util.SegmentGeometryUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches an uploaded GPS track against a segment defined by a reference polyline.
 *
 * <p>Algorithm: for each contiguous on-corridor run of track points (within 30 m of any
 * polyline edge), find the sub-run that starts near the foot of the climb (progress ≤ 12 % of
 * segment length) and reaches near the top (progress ≥ 85 %), covering at least 75 % of the
 * segment length. The fastest such sub-run wins. This correctly handles activities that wander
 * near the start gate early but only climb the segment later.</p>
 */
@Service
public class SegmentMatchingService {

    /** Maximum perpendicular distance (m) from the polyline for a point to be "on corridor". */
    private static final double CORRIDOR = 30.0;
    /** Maximum number of consecutive off-corridor points before a corridor run is broken. */
    private static final int MAX_GAP = 10;
    /** Progress fraction of total length that counts as "near the foot". */
    private static final double START_BAND_FRACTION = 0.12;
    /** Minimum progress fraction that counts as "near the top". */
    private static final double END_BAND_FRACTION = 0.85;
    /** Minimum coverage fraction of total length for a valid traversal. */
    private static final double MIN_COVERAGE_FRACTION = 0.75;

    public SegmentMatchResult match(List<double[]> latLngPoints,
                                    List<Integer> timeSeconds,
                                    List<Double> elevations,
                                    List<double[]> polyline) {

        if (latLngPoints == null || latLngPoints.size() < 2
                || timeSeconds == null || timeSeconds.size() != latLngPoints.size()) {
            return SegmentMatchResult.rejected("track_too_short_or_misaligned");
        }
        if (polyline == null || polyline.size() < 2) {
            return SegmentMatchResult.rejected("segment_not_configured");
        }
        double L = SegmentGeometryUtil.polylineLengthM(polyline);
        if (L <= 0) {
            return SegmentMatchResult.rejected("segment_not_configured");
        }

        int n = latLngPoints.size();
        double[] dist = new double[n];
        double[] prog = new double[n];
        for (int i = 0; i < n; i++) {
            double[] pr = SegmentGeometryUtil.projectOntoPolyline(
                    latLngPoints.get(i)[0], latLngPoints.get(i)[1], polyline);
            dist[i] = pr[0];
            prog[i] = pr[1];
        }

        double startBand   = START_BAND_FRACTION * L;
        double endBand     = END_BAND_FRACTION   * L;
        double minCoverage = MIN_COVERAGE_FRACTION * L;

        int bestEntry = -1, bestExit = -1, bestElapsed = Integer.MAX_VALUE;

        int i = 0;
        while (i < n) {
            if (dist[i] > CORRIDOR) { i++; continue; }

            // Find the extent of this corridor run (tolerating small gaps).
            int lastOn = i, gap = 0;
            for (int k = i + 1; k < n; k++) {
                if (dist[k] <= CORRIDOR) { lastOn = k; gap = 0; }
                else { gap++; if (gap > MAX_GAP) break; }
            }

            // Entry = first on-corridor point near the foot of the segment.
            int entry = -1;
            for (int a = i; a <= lastOn; a++) {
                if (dist[a] <= CORRIDOR && prog[a] <= startBand) { entry = a; break; }
            }

            if (entry >= 0) {
                // Exit = highest-progress on-corridor point after entry.
                int exit = -1;
                double maxP = -1;
                for (int b = entry + 1; b <= lastOn; b++) {
                    if (dist[b] <= CORRIDOR && prog[b] > maxP) { maxP = prog[b]; exit = b; }
                }

                if (exit > entry && maxP >= endBand && (maxP - prog[entry]) >= minCoverage) {
                    int elapsed = timeSeconds.get(exit) - timeSeconds.get(entry);
                    if (elapsed > 0 && elapsed < bestElapsed) {
                        bestElapsed = elapsed;
                        bestEntry   = entry;
                        bestExit    = exit;
                    }
                }
            }

            i = lastOn + 1;
        }

        if (bestEntry < 0) {
            return SegmentMatchResult.rejected("segment_not_traversed");
        }

        List<double[]> cropped = new ArrayList<>();
        double distanceM = 0.0;
        int baseTime = timeSeconds.get(bestEntry);
        double[] prev = null;
        for (int idx = bestEntry; idx <= bestExit; idx++) {
            double lat = latLngPoints.get(idx)[0];
            double lng = latLngPoints.get(idx)[1];
            double ele = (elevations != null && idx < elevations.size() && elevations.get(idx) != null)
                    ? elevations.get(idx) : 0.0;
            int relSec = timeSeconds.get(idx) - baseTime;
            cropped.add(new double[]{lat, lng, ele, relSec});
            if (prev != null) {
                distanceM += GeoUtils.haversineMeters(prev[0], prev[1], lat, lng);
            }
            prev = new double[]{lat, lng};
        }

        double distanceKm  = distanceM / 1000.0;
        double avgSpeedKmh = distanceKm / (bestElapsed / 3600.0);
        int    avgPace     = distanceKm > 0 ? (int) Math.round(bestElapsed / distanceKm) : 0;

        return SegmentMatchResult.matched(bestElapsed, distanceKm, avgSpeedKmh, avgPace, cropped);
    }
}
