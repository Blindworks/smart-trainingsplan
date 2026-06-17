package com.trainingsplan.service;

import com.trainingsplan.util.GeoUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the polyline-corridor segment matching algorithm.
 * No Spring context — the service is instantiated directly.
 */
class SegmentMatchingServiceTest {

    private final SegmentMatchingService svc = new SegmentMatchingService();

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Straight northbound polyline: {@code count} points at constant longitude,
     * latitude increasing by {@code stepDeg} per step, elevation rising by 1 m per step.
     */
    private static List<double[]> northPolyline(double baseLat, double lng, int count, double stepDeg) {
        List<double[]> pts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pts.add(new double[]{baseLat + i * stepDeg, lng, 100.0 + i});
        }
        return pts;
    }

    /**
     * Builds a user track that walks along {@code polyline} from index {@code from} to {@code to}
     * (inclusive). Each step advances one polyline segment.  The timestamps start at
     * {@code startSec} and increment by {@code secPerPt}.
     */
    private static List<double[]> trackAlongPoly(List<double[]> polyline,
                                                  int from, int to,
                                                  int startSec, int secPerPt) {
        List<double[]> pts = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            pts.add(new double[]{polyline.get(i)[0], polyline.get(i)[1]});
        }
        return pts;
    }

    private static List<Integer> timesFrom(int startSec, int count, int secPerPt) {
        List<Integer> t = new ArrayList<>();
        for (int i = 0; i < count; i++) t.add(startSec + i * secPerPt);
        return t;
    }

    private static List<Double> flatElevations(int count) {
        List<Double> e = new ArrayList<>();
        for (int i = 0; i < count; i++) e.add(100.0);
        return e;
    }

    // ---------------------------------------------------------------------------
    // Real Heartbreak-Hill polyline (19 waypoints)
    // ---------------------------------------------------------------------------

    private static final List<double[]> HEARTBREAK_POLY = Arrays.asList(
            new double[]{50.178000000000004, 8.735460000000002},
            new double[]{50.17794000000001,  8.735460000000002},
            new double[]{50.177800000000005, 8.7354},
            new double[]{50.17719666776842,  8.73504665776016},
            new double[]{50.17659333446508,  8.73469332444453},
            new double[]{50.175990000000006, 8.734340000000001},
            new double[]{50.17527600290662,  8.733927975368319},
            new double[]{50.17456200435589,  8.73351596305086},
            new double[]{50.17384800434792,  8.733103963046968},
            new double[]{50.1731340028828,   8.73269197535599},
            new double[]{50.17242,           8.732280000000001},
            new double[]{50.1719,            8.731940000000002},
            new double[]{50.171780000000005, 8.73179},
            new double[]{50.17110500111683,  8.731279992796285},
            new double[]{50.17043,           8.730770000000001},
            new double[]{50.16996,           8.73053},
            new double[]{50.1696,            8.73032},
            new double[]{50.168940000000006, 8.730070000000001},
            new double[]{50.168850000000006, 8.73}
    );

    // ---------------------------------------------------------------------------
    // Test 1: synthetic long track with an incidental early touch + clean climb
    // ---------------------------------------------------------------------------

    @Test
    void match_extractsClimbFromLongTrackIgnoringEarlyTouch() {
        // Polyline: 10 points heading north, ~33 m apart (0.0003 deg lat ≈ 33 m)
        double baseLat = 50.000, lng = 8.000;
        double stepDeg = 0.0003;
        List<double[]> poly = northPolyline(baseLat, lng, 10, stepDeg);

        // ---- User track construction ----
        List<double[]> track = new ArrayList<>();
        List<Integer>  times = new ArrayList<>();
        List<Double>   eles  = new ArrayList<>();
        int t = 0;

        // A) Far-away approach (100 points; lat near 49°, far from segment)
        for (int i = 0; i < 100; i++) {
            track.add(new double[]{49.0 + i * 0.0001, lng});
            times.add(t++);
            eles.add(100.0);
        }

        // B) Incidental early touch: one point near the foot (poly index 0), then immediately leaves
        track.add(new double[]{baseLat + 0.000001, lng});        // on-corridor at foot
        times.add(t++);
        eles.add(100.0);
        for (int i = 0; i < 30; i++) {                           // drift away east
            track.add(new double[]{baseLat + i * 0.00005, lng + 0.01});
            times.add(t++);
            eles.add(100.0);
        }

        // C) More far-away points
        for (int i = 0; i < 50; i++) {
            track.add(new double[]{49.5 + i * 0.0001, lng - 0.05});
            times.add(t++);
            eles.add(100.0);
        }

        // D) Clean traversal: foot → top along the polyline, 1 pt per ~5 m, 1 s apart
        //    We walk through all 10 polyline points (indices 0..9).
        int climbStart = t;
        List<double[]> climbPts = trackAlongPoly(poly, 0, 9, 0, 1);
        int climbCount = climbPts.size();   // 10 points
        for (int i = 0; i < climbCount; i++) {
            track.add(climbPts.get(i));
            times.add(t++);
            eles.add(100.0 + i);
        }
        int climbDuration = climbCount - 1; // 9 seconds

        // E) Trailing far points
        for (int i = 0; i < 20; i++) {
            track.add(new double[]{baseLat + 0.01 + i * 0.0001, lng + 0.1});
            times.add(t++);
            eles.add(100.0);
        }

        SegmentMatchResult r = svc.match(track, times, eles, poly);

        assertTrue(r.isMatched(), () -> "expected match, got: " + r.getRejectionReason());
        // Elapsed must equal the clean climb duration (9 s), NOT the distance from first touch
        assertEquals(climbDuration, r.getElapsedSeconds(),
                "Should extract only the clean climb, not include the early incidental touch");
        // Cropped track size must equal climbCount (foot inclusive → bestEntry..bestExit inclusive)
        assertEquals(climbCount, r.getCroppedTrack().size(),
                "Cropped track size must match the number of climb points constructed");
        assertTrue(r.getDistanceKm() > 0.1, "distance should be positive");
        assertTrue(r.getAvgSpeedKmh() > 0);
        assertEquals(0.0, r.getCroppedTrack().get(0)[3], 1e-9,
                "cropped track must start at relative second 0");
    }

    // ---------------------------------------------------------------------------
    // Test 2: reverse traversal rejected
    // ---------------------------------------------------------------------------

    @Test
    void match_reverseTraversal_isRejected() {
        double baseLat = 50.000, lng = 8.000, stepDeg = 0.0003;
        List<double[]> poly = northPolyline(baseLat, lng, 10, stepDeg);

        // Track goes from top (index 9) to foot (index 0)
        List<double[]> track = new ArrayList<>();
        List<Integer>  times = new ArrayList<>();
        List<Double>   eles  = new ArrayList<>();
        for (int i = 9; i >= 0; i--) {
            track.add(new double[]{poly.get(i)[0], poly.get(i)[1]});
            times.add(9 - i);
            eles.add(100.0);
        }

        SegmentMatchResult r = svc.match(track, times, eles, poly);

        assertFalse(r.isMatched(), "reverse traversal must be rejected");
        assertEquals("segment_not_traversed", r.getRejectionReason());
    }

    // ---------------------------------------------------------------------------
    // Test 3: track never near segment
    // ---------------------------------------------------------------------------

    @Test
    void match_trackFarFromSegment_isRejected() {
        double baseLat = 50.000, lng = 8.000, stepDeg = 0.0003;
        List<double[]> poly = northPolyline(baseLat, lng, 10, stepDeg);

        // Track 10 km east of segment
        List<double[]> track = new ArrayList<>();
        List<Integer>  times = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            track.add(new double[]{baseLat + i * 0.0001, lng + 0.10});
            times.add(i);
        }

        SegmentMatchResult r = svc.match(track, times, flatElevations(20), poly);

        assertFalse(r.isMatched());
        assertEquals("segment_not_traversed", r.getRejectionReason());
    }

    // ---------------------------------------------------------------------------
    // Test 4: empty track
    // ---------------------------------------------------------------------------

    @Test
    void match_emptyTrack_isRejected() {
        List<double[]> poly = northPolyline(50.0, 8.0, 10, 0.0003);

        SegmentMatchResult r = svc.match(List.of(), List.of(), List.of(), poly);

        assertFalse(r.isMatched());
        assertEquals("track_too_short_or_misaligned", r.getRejectionReason());
    }

    // ---------------------------------------------------------------------------
    // Test 5: single-point track
    // ---------------------------------------------------------------------------

    @Test
    void match_singlePointTrack_isRejected() {
        List<double[]> poly = northPolyline(50.0, 8.0, 10, 0.0003);

        SegmentMatchResult r = svc.match(
                List.of(new double[]{50.0, 8.0}),
                List.of(0),
                List.of(100.0),
                poly);

        assertFalse(r.isMatched());
        assertEquals("track_too_short_or_misaligned", r.getRejectionReason());
    }

    // ---------------------------------------------------------------------------
    // Test 6: null polyline
    // ---------------------------------------------------------------------------

    @Test
    void match_nullPolyline_isRejected() {
        List<double[]> track = List.of(new double[]{50.0, 8.0}, new double[]{50.001, 8.0});
        List<Integer>  times = List.of(0, 5);

        SegmentMatchResult r = svc.match(track, times, null, null);

        assertFalse(r.isMatched());
        assertEquals("segment_not_configured", r.getRejectionReason());
    }

    // ---------------------------------------------------------------------------
    // Test 7: single-point polyline
    // ---------------------------------------------------------------------------

    @Test
    void match_singlePointPolyline_isRejected() {
        List<double[]> track = List.of(new double[]{50.0, 8.0}, new double[]{50.001, 8.0});
        List<Integer>  times = List.of(0, 5);
        List<double[]> poly  = List.of(new double[]{50.0, 8.0});

        SegmentMatchResult r = svc.match(track, times, null, poly);

        assertFalse(r.isMatched());
        assertEquals("segment_not_configured", r.getRejectionReason());
    }

    // ---------------------------------------------------------------------------
    // Test 8: REAL FIXTURE — regression test for 12.6 km run over Heartbreak Hill
    // ---------------------------------------------------------------------------

    @Test
    void match_realHeartbreakHillRun_extractsCorrectClimb() throws Exception {
        InputStream gpxStream = getClass().getResourceAsStream("/gpx/heartbreak-real-run.gpx");
        assertNotNull(gpxStream, "Test fixture not found at /gpx/heartbreak-real-run.gpx");

        byte[] bytes = gpxStream.readAllBytes();
        gpxStream.close();

        ParsedActivityData data = new GpxParsingService().parse(bytes);

        assertNotNull(data.latLngPoints, "GPX parsing must produce track points");
        assertTrue(data.latLngPoints.size() > 100,
                "Real run should have many track points, got: " + data.latLngPoints.size());

        SegmentMatchResult r = svc.match(data.latLngPoints, data.timeSeconds, data.elevations,
                HEARTBREAK_POLY);

        System.out.printf("[FIXTURE] elapsed=%d s, distance=%.3f km, speed=%.2f km/h%n",
                r.getElapsedSeconds(), r.getDistanceKm(), r.getAvgSpeedKmh());

        assertTrue(r.isMatched(),
                () -> "Expected match but got rejection: " + r.getRejectionReason()
                      + " | track points: " + data.latLngPoints.size());

        // The real climb is ~531 s. The old bug returned ~3567 s.
        assertTrue(r.getElapsedSeconds() >= 300 && r.getElapsedSeconds() <= 900,
                "Elapsed must be 300–900 s (real ~531 s), got: " + r.getElapsedSeconds());

        assertTrue(r.getDistanceKm() >= 0.85 && r.getDistanceKm() <= 1.3,
                "Distance must be 0.85–1.3 km (segment ~1.087 km), got: " + r.getDistanceKm());

        // Cropped track sanity
        assertFalse(r.getCroppedTrack().isEmpty(), "Cropped track must not be empty");
        assertEquals(0.0, r.getCroppedTrack().get(0)[3], 1e-9,
                "Cropped track must start at relative second 0");
    }
}
