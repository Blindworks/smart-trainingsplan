package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.AthleteStateDTO;
import com.trainingsplan.dto.Workout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pacr.training.simulation.dto.WeekSimulationResultDTO;

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

class WeekSimulationServiceTest {

    private WeekSimulationEngine weekSimulationEngine;
    private WeekRiskAnalyzer weekRiskAnalyzer;
    private AthleteStateService athleteStateService;
    private WeekSimulationService service;

    @BeforeEach
    void setUp() {
        weekSimulationEngine = mock(WeekSimulationEngine.class);
        weekRiskAnalyzer = mock(WeekRiskAnalyzer.class);
        athleteStateService = mock(AthleteStateService.class);

        service = new WeekSimulationService(weekSimulationEngine, weekRiskAnalyzer, athleteStateService);
    }

    @Test
    void simulateTrainingWeek_runsPipelineAndAttachesUniqueRiskFlags() {
        Long userId = 42L;
        LocalDate day = LocalDate.of(2026, 3, 2);
        List<Workout> workouts = List.of(new Workout(day, "Intervals Z5", 8.0, 40, 300, 165));

        AthleteStateDTO athleteStateDTO = new AthleteStateDTO();
        athleteStateDTO.setFatigueScore(35);
        athleteStateDTO.setEfficiencyScore(0.022);
        athleteStateDTO.setLongRunCapacityMinutes(100);
        athleteStateDTO.setTrimpMetrics(new AthleteStateDTO.TrimpMetricsDTO(10.0, 100.0, 55.0));

        WeekSimulationResultDTO simulation = WeekSimulationResultDTO.builder()
                .riskFlags(List.of("BASE", WeekRiskAnalyzer.FATIGUE_SPIKE))
                .build();

        when(athleteStateService.getAthleteState(userId)).thenReturn(athleteStateDTO);
        when(weekSimulationEngine.simulateWeek(eq(workouts), any(AthleteState.class))).thenReturn(simulation);
        when(weekRiskAnalyzer.analyzeWeek(workouts, simulation))
                .thenReturn(List.of(WeekRiskAnalyzer.FATIGUE_SPIKE, WeekRiskAnalyzer.OVERTRAINING_RISK));

        WeekSimulationResultDTO result = service.simulateTrainingWeek(userId, workouts);

        assertEquals(simulation, result);
        assertEquals(List.of("BASE", WeekRiskAnalyzer.FATIGUE_SPIKE, WeekRiskAnalyzer.OVERTRAINING_RISK), result.getRiskFlags());

        var order = inOrder(athleteStateService, weekSimulationEngine, weekRiskAnalyzer);
        order.verify(athleteStateService).getAthleteState(userId);
        order.verify(weekSimulationEngine).simulateWeek(eq(workouts), any(AthleteState.class));
        order.verify(weekRiskAnalyzer).analyzeWeek(workouts, simulation);

        verify(weekSimulationEngine).simulateWeek(eq(workouts), any(AthleteState.class));
    }

    @Test
    void simulateTrainingWeek_withUuid_throwsUnsupportedMessage() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.simulateTrainingWeek(UUID.randomUUID(), List.of())
        );

        assertTrue(ex.getMessage().contains("UUID user IDs are not supported"));
    }

    @Test
    void simulateTrainingWeek_missingUserId_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.simulateTrainingWeek((Long) null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.simulateTrainingWeek((UUID) null, List.of()));
    }
}
