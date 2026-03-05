package com.trainingsplan.service;

import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.AthleteStateDTO;
import com.trainingsplan.dto.RecentWorkoutDto;
import com.trainingsplan.dto.Workout;
import com.trainingsplan.entity.AiTrainingPlanStatus;
import com.trainingsplan.logging.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class AIPlanGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(AIPlanGeneratorService.class);

    private static final String DEFAULT_GOAL = "Improve aerobic running fitness";
    private static final int DEFAULT_WEEKS_TO_RACE = 8;
    private static final String DEFAULT_MODEL_VERSION = "unknown";

    private final PromptBuilder promptBuilder;
    private final AthleteStateService athleteStateService;
    private final RecentWorkoutService recentWorkoutService;
    private final LLMClientService llmClientService;
    private final AIPlanResponseParser aiPlanResponseParser;
    private final AIPlanValidator aiPlanValidator;
    private final String modelName;

    public AIPlanGeneratorService(
            PromptBuilder promptBuilder,
            AthleteStateService athleteStateService,
            RecentWorkoutService recentWorkoutService,
            LLMClientService llmClientService,
            AIPlanResponseParser aiPlanResponseParser,
            AIPlanValidator aiPlanValidator,
            @Value("${pacr.ai.model:unknown}") String modelName
    ) {
        this.promptBuilder = promptBuilder;
        this.athleteStateService = athleteStateService;
        this.recentWorkoutService = recentWorkoutService;
        this.llmClientService = llmClientService;
        this.aiPlanResponseParser = aiPlanResponseParser;
        this.aiPlanValidator = aiPlanValidator;
        this.modelName = modelName;
    }

    public AITrainingPlanDTO generateWeeklyPlan(UUID userId, LocalDate weekStart) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        throw new IllegalArgumentException(
                "UUID user IDs are not supported by the current data model. Use numeric user id."
        );
    }

    public AITrainingPlanDTO generateWeeklyPlan(Long userId, LocalDate weekStart) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (weekStart == null) {
            throw new IllegalArgumentException("weekStart is required");
        }

        AthleteStateDTO athleteStateDto = athleteStateService.getAthleteState(userId);
        List<RecentWorkoutDto> recentWorkoutDtos = recentWorkoutService.getRecentWorkouts(userId);

        String prompt = promptBuilder.buildWeeklyPlanPrompt(
                mapAthleteState(athleteStateDto),
                mapWorkouts(recentWorkoutDtos),
                weekStart
        );

        String validationResult = "NOT_RUN";
        Long llmResponseTimeMs = null;

        try {
            long llmStartNanos = System.nanoTime();
            String llmResponse = llmClientService.generateText(prompt);
            llmResponseTimeMs = (System.nanoTime() - llmStartNanos) / 1_000_000;

            AITrainingPlanDTO plan = aiPlanResponseParser.parse(llmResponse);
            aiPlanValidator.validate(plan, weekStart);
            validationResult = "VALID";

            if (plan.getCreatedAt() == null) {
                plan.setCreatedAt(LocalDateTime.now());
            }
            if (plan.getModelName() == null || plan.getModelName().isBlank()) {
                plan.setModelName(modelName);
            }
            if (plan.getModelVersion() == null || plan.getModelVersion().isBlank()) {
                plan.setModelVersion(DEFAULT_MODEL_VERSION);
            }
            if (plan.getStatus() == null) {
                plan.setStatus(AiTrainingPlanStatus.DRAFT);
            }

            return plan;
        } catch (AIResponseParsingException ex) {
            validationResult = "INVALID_RESPONSE";
            throw ex;
        } catch (IllegalArgumentException ex) {
            validationResult = "INVALID_PLAN";
            throw ex;
        } finally {
            log.info(
                    "event=ai_plan_generation_metrics correlationId={} userId={} weekStart={} promptLength={} llmResponseTimeMs={} validationResult={}",
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY),
                    userId,
                    weekStart,
                    prompt.length(),
                    llmResponseTimeMs,
                    validationResult
            );
        }
    }

    private AthleteState mapAthleteState(AthleteStateDTO dto) {
        double fitness = 0.0;
        if (dto.getTrimpMetrics() != null) {
            fitness = clamp(dto.getTrimpMetrics().getRolling28DayAverage(), 0.0, 100.0);
        }

        return new AthleteState(
                DEFAULT_GOAL,
                DEFAULT_WEEKS_TO_RACE,
                dto.getFatigueScore(),
                fitness,
                dto.getEfficiencyScore(),
                dto.getLongRunCapacityMinutes(),
                dto.getRunningZones()
        );
    }

    private List<Workout> mapWorkouts(List<RecentWorkoutDto> recentWorkoutDtos) {
        return recentWorkoutDtos.stream()
                .map(w -> new Workout(
                        w.date(),
                        buildWorkoutName(w),
                        w.distanceKm(),
                        null,
                        null,
                        null
                ))
                .toList();
    }

    private String buildWorkoutName(RecentWorkoutDto workoutDto) {
        if (workoutDto.trimp() == null) {
            return workoutDto.type();
        }
        return workoutDto.type() + " (TRIMP " + String.format(java.util.Locale.ROOT, "%.1f", workoutDto.trimp()) + ")";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}


