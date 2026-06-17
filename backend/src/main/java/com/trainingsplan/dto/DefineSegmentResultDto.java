package com.trainingsplan.dto;

public record DefineSegmentResultDto(
    double startLat, double startLng, double endLat, double endLng,
    double distanceM, Integer elevationGainM, double avgGradePct, double maxGradePct,
    int sourcePoints, int polylinePoints) {}
