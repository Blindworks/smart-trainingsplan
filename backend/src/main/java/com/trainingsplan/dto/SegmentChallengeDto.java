package com.trainingsplan.dto;

import java.time.LocalDate;

public record SegmentChallengeDto(
        Long id,
        String slug,
        String name,
        String subtitle,
        LocalDate eventDate,
        Double distanceM,
        Integer elevationGainM,
        Double avgGradePct,
        Double maxGradePct,
        String polylineJson,
        String terrainAssetRef,
        long rideCount,
        long runCount
) {}
