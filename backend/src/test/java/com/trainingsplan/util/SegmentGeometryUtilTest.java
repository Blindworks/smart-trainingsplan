package com.trainingsplan.util;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SegmentGeometryUtilTest {

    @Test
    void downsample_returnsInputWhenSizeLteMaxPoints() {
        List<double[]> pts = List.of(new double[]{1,2}, new double[]{3,4}, new double[]{5,6});
        List<double[]> result = SegmentGeometryUtil.downsample(pts, 5);
        assertEquals(3, result.size());
    }

    @Test
    void downsample_returnsExactlyMaxPointsWhenLarger() {
        List<double[]> pts = new ArrayList<>();
        for (int i = 0; i < 100; i++) pts.add(new double[]{i, i});
        List<double[]> result = SegmentGeometryUtil.downsample(pts, 10);
        assertEquals(10, result.size());
        assertArrayEquals(pts.get(0), result.get(0), 1e-9);
        assertArrayEquals(pts.get(99), result.get(result.size() - 1), 1e-9);
    }

    @Test
    void avgGradePct_correctValue() {
        assertEquals(6.0, SegmentGeometryUtil.avgGradePct(1000, 60), 1e-9);
    }

    @Test
    void avgGradePct_zeroWhenDistanceZero() {
        assertEquals(0.0, SegmentGeometryUtil.avgGradePct(0, 50), 1e-9);
    }

    @Test
    void maxGradePct_detectsSteepStretch() {
        // Build a track at ~50° lat using small lat increments (~0.0001 deg ≈ 11 m)
        // Points [lat, lng, ele]: gentle climb for most, steep over ~30 m window
        List<double[]> pts = new ArrayList<>();
        double lat = 50.0, lng = 10.0;
        // 10 gentle points: +0.5 m ele per ~11 m => ~4.5%
        for (int i = 0; i < 10; i++) {
            pts.add(new double[]{lat + i * 0.0001, lng, i * 0.5});
        }
        // 3 steep points: +5 m ele per ~11 m => ~45% but clamped to 35%
        double baseLat = lat + 10 * 0.0001;
        double baseEle = 10 * 0.5;
        for (int i = 1; i <= 3; i++) {
            pts.add(new double[]{baseLat + i * 0.0001, lng, baseEle + i * 5.0});
        }
        // 5 more gentle points
        double lastLat = baseLat + 3 * 0.0001;
        double lastEle = baseEle + 15.0;
        for (int i = 1; i <= 5; i++) {
            pts.add(new double[]{lastLat + i * 0.0001, lng, lastEle + i * 0.5});
        }

        double result = SegmentGeometryUtil.maxGradePct(pts);
        // Should be above the gentle grade (~4.5%) and at most 35.0
        assertTrue(result > 4.5, "expected max grade > gentle grade, got " + result);
        assertTrue(result <= 35.0, "expected grade clamped at 35, got " + result);
    }

    @Test
    void maxGradePct_returnsZeroForNullOrSmall() {
        assertEquals(0.0, SegmentGeometryUtil.maxGradePct(null));
        assertEquals(0.0, SegmentGeometryUtil.maxGradePct(List.of(new double[]{1,2,3})));
    }

    // -----------------------------------------------------------------------
    // polylineLengthM tests
    // -----------------------------------------------------------------------

    @Test
    void polylineLengthM_nullOrSinglePoint_returnsZero() {
        assertEquals(0.0, SegmentGeometryUtil.polylineLengthM(null));
        assertEquals(0.0, SegmentGeometryUtil.polylineLengthM(List.of(new double[]{50.0, 8.0})));
    }

    @Test
    void polylineLengthM_meridianSegment_approx1113m() {
        // [50.00, 8.0] → [50.01, 8.0]: 0.01° latitude ≈ 1113 m (110_540 m/deg × 0.01)
        List<double[]> poly = List.of(new double[]{50.0, 8.0}, new double[]{50.01, 8.0});
        double length = SegmentGeometryUtil.polylineLengthM(poly);
        assertEquals(1113.0, length, 5.0, "Expected ~1113 m for 0.01° lat at 50°N, got " + length);
    }

    // -----------------------------------------------------------------------
    // projectOntoPolyline tests
    // -----------------------------------------------------------------------

    @Test
    void projectOntoPolyline_pointOnLine_zeroDistance() {
        List<double[]> poly = List.of(new double[]{50.0, 8.0}, new double[]{50.01, 8.0});
        // Midpoint of the segment
        double[] result = SegmentGeometryUtil.projectOntoPolyline(50.005, 8.0, poly);
        assertEquals(0.0,   result[0], 1.0,  "Perpendicular distance should be ~0 m");
        assertEquals(556.0, result[1], 10.0, "Progress should be ~556 m (half of ~1113 m)");
    }

    @Test
    void projectOntoPolyline_pointOffLine_correctDistance() {
        List<double[]> poly = List.of(new double[]{50.0, 8.0}, new double[]{50.01, 8.0});
        // 0.001° east at 50°N: dist = 0.001 * cos(50°) * 111320 ≈ 71.5 m
        double[] result = SegmentGeometryUtil.projectOntoPolyline(50.005, 8.0010, poly);
        assertEquals(71.5,  result[0], 5.0,  "Perpendicular distance should be ~71 m, got " + result[0]);
        assertEquals(556.0, result[1], 10.0, "Progress should still be ~556 m (projection is orthogonal)");
    }
}
