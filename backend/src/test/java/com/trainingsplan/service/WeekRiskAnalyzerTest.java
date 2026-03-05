package com.trainingsplan.service;

import com.trainingsplan.dto.Workout;
import org.junit.jupiter.api.Test;
import pacr.training.simulation.dto.FatiguePointDTO;
import pacr.training.simulation.dto.WeekSimulationResultDTO;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeekRiskAnalyzerTest {

    private final WeekRiskAnalyzer weekRiskAnalyzer = new WeekRiskAnalyzer();

    @Test
    void analyzeWeek_addsOvertrainingRiskWhenPeakFatigueIsAboveThreshold() {
        WeekSimulationResultDTO simulation = WeekSimulationResultDTO.builder()
                .fatigueTimeline(List.of(
                        new FatiguePointDTO(LocalDate.of(2026, 3, 2), 0.8),
                        new FatiguePointDTO(LocalDate.of(2026, 3, 3), 0.91)
                ))
                .peakFatigue(0.91)
                .riskFlags(List.of())
                .build();

        List<String> flags = weekRiskAnalyzer.analyzeWeek(List.of(), simulation);

        assertTrue(flags.contains(WeekRiskAnalyzer.OVERTRAINING_RISK));
        assertEquals(1, flags.size());
    }

    @Test
    void analyzeWeek_addsFatigueSpikeWhenDayToDayIncreaseExceedsThreshold() {
        WeekSimulationResultDTO simulation = WeekSimulationResultDTO.builder()
                .fatigueTimeline(List.of(
                        new FatiguePointDTO(LocalDate.of(2026, 3, 2), 0.40),
                        new FatiguePointDTO(LocalDate.of(2026, 3, 3), 0.61)
                ))
                .peakFatigue(0.61)
                .riskFlags(List.of())
                .build();

        List<String> flags = weekRiskAnalyzer.analyzeWeek(List.of(), simulation);

        assertTrue(flags.contains(WeekRiskAnalyzer.FATIGUE_SPIKE));
        assertEquals(1, flags.size());
    }

    @Test
    void analyzeWeek_addsIntensityClusterWhenMoreThanTwoHighIntensityWorkoutsExist() {
        LocalDate day = LocalDate.of(2026, 3, 2);
        List<Workout> workouts = List.of(
                new Workout(day, "Intervals Z5", null, 50, null, null),
                new Workout(day.plusDays(1), "Threshold Z4", null, 40, null, null),
                new Workout(day.plusDays(2), "Hill Repeats Z4", null, 30, null, null)
        );

        WeekSimulationResultDTO simulation = WeekSimulationResultDTO.builder()
                .fatigueTimeline(List.of())
                .peakFatigue(0.5)
                .riskFlags(List.of())
                .build();

        List<String> flags = weekRiskAnalyzer.analyzeWeek(workouts, simulation);

        assertTrue(flags.contains(WeekRiskAnalyzer.INTENSITY_CLUSTER));
        assertEquals(1, flags.size());
    }
}
