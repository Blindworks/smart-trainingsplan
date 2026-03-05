package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.AthleteStateDTO;
import com.trainingsplan.dto.Workout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pacr.training.simulation.dto.WorkoutImpactDTO;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrainingImpactServiceTest {

    private AthleteStateService athleteStateService;
    private TrainingImpactEngine trainingImpactEngine;
    private TrainingImpactService service;

    @BeforeEach
    void setUp() {
        athleteStateService = mock(AthleteStateService.class);
        trainingImpactEngine = mock(TrainingImpactEngine.class);
        service = new TrainingImpactService(athleteStateService, trainingImpactEngine);
    }

    @Test
    void predictImpact_loadsAthleteStateAndCallsEngine() {
        Workout workout = new Workout(LocalDate.of(2026, 3, 5), "Tempo", 10.0, 45, 300, 155);

        AthleteStateDTO athleteStateDto = new AthleteStateDTO();
        athleteStateDto.setFatigueScore(42);
        athleteStateDto.setEfficiencyScore(0.024);
        athleteStateDto.setLongRunCapacityMinutes(110);
        athleteStateDto.setRunningZones(Collections.emptyList());
        athleteStateDto.setTrimpMetrics(new AthleteStateDTO.TrimpMetricsDTO(30.0, 220.0, 55.0));

        WorkoutImpactDTO expected = WorkoutImpactDTO.builder()
                .predictedTRIMP(120.0)
                .fatigueIncrease(0.15)
                .predictedFatigue(0.57)
                .recoveryHours(40)
                .injuryRisk(WorkoutImpactDTO.InjuryRisk.MEDIUM)
                .build();

        when(athleteStateService.getAthleteState(7L)).thenReturn(athleteStateDto);
        when(trainingImpactEngine.predictImpact(eq(workout), any(AthleteState.class))).thenReturn(expected);

        WorkoutImpactDTO result = service.predictImpact("7", workout);

        assertSame(expected, result);
        verify(athleteStateService).getAthleteState(7L);

        ArgumentCaptor<AthleteState> stateCaptor = ArgumentCaptor.forClass(AthleteState.class);
        verify(trainingImpactEngine).predictImpact(eq(workout), stateCaptor.capture());

        AthleteState mappedState = stateCaptor.getValue();
        assertEquals("Improve aerobic running fitness", mappedState.goal());
        assertEquals(8, mappedState.weeksToRace());
        assertEquals(42.0, mappedState.fatigue());
        assertEquals(55.0, mappedState.fitness());
        assertEquals(0.024, mappedState.efficiency());
        assertEquals(110, mappedState.longRunCapacity());
    }

    @Test
    void predictImpact_throwsWhenUserIdIsNotNumeric() {
        Workout workout = new Workout(LocalDate.of(2026, 3, 5), "Easy", 5.0, 30, 360, 140);

        assertThrows(IllegalArgumentException.class, () -> service.predictImpact("abc", workout));
        verifyNoInteractions(athleteStateService, trainingImpactEngine);
    }

    @Test
    void predictImpact_throwsWhenWorkoutMissing() {
        assertThrows(IllegalArgumentException.class, () -> service.predictImpact("1", null));
        verifyNoInteractions(athleteStateService, trainingImpactEngine);
    }
}
