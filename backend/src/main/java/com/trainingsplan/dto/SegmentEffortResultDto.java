package com.trainingsplan.dto;

public record SegmentEffortResultDto(
        Long effortId,
        String editToken,
        int rank,
        long totalCount,
        int elapsedSeconds,
        String elapsedFormatted,
        Integer gapToLeaderSeconds,
        double percentileBeaten,
        String status
) {}
