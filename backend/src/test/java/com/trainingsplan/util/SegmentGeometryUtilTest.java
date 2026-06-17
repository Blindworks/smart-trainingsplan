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
}
