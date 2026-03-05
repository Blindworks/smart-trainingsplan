package com.trainingsplan.service;

import com.trainingsplan.dto.Workout;
import org.springframework.stereotype.Service;
import pacr.training.simulation.dto.FatiguePointDTO;
import pacr.training.simulation.dto.WeekSimulationResultDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class WeekRiskAnalyzer {

    public static final String OVERTRAINING_RISK = "OVERTRAINING_RISK";
    public static final String FATIGUE_SPIKE = "FATIGUE_SPIKE";
    public static final String INTENSITY_CLUSTER = "INTENSITY_CLUSTER";

    private static final double OVERTRAINING_THRESHOLD = 0.90;
    private static final double FATIGUE_SPIKE_THRESHOLD = 0.20;
    private static final int HIGH_INTENSITY_CLUSTER_THRESHOLD = 2;

    public List<String> analyzeWeek(List<Workout> workouts, WeekSimulationResultDTO simulation) {
        List<String> riskFlags = new ArrayList<>();

        if (simulation != null && simulation.getPeakFatigue() > OVERTRAINING_THRESHOLD) {
            riskFlags.add(OVERTRAINING_RISK);
        }

        if (hasFatigueSpike(simulation)) {
            riskFlags.add(FATIGUE_SPIKE);
        }

        if (countHighIntensityWorkouts(workouts) > HIGH_INTENSITY_CLUSTER_THRESHOLD) {
            riskFlags.add(INTENSITY_CLUSTER);
        }

        return riskFlags;
    }

    private boolean hasFatigueSpike(WeekSimulationResultDTO simulation) {
        List<FatiguePointDTO> timeline = simulation != null && simulation.getFatigueTimeline() != null
                ? simulation.getFatigueTimeline()
                : Collections.emptyList();

        for (int i = 1; i < timeline.size(); i++) {
            double previous = timeline.get(i - 1).getFatigue();
            double current = timeline.get(i).getFatigue();
            if (current - previous > FATIGUE_SPIKE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private int countHighIntensityWorkouts(List<Workout> workouts) {
        if (workouts == null) {
            return 0;
        }

        int highIntensityCount = 0;
        for (Workout workout : workouts) {
            if (workout != null && isHighIntensity(workout.activityName())) {
                highIntensityCount++;
            }
        }
        return highIntensityCount;
    }

    private boolean isHighIntensity(String activityName) {
        if (activityName == null || activityName.isBlank()) {
            return false;
        }

        String normalized = activityName.toUpperCase(Locale.ROOT);
        return normalized.contains("Z4") || normalized.contains("Z5");
    }
}
