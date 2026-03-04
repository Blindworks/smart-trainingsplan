package com.trainingsplan.dto;

import java.util.List;

public class TrainingStatsDto {

    private List<Bucket> buckets;
    private double totalDistanceKm;
    private int totalDurationSeconds;
    private int totalActivityCount;

    public TrainingStatsDto() {}

    public TrainingStatsDto(List<Bucket> buckets, double totalDistanceKm, int totalDurationSeconds, int totalActivityCount) {
        this.buckets = buckets;
        this.totalDistanceKm = totalDistanceKm;
        this.totalDurationSeconds = totalDurationSeconds;
        this.totalActivityCount = totalActivityCount;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<Bucket> buckets) {
        this.buckets = buckets;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(int totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public int getTotalActivityCount() {
        return totalActivityCount;
    }

    public void setTotalActivityCount(int totalActivityCount) {
        this.totalActivityCount = totalActivityCount;
    }

    // -------------------------------------------------------------------------

    public static class Bucket {

        private String label;
        private String startDate;
        private String endDate;
        private double distanceKm;
        private int durationSeconds;
        private int elevationGainM;
        private int activityCount;

        public Bucket() {}

        public Bucket(String label, String startDate, String endDate,
                      double distanceKm, int durationSeconds, int elevationGainM, int activityCount) {
            this.label = label;
            this.startDate = startDate;
            this.endDate = endDate;
            this.distanceKm = distanceKm;
            this.durationSeconds = durationSeconds;
            this.elevationGainM = elevationGainM;
            this.activityCount = activityCount;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(double distanceKm) {
            this.distanceKm = distanceKm;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public int getElevationGainM() {
            return elevationGainM;
        }

        public void setElevationGainM(int elevationGainM) {
            this.elevationGainM = elevationGainM;
        }

        public int getActivityCount() {
            return activityCount;
        }

        public void setActivityCount(int activityCount) {
            this.activityCount = activityCount;
        }
    }
}
