package com.trainingsplan.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class SegmentEffortDedup {

    private SegmentEffortDedup() {}

    /**
     * Identity key for a public leaderboard entry: one row per (challenge, activityType, displayName).
     * Display name is normalised — trimmed, internal whitespace collapsed to a single space,
     * lowercased — so "Lukas R", "  lukas  r  " and "LUKAS R" all map to the same key.
     */
    public static String identityKey(Long challengeId, String activityType, String displayName) {
        String norm = displayName == null ? "" : displayName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String raw = challengeId + "|" + activityType + "|" + norm;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
