package com.trainingsplan.dto;

import com.trainingsplan.service.PaceZoneService.PaceZoneDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated athlete state used as input for AI planning.
 */
public class AthleteStateDTO {

    private TrimpMetricsDTO trimpMetrics;
    private int fatigueScore;
    private double efficiencyScore;
    private double weeklyLoad;
    private int longRunCapacityMinutes;
    private List<PaceZoneDto> runningZones = new ArrayList<>();

    public TrimpMetricsDTO getTrimpMetrics() {
        return trimpMetrics;
    }

    public void setTrimpMetrics(TrimpMetricsDTO trimpMetrics) {
        this.trimpMetrics = trimpMetrics;
    }

    public int getFatigueScore() {
        return fatigueScore;
    }

    public void setFatigueScore(int fatigueScore) {
        this.fatigueScore = fatigueScore;
    }

    public double getEfficiencyScore() {
        return efficiencyScore;
    }

    public void setEfficiencyScore(double efficiencyScore) {
        this.efficiencyScore = efficiencyScore;
    }

    public double getWeeklyLoad() {
        return weeklyLoad;
    }

    public void setWeeklyLoad(double weeklyLoad) {
        this.weeklyLoad = weeklyLoad;
    }

    public int getLongRunCapacityMinutes() {
        return longRunCapacityMinutes;
    }

    public void setLongRunCapacityMinutes(int longRunCapacityMinutes) {
        this.longRunCapacityMinutes = longRunCapacityMinutes;
    }

    public List<PaceZoneDto> getRunningZones() {
        return runningZones;
    }

    public void setRunningZones(List<PaceZoneDto> runningZones) {
        this.runningZones = runningZones;
    }

    public static class TrimpMetricsDTO {
        private double today;
        private double weeklyTotal;
        private double rolling28DayAverage;

        public TrimpMetricsDTO() {
        }

        public TrimpMetricsDTO(double today, double weeklyTotal, double rolling28DayAverage) {
            this.today = today;
            this.weeklyTotal = weeklyTotal;
            this.rolling28DayAverage = rolling28DayAverage;
        }

        public double getToday() {
            return today;
        }

        public void setToday(double today) {
            this.today = today;
        }

        public double getWeeklyTotal() {
            return weeklyTotal;
        }

        public void setWeeklyTotal(double weeklyTotal) {
            this.weeklyTotal = weeklyTotal;
        }

        public double getRolling28DayAverage() {
            return rolling28DayAverage;
        }

        public void setRolling28DayAverage(double rolling28DayAverage) {
            this.rolling28DayAverage = rolling28DayAverage;
        }
    }
}
