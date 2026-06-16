package com.trainingsplan.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void haversine_samepoint_isZero() {
        assertEquals(0.0, GeoUtils.haversineMeters(50.178, 8.74, 50.178, 8.74), 1e-6);
    }

    @Test
    void haversine_oneDegreeLat_isAboutOneEleventhKm() {
        // ~0.0003 deg latitude ≈ 33.3 m
        double d = GeoUtils.haversineMeters(50.0000, 8.0, 50.0003, 8.0);
        assertEquals(33.3, d, 1.5);
    }

    @Test
    void haversine_knownDistance_frankfurtToOffenbach() {
        // Frankfurt Hbf (50.1070, 8.6638) → Offenbach (50.0997, 8.7765): ~8.1 km
        double d = GeoUtils.haversineMeters(50.1070, 8.6638, 50.0997, 8.7765);
        assertEquals(8100, d, 400);
    }
}
