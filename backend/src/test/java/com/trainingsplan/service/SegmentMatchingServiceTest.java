package com.trainingsplan.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmentMatchingServiceTest {

    private final SegmentMatchingService svc = new SegmentMatchingService();

    // Straight northbound track: 11 points, ~33 m apart, 5 s apart.
    private static final double BASE_LAT = 50.1780;
    private static final double LNG = 8.7400;

    private List<double[]> latLng(int n) {
        List<double[]> pts = new ArrayList<>();
        for (int i = 0; i < n; i++) pts.add(new double[]{BASE_LAT + i * 0.0003, LNG});
        return pts;
    }

    private List<Integer> times(int n) {
        List<Integer> t = new ArrayList<>();
        for (int i = 0; i < n; i++) t.add(i * 5);
        return t;
    }

    private List<Double> eles(int n) {
        List<Double> e = new ArrayList<>();
        for (int i = 0; i < n; i++) e.add(100.0 + i);
        return e;
    }

    @Test
    void match_trackThroughBothGatesInOrder_isMatched() {
        // start gate = point index 2, end gate = point index 8
        double startLat = BASE_LAT + 2 * 0.0003;
        double endLat = BASE_LAT + 8 * 0.0003;

        SegmentMatchResult r = svc.match(latLng(11), times(11), eles(11),
                startLat, LNG, endLat, LNG);

        assertTrue(r.isMatched(), () -> "expected match, got: " + r.getRejectionReason());
        assertEquals(30, r.getElapsedSeconds());   // (8-2)*5
        assertEquals(7, r.getCroppedTrack().size()); // indices 2..8 inclusive
        assertTrue(r.getAvgSpeedKmh() > 0);
        // cropped track relative seconds start at 0
        assertEquals(0.0, r.getCroppedTrack().get(0)[3], 1e-9);
        assertEquals(30.0, r.getCroppedTrack().get(6)[3], 1e-9);
    }

    @Test
    void match_startGateNeverApproached_isRejected() {
        double farLat = BASE_LAT + 1.0; // ~111 km away
        double endLat = BASE_LAT + 8 * 0.0003;

        SegmentMatchResult r = svc.match(latLng(11), times(11), eles(11),
                farLat, LNG, endLat, LNG);

        assertFalse(r.isMatched());
        assertNotNull(r.getRejectionReason());
    }

    @Test
    void match_endGateBeforeStartGate_wrongDirection_isRejected() {
        // start gate = point 8, end gate = point 2 → no end point AFTER the start index
        double startLat = BASE_LAT + 8 * 0.0003;
        double endLat = BASE_LAT + 2 * 0.0003;

        SegmentMatchResult r = svc.match(latLng(11), times(11), eles(11),
                startLat, LNG, endLat, LNG);

        assertFalse(r.isMatched());
        assertNotNull(r.getRejectionReason());
    }

    @Test
    void match_emptyTrack_isRejected() {
        SegmentMatchResult r = svc.match(List.of(), List.of(), List.of(),
                BASE_LAT, LNG, BASE_LAT, LNG);
        assertFalse(r.isMatched());
    }
}
