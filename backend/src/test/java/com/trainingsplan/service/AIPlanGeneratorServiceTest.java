package com.trainingsplan.service;

import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.dto.AthleteStateDTO;
import com.trainingsplan.dto.RecentWorkoutDto;
import com.trainingsplan.entity.AiTrainingPlanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIPlanGeneratorServiceTest {

    private PromptBuilder promptBuilder;
    private AthleteStateService athleteStateService;
    private RecentWorkoutService recentWorkoutService;
    private LLMClientService llmClientService;
    private AIPlanResponseParser aiPlanResponseParser;
    private AIPlanValidator aiPlanValidator;
    private AIPlanGeneratorService service;

    @BeforeEach
    void setUp() {
        promptBuilder = mock(PromptBuilder.class);
        athleteStateService = mock(AthleteStateService.class);
        recentWorkoutService = mock(RecentWorkoutService.class);
        llmClientService = mock(LLMClientService.class);
        aiPlanResponseParser = mock(AIPlanResponseParser.class);
        aiPlanValidator = mock(AIPlanValidator.class);

        service = new AIPlanGeneratorService(
                promptBuilder,
                athleteStateService,
                recentWorkoutService,
                llmClientService,
                aiPlanResponseParser,
                aiPlanValidator,
                "gpt-test"
        );
    }

    @Test
    void generateWeeklyPlan_runsFullPipelineAndReturnsDto() {
        Long userId = 15L;
        LocalDate weekStart = LocalDate.of(2026, 3, 2);

        AthleteStateDTO athleteState = new AthleteStateDTO();
        athleteState.setFatigueScore(35);
        athleteState.setEfficiencyScore(0.023);
        athleteState.setLongRunCapacityMinutes(100);
        athleteState.setTrimpMetrics(new AthleteStateDTO.TrimpMetricsDTO(40.0, 200.0, 55.0));

        RecentWorkoutDto workout = new RecentWorkoutDto(LocalDate.of(2026, 3, 1), "Easy Run", 8.5, 72.0);

        AITrainingPlanDTO parsedPlan = new AITrainingPlanDTO();
        parsedPlan.setWeekStartDate(weekStart);

        when(athleteStateService.getAthleteState(userId)).thenReturn(athleteState);
        when(recentWorkoutService.getRecentWorkouts(userId)).thenReturn(List.of(workout));
        when(promptBuilder.buildWeeklyPlanPrompt(any(), any(), any())).thenReturn("PROMPT");
        when(llmClientService.generateText("PROMPT")).thenReturn("{\"weekStartDate\":\"2026-03-02\",\"days\":[]}");
        when(aiPlanResponseParser.parse(any())).thenReturn(parsedPlan);

        AITrainingPlanDTO result = service.generateWeeklyPlan(userId, weekStart);

        assertEquals(parsedPlan, result);
        assertEquals("gpt-test", result.getModelName());
        assertEquals("unknown", result.getModelVersion());
        assertEquals(AiTrainingPlanStatus.DRAFT, result.getStatus());

        var inOrder = inOrder(
                athleteStateService,
                recentWorkoutService,
                promptBuilder,
                llmClientService,
                aiPlanResponseParser,
                aiPlanValidator
        );
        inOrder.verify(athleteStateService).getAthleteState(userId);
        inOrder.verify(recentWorkoutService).getRecentWorkouts(userId);
        inOrder.verify(promptBuilder).buildWeeklyPlanPrompt(any(), any(), any());
        inOrder.verify(llmClientService).generateText("PROMPT");
        inOrder.verify(aiPlanResponseParser).parse(any());
        inOrder.verify(aiPlanValidator).validate(parsedPlan, weekStart);
        verify(aiPlanValidator).validate(eq(parsedPlan), eq(weekStart));
    }

    @Test
    void generateWeeklyPlan_withUuid_throwsUnsupportedMessage() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.generateWeeklyPlan(UUID.randomUUID(), LocalDate.of(2026, 3, 2))
        );

        assertTrue(ex.getMessage().contains("UUID user IDs are not supported"));
    }

    @Test
    void generateWeeklyPlan_missingInputs_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.generateWeeklyPlan((Long) null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> service.generateWeeklyPlan(1L, null));
    }
}

