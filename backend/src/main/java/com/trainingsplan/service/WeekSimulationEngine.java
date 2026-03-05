package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.Workout;
import org.springframework.stereotype.Service;
import pacr.training.simulation.dto.FatiguePointDTO;
import pacr.training.simulation.dto.WeekSimulationResultDTO;
import pacr.training.simulation.dto.WorkoutImpactDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WeekSimulationEngine {

    private static final int WEEK_DAYS = 7;
    private static final double REST_DAY_RECOVERY = 0.05;

    private final TrainingImpactEngine trainingImpactEngine;

    public WeekSimulationEngine(TrainingImpactEngine trainingImpactEngine) {
        this.trainingImpactEngine = trainingImpactEngine;
    }

    public WeekSimulationResultDTO simulateWeek(List<Workout> workouts, AthleteState initialState) {
        if (initialState == null) {
            throw new IllegalArgumentException("initialState is required");
        }

        List<Workout> safeWorkouts = workouts == null ? Collections.emptyList() : workouts;
        Map<LocalDate, List<Workout>> workoutsByDate = safeWorkouts.stream()
                .filter(Objects::nonNull)
                .filter(workout -> workout.date() != null)
                .collect(Collectors.groupingBy(Workout::date));

        LocalDate startDate = workoutsByDate.keySet().stream()
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        double fatigue = clamp(initialState.fatigue());
        double peakFatigue = fatigue;
        List<FatiguePointDTO> timeline = new ArrayList<>(WEEK_DAYS);
        List<String> riskFlags = new ArrayList<>();

        for (int dayOffset = 0; dayOffset < WEEK_DAYS; dayOffset++) {
            LocalDate day = startDate.plusDays(dayOffset);
            List<Workout> dayWorkouts = workoutsByDate.getOrDefault(day, Collections.emptyList());

            if (dayWorkouts.isEmpty()) {
                fatigue = clamp(fatigue - REST_DAY_RECOVERY);
            } else {
                for (Workout workout : dayWorkouts) {
                    AthleteState dayState = withUpdatedFatigue(initialState, fatigue);
                    WorkoutImpactDTO impact = trainingImpactEngine.predictImpact(workout, dayState);
                    fatigue = clamp(impact.getPredictedFatigue());
                    if (impact.getInjuryRisk() != WorkoutImpactDTO.InjuryRisk.LOW) {
                        riskFlags.add(day + ": " + impact.getInjuryRisk() + " injury risk");
                    }
                }
            }

            peakFatigue = Math.max(peakFatigue, fatigue);
            timeline.add(new FatiguePointDTO(day, fatigue));
        }

        return WeekSimulationResultDTO.builder()
                .fatigueTimeline(timeline)
                .peakFatigue(peakFatigue)
                .riskFlags(riskFlags)
                .build();
    }

    private AthleteState withUpdatedFatigue(AthleteState baseState, double fatigue) {
        return new AthleteState(
                baseState.goal(),
                baseState.weeksToRace(),
                fatigue,
                baseState.fitness(),
                baseState.efficiency(),
                baseState.longRunCapacity(),
                baseState.zones()
        );
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
