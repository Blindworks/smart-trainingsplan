package com.trainingsplan.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public final class SegmentEffortDedup {

    private SegmentEffortDedup() {}

    /**
     * Stable fingerprint of an effort: same physical climb (same segment, sport, duration and
     * start location to ~1 m) yields the same key, so re-uploading the same run dedupes even if
     * the file bytes differ (re-export). startLat/startLng are the effort's first (foot) point.
     */
    public static String key(Long challengeId, String activityType, int elapsedSeconds,
                             double startLat, double startLng) {
        String raw = challengeId + "|" + activityType + "|" + elapsedSeconds + "|"
                + String.format(java.util.Locale.ROOT, "%.5f", startLat) + "|"
                + String.format(java.util.Locale.ROOT, "%.5f", startLng);
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
