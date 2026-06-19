package com.trainingsplan.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SegmentEffortDedupTest {

    // ---- normalisation: casing + surrounding/internal whitespace ----

    @Test
    void sameNameExact_produceSameKey() {
        String k1 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas");
        String k2 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas");
        assertEquals(k1, k2);
    }

    @Test
    void differentCasing_produceSameKey() {
        String k1 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas");
        String k2 = SegmentEffortDedup.identityKey(1L, "RUN", "lukas");
        assertEquals(k1, k2);
    }

    @Test
    void surroundingWhitespace_produceSameKey() {
        String k1 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas");
        String k2 = SegmentEffortDedup.identityKey(1L, "RUN", "  Lukas  ");
        assertEquals(k1, k2);
    }

    @Test
    void internalWhitespaceCollapsed_produceSameKey() {
        String k1 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas R");
        String k2 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas   R");
        assertEquals(k1, k2);
    }

    @Test
    void combinedCasingAndWhitespace_produceSameKey() {
        String k1 = SegmentEffortDedup.identityKey(1L, "RUN", "  Lukas  ");
        String k2 = SegmentEffortDedup.identityKey(1L, "RUN", "lukas");
        assertEquals(k1, k2);
    }

    // ---- different inputs must differ ----

    @Test
    void differentName_produceDifferentKey() {
        String k1 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas");
        String k2 = SegmentEffortDedup.identityKey(1L, "RUN", "Anna");
        assertNotEquals(k1, k2);
    }

    @Test
    void differentActivityType_produceDifferentKey() {
        String k1 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas");
        String k2 = SegmentEffortDedup.identityKey(1L, "RIDE", "Lukas");
        assertNotEquals(k1, k2);
    }

    @Test
    void differentChallengeId_produceDifferentKey() {
        String k1 = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas");
        String k2 = SegmentEffortDedup.identityKey(2L, "RUN", "Lukas");
        assertNotEquals(k1, k2);
    }

    // ---- output format ----

    @Test
    void keyIsNonNull32CharHexString() {
        String k = SegmentEffortDedup.identityKey(1L, "RUN", "Lukas");
        assertNotNull(k);
        assertEquals(32, k.length());
        assertTrue(k.matches("[0-9a-f]{32}"));
    }

    // ---- file content hash (SHA-256) ----

    @Test
    void fileHash_isSha256HexOfBytes() {
        // Known SHA-256 test vector for the ASCII string "abc".
        String h = SegmentEffortDedup.fileHash("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", h);
        assertEquals(64, h.length(), "SHA-256 hex is 64 chars");
        assertTrue(h.matches("[0-9a-f]{64}"));
    }

    @Test
    void fileHash_sameBytesSameHash_differentBytesDiffer() {
        String a1 = SegmentEffortDedup.fileHash("<gpx>track-A</gpx>".getBytes(StandardCharsets.UTF_8));
        String a2 = SegmentEffortDedup.fileHash("<gpx>track-A</gpx>".getBytes(StandardCharsets.UTF_8));
        String b = SegmentEffortDedup.fileHash("<gpx>track-B</gpx>".getBytes(StandardCharsets.UTF_8));
        assertEquals(a1, a2, "identical bytes must hash identically");
        assertNotEquals(a1, b, "different bytes must hash differently");
    }
}
