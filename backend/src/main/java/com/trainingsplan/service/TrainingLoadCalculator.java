package com.trainingsplan.service;

import com.trainingsplan.dto.Workout;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Computes a simple TRIMP approximation from workout duration and target zone.
 *
 * <p>Formula: TRIMP = durationMinutes * zoneFactor</p>
 */
@Service
public class TrainingLoadCalculator {

    public double calculateTRIMP(Workout workout) {
        if (workout == null) {
            throw new IllegalArgumentException("workout must not be null");
        }
        if (workout.durationMinutes() == null) {
            throw new IllegalArgumentException("workout.durationMinutes must not be null");
        }

        int zoneFactor = resolveZoneFactor(workout.activityName());
        return workout.durationMinutes() * (double) zoneFactor;
    }

    private int resolveZoneFactor(String activityName) {
        if (activityName == null || activityName.isBlank()) {
            throw new IllegalArgumentException("workout.activityName must contain zone Z1-Z5");
        }

        String normalized = activityName.toUpperCase(Locale.ROOT);
        if (normalized.contains("Z1")) {
            return 1;
        }
        if (normalized.contains("Z2")) {
            return 2;
        }
        if (normalized.contains("Z3")) {
            return 3;
        }
        if (normalized.contains("Z4")) {
            return 4;
        }
        if (normalized.contains("Z5")) {
            return 5;
        }

        throw new IllegalArgumentException("Unsupported zone in workout.activityName: " + activityName);
    }
}
