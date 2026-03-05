package com.trainingsplan.service;

import com.trainingsplan.dto.AITrainingDayDTO;
import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.dto.AIWorkoutDTO;
import com.trainingsplan.entity.AiTrainingPlanStatus;
import com.trainingsplan.entity.AiWorkoutType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Generates AI training plans for a given user and week.
 *
 * Currently returns a hard-coded mock plan.
 * Replace {@link #buildPlanFromLLM} with an actual LLM call when ready.
 */
@Service
public class TrainingAIService {

    // Placeholder model identifier — update when LLM integration is wired in.
    private static final String MOCK_MODEL_NAME = "mock";
    private static final String MOCK_MODEL_VERSION = "0.1";

    /**
     * Generates a weekly training plan DTO for the given user and week.
     *
     * @param userId        the ID of the user the plan is generated for
     * @param weekStartDate Monday of the target week
     * @return a populated {@link AITrainingPlanDTO} (currently mocked)
     */
    public AITrainingPlanDTO generateWeeklyPlan(Long userId, LocalDate weekStartDate) {
        return buildPlanFromLLM(userId, weekStartDate);
    }

    // -------------------------------------------------------------------------
    // LLM integration point
    // -------------------------------------------------------------------------

    /**
     * Placeholder for the LLM call. Replace this method body with the actual
     * prompt-construction → API call → response-parsing logic.
     */
    private AITrainingPlanDTO buildPlanFromLLM(Long userId, LocalDate weekStartDate) {
        AITrainingPlanDTO plan = new AITrainingPlanDTO();
        plan.setWeekStartDate(weekStartDate);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setModelName(MOCK_MODEL_NAME);
        plan.setModelVersion(MOCK_MODEL_VERSION);
        plan.setStatus(AiTrainingPlanStatus.DRAFT);
        plan.setDays(buildMockDays(weekStartDate));
        return plan;
    }

    // -------------------------------------------------------------------------
    // Mock data
    // -------------------------------------------------------------------------

    private List<AITrainingDayDTO> buildMockDays(LocalDate weekStart) {
        return List.of(
            day(weekStart, 0, workout(AiWorkoutType.EASY,   "Z2",  45, "Easy aerobic run — conversational pace")),
            day(weekStart, 1, workout(AiWorkoutType.REST,   null,  null, "Rest or light stretching")),
            day(weekStart, 2, workout(AiWorkoutType.INTERVAL, "Z4", 60, "6×800 m at 5 km pace with 90 s recovery")),
            day(weekStart, 3, workout(AiWorkoutType.EASY,   "Z2",  40, "Recovery run — keep HR below 140")),
            day(weekStart, 4, workout(AiWorkoutType.TEMPO,  "Z3",  50, "Tempo run at lactate threshold pace")),
            day(weekStart, 5, workout(AiWorkoutType.LONG,   "Z2",  90, "Long run at easy effort — build aerobic base")),
            day(weekStart, 6, workout(AiWorkoutType.REST,   null,  null, "Full rest day"))
        );
    }

    private AITrainingDayDTO day(LocalDate weekStart, int offset, AIWorkoutDTO workout) {
        AITrainingDayDTO day = new AITrainingDayDTO();
        day.setDate(weekStart.plusDays(offset));
        day.setWorkouts(List.of(workout));
        return day;
    }

    private AIWorkoutDTO workout(AiWorkoutType type, String zone, Integer duration, String description) {
        AIWorkoutDTO w = new AIWorkoutDTO();
        w.setType(type);
        w.setTargetZone(zone);
        w.setDurationMinutes(duration);
        w.setDescription(description);
        return w;
    }
}
