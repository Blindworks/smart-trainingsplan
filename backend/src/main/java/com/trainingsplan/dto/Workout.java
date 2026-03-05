package com.trainingsplan.dto;

import java.time.LocalDate;

/**
 * Lightweight summary of a completed workout used as context in AI prompt generation.
 *
 * @param date                    date the workout was performed
 * @param activityName            display name or type label (e.g. "Langer Lauf", "Intervalle")
 * @param distanceKm              total distance in kilometres; null when not available
 * @param durationMinutes         total elapsed time in minutes; null when not available
 * @param averagePaceSecondsPerKm average pace in seconds per km; null when not available
 * @param averageHeartRate        average heart rate in bpm; null when not available
 */
public record Workout(
        LocalDate date,
        String activityName,
        Double distanceKm,
        Integer durationMinutes,
        Integer averagePaceSecondsPerKm,
        Integer averageHeartRate
) {
    /** Convenience factory from a {@link com.trainingsplan.entity.CompletedTraining}. */
    public static Workout from(com.trainingsplan.entity.CompletedTraining ct) {
        Integer durationMinutes = ct.getDurationSeconds() != null
                ? ct.getDurationSeconds() / 60
                : null;
        String name = ct.getActivityName() != null
                ? ct.getActivityName()
                : ct.getTrainingType();
        return new Workout(
                ct.getTrainingDate(),
                name,
                ct.getDistanceKm(),
                durationMinutes,
                ct.getAveragePaceSecondsPerKm(),
                ct.getAverageHeartRate()
        );
    }
}
