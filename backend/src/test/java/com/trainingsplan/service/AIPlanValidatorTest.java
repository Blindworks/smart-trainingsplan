package com.trainingsplan.service;

import com.trainingsplan.dto.AITrainingDayDTO;
import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.dto.AIWorkoutDTO;
import com.trainingsplan.entity.AiWorkoutType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIPlanValidatorTest {

    private AIPlanValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AIPlanValidator();
    }

    @Test
    void validate_validPlan_returnsValidResult() {
        AITrainingPlanDTO plan = planWithWorkout(workout(AiWorkoutType.EASY, "Z2", 45));

        ValidationResult result = validator.validate(plan);

        assertTrue(result.isValid());
    }

    @Test
    void validate_tooManyWorkoutsPerDay_returnsInvalidResult() {
        AITrainingPlanDTO plan = basePlan();
        AITrainingDayDTO day = new AITrainingDayDTO();
        day.setDate(LocalDate.of(2026, 3, 2));
        day.setWorkouts(List.of(
                workout(AiWorkoutType.EASY, "Z2", 40),
                workout(AiWorkoutType.TEMPO, "Z3", 30),
                workout(AiWorkoutType.LONG, "Z2", 80)
        ));
        plan.setDays(List.of(day));

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("workouts count")));
    }

    @Test
    void validate_invalidTargetZone_returnsInvalidResult() {
        AITrainingPlanDTO plan = planWithWorkout(workout(AiWorkoutType.EASY, "Z9", 45));

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("targetZone")));
    }

    @Test
    void validate_nonPositiveDuration_returnsInvalidResult() {
        AITrainingPlanDTO plan = planWithWorkout(workout(AiWorkoutType.INTERVAL, "Z4", 0));

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("durationMinutes")));
    }

    @Test
    void validate_missingWorkoutType_returnsInvalidResult() {
        AITrainingPlanDTO plan = planWithWorkout(workout(null, "Z2", 30));

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains(".type")));
    }

    @Test
    void validate_withExpectedWeekStart_throwsWhenWeekStartMismatches() {
        AITrainingPlanDTO plan = planWithWorkout(workout(AiWorkoutType.EASY, "Z2", 45));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(plan, LocalDate.of(2026, 3, 9))
        );

        assertTrue(ex.getMessage().contains("weekStartDate"));
    }

    private AITrainingPlanDTO planWithWorkout(AIWorkoutDTO workout) {
        AITrainingPlanDTO plan = basePlan();

        AITrainingDayDTO day = new AITrainingDayDTO();
        day.setDate(LocalDate.of(2026, 3, 2));
        day.setWorkouts(List.of(workout));
        plan.setDays(List.of(day));

        return plan;
    }

    private AITrainingPlanDTO basePlan() {
        AITrainingPlanDTO plan = new AITrainingPlanDTO();
        plan.setWeekStartDate(LocalDate.of(2026, 3, 2));
        return plan;
    }

    private AIWorkoutDTO workout(AiWorkoutType type, String zone, Integer duration) {
        AIWorkoutDTO workout = new AIWorkoutDTO();
        workout.setType(type);
        workout.setTargetZone(zone);
        workout.setDurationMinutes(duration);
        workout.setDescription("test");
        return workout;
    }
}
