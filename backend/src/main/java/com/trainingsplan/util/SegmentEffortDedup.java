package com.trainingsplan.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    /**
     * Content hash (SHA-256 hex, 64 chars) of an uploaded file's raw bytes. Two uploads of the
     * exact same file produce the same hash, so a re-upload of an identical recording can be rejected.
     */
    public static String fileHash(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes == null ? new byte[0] : bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
