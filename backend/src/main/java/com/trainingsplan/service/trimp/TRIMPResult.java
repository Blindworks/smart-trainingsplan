package com.trainingsplan.service.trimp;

/**
 * Holds the result of a Bannister TRIMP calculation.
 *
 * @param trimp         computed TRIMP value (Bannister formula)
 * @param quality       reliability rating based on HR data coverage
 * @param hrCoverage    fraction of activity duration with valid HR data (0.0–1.0)
 */
public record TRIMPResult(double trimp, TRIMPQuality quality, double hrCoverage) {}
