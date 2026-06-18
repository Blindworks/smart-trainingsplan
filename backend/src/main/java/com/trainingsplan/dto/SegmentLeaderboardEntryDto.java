package com.trainingsplan.dto;

public record SegmentLeaderboardEntryDto(
        Long effortId,
        int rank,
        String displayName,
        String kind,
        String category,
        int elapsedSeconds,
        String elapsedFormatted,
        Integer gapToLeaderSeconds,
        Double avgSpeedKmh,
        Integer avgPaceSecondsPerKm,
        boolean reference,
        String gender,
        String ageGroup,
        int attemptCount
) {}
