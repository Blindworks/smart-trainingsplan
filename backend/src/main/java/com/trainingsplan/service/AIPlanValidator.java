package com.trainingsplan.service;

import com.trainingsplan.dto.AITrainingDayDTO;
import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.dto.AIWorkoutDTO;
import com.trainingsplan.entity.AiWorkoutType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AIPlanValidator {

    private static final int MIN_WORKOUTS_PER_DAY = 1;
    private static final int MAX_WORKOUTS_PER_DAY = 2;
    private static final Set<String> VALID_TARGET_ZONES = Set.of("Z1", "Z2", "Z3", "Z4", "Z5");
    private static final Set<AiWorkoutType> VALID_WORKOUT_TYPES = EnumSet.allOf(AiWorkoutType.class);

    public ValidationResult validate(AITrainingPlanDTO plan) {
        List<String> errors = new ArrayList<>();

        if (plan == null) {
            errors.add("Plan must not be null");
            return ValidationResult.invalid(errors);
        }

        List<AITrainingDayDTO> days = plan.getDays();
        if (days == null) {
            errors.add("Plan days must not be null");
            return ValidationResult.invalid(errors);
        }

        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            AITrainingDayDTO day = days.get(dayIndex);
            if (day == null) {
                errors.add("days[" + dayIndex + "] must not be null");
                continue;
            }

            List<AIWorkoutDTO> workouts = day.getWorkouts();
            int workoutCount = workouts == null ? 0 : workouts.size();
            if (workoutCount < MIN_WORKOUTS_PER_DAY || workoutCount > MAX_WORKOUTS_PER_DAY) {
                errors.add("days[" + dayIndex + "] workouts count must be between "
                        + MIN_WORKOUTS_PER_DAY + " and " + MAX_WORKOUTS_PER_DAY);
            }

            if (workouts == null) {
                continue;
            }

            for (int workoutIndex = 0; workoutIndex < workouts.size(); workoutIndex++) {
                AIWorkoutDTO workout = workouts.get(workoutIndex);
                String path = "days[" + dayIndex + "].workouts[" + workoutIndex + "]";
                if (workout == null) {
                    errors.add(path + " must not be null");
                    continue;
                }

                validateWorkoutType(workout, path, errors);
                validateTargetZone(workout, path, errors);
                validateDuration(workout, path, errors);
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    public void validate(AITrainingPlanDTO plan, LocalDate expectedWeekStart) {
        ValidationResult result = validate(plan);
        List<String> errors = new ArrayList<>(result.getErrors());

        if (expectedWeekStart == null) {
            errors.add("Week start date is required");
        } else {
            if (plan == null || plan.getWeekStartDate() == null || !expectedWeekStart.equals(plan.getWeekStartDate())) {
                errors.add("Plan weekStartDate must match requested week start date");
            }

            List<AITrainingDayDTO> days = plan == null ? null : plan.getDays();
            if (days == null || days.size() != 7) {
                errors.add("Plan must contain exactly 7 days");
            } else {
                validateWeekDates(days, expectedWeekStart, errors);
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    private void validateWeekDates(List<AITrainingDayDTO> days, LocalDate weekStart, List<String> errors) {
        LocalDate weekEnd = weekStart.plusDays(6);
        Set<LocalDate> seenDates = new HashSet<>();

        for (AITrainingDayDTO day : days) {
            if (day == null || day.getDate() == null) {
                errors.add("Each plan day must include a date");
                continue;
            }
            if (day.getDate().isBefore(weekStart) || day.getDate().isAfter(weekEnd)) {
                errors.add("Plan day date must be within the requested week");
            }
            if (!seenDates.add(day.getDate())) {
                errors.add("Plan contains duplicate day entries");
            }
        }
    }

    private void validateWorkoutType(AIWorkoutDTO workout, String path, List<String> errors) {
        if (workout.getType() == null || !VALID_WORKOUT_TYPES.contains(workout.getType())) {
            errors.add(path + ".type must be a valid workout type");
        }
    }

    private void validateTargetZone(AIWorkoutDTO workout, String path, List<String> errors) {
        if (workout.getType() == AiWorkoutType.REST) {
            return;
        }

        String zone = workout.getTargetZone();
        if (zone == null || !VALID_TARGET_ZONES.contains(zone.trim().toUpperCase())) {
            errors.add(path + ".targetZone must be one of Z1, Z2, Z3, Z4, Z5");
        }
    }

    private void validateDuration(AIWorkoutDTO workout, String path, List<String> errors) {
        if (workout.getType() == AiWorkoutType.REST) {
            return;
        }

        Integer durationMinutes = workout.getDurationMinutes();
        if (durationMinutes == null || durationMinutes <= 0) {
            errors.add(path + ".durationMinutes must be greater than 0");
        }
    }
}
