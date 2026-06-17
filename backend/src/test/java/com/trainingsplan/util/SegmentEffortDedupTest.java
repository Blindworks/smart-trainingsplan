package com.trainingsplan.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SegmentEffortDedupTest {

    @Test
    void sameInputs_produceSameKey() {
        String k1 = SegmentEffortDedup.key(1L, "RUN", 298, 50.17800, 8.73546);
        String k2 = SegmentEffortDedup.key(1L, "RUN", 298, 50.17800, 8.73546);
        assertEquals(k1, k2);
    }

    @Test
    void differentElapsedSeconds_produceDifferentKey() {
        String k1 = SegmentEffortDedup.key(1L, "RUN", 298, 50.17800, 8.73546);
        String k2 = SegmentEffortDedup.key(1L, "RUN", 299, 50.17800, 8.73546);
        assertNotEquals(k1, k2);
    }

    @Test
    void coordsDifferingOnlyIn7thDecimal_produceSameKey() {
        // 50.1780001 vs 50.1780009 differ only at the 7th decimal (~1 cm), must round to same 5 dp
        String k1 = SegmentEffortDedup.key(1L, "RUN", 298, 50.1780001, 8.7354600);
        String k2 = SegmentEffortDedup.key(1L, "RUN", 298, 50.1780009, 8.7354609);
        assertEquals(k1, k2);
    }

    @Test
    void differentActivityType_produceDifferentKey() {
        String k1 = SegmentEffortDedup.key(1L, "RUN", 298, 50.17800, 8.73546);
        String k2 = SegmentEffortDedup.key(1L, "RIDE", 298, 50.17800, 8.73546);
        assertNotEquals(k1, k2);
    }

    @Test
    void differentChallengeId_produceDifferentKey() {
        String k1 = SegmentEffortDedup.key(1L, "RUN", 298, 50.17800, 8.73546);
        String k2 = SegmentEffortDedup.key(2L, "RUN", 298, 50.17800, 8.73546);
        assertNotEquals(k1, k2);
    }

    @Test
    void keyIsNonNull32CharHexString() {
        String k = SegmentEffortDedup.key(1L, "RUN", 298, 50.17800, 8.73546);
        assertNotNull(k);
        assertEquals(32, k.length());
        assertTrue(k.matches("[0-9a-f]{32}"));
    }
}
