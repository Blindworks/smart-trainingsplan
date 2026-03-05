package com.trainingsplan.dto;

import com.trainingsplan.service.PaceZoneService.PaceZoneDto;

import java.util.List;

/**
 * Snapshot of an athlete's current state used as input for AI prompt generation.
 *
 * @param goal             race goal description (e.g. "Sub-4h Marathon")
 * @param weeksToRace      number of full weeks remaining until race day
 * @param fatigue          acute training load / fatigue score (0–100, higher = more fatigued)
 * @param fitness          chronic training load / fitness score (0–100, higher = more fit)
 * @param efficiency       current aerobic efficiency factor (EF, m/s per bpm)
 * @param longRunCapacity  maximum long run the athlete can handle this week, in minutes
 * @param zones            pace zone configuration, ordered Z1–Z5
 */
public record AthleteState(
        String goal,
        int weeksToRace,
        double fatigue,
        double fitness,
        double efficiency,
        int longRunCapacity,
        List<PaceZoneDto> zones
) {}
