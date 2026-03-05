package com.trainingsplan.dto;

import java.time.LocalDate;

/**
 * Minimal workout summary used as context in AI prompt generation.
 *
 * @param date       date the workout was performed
 * @param type       sport or training type label (e.g. "running", "Intervalle")
 * @param distanceKm total distance in kilometres; null when not recorded
 * @param trimp      Bannister TRIMP load value; null when HR data was insufficient
 */
public record RecentWorkoutDto(
        LocalDate date,
        String type,
        Double distanceKm,
        Double trimp
) {}
