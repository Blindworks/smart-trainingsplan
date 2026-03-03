package com.trainingsplan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityComparisonItemDto {

    private Long id;
    private String activityName;
    private String sport;
    private String trainingDate;
    private Double distanceKm;
    private Integer durationSeconds;
    private Integer movingTimeSeconds;
    private Integer averagePaceSecondsPerKm;
    private Double averageSpeedKmh;
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private Integer averagePowerWatts;
    private Integer normalizedPowerWatts;
    private Integer averageCadence;
    private Integer elevationGainM;
    private Integer calories;
    private String source;
    // From ActivityMetrics
    private Double z1Min;
    private Double z2Min;
    private Double z3Min;
    private Double z4Min;
    private Double z5Min;
    private Double strain21;
    private Double trimp;
    private Double efficiencyFactor;
    private Double decouplingPct;

    public ActivityComparisonItemDto() {}

    public ActivityComparisonItemDto(
            Long id,
            String activityName,
            String sport,
            String trainingDate,
            Double distanceKm,
            Integer durationSeconds,
            Integer movingTimeSeconds,
            Integer averagePaceSecondsPerKm,
            Double averageSpeedKmh,
            Integer averageHeartRate,
            Integer maxHeartRate,
            Integer averagePowerWatts,
            Integer normalizedPowerWatts,
            Integer averageCadence,
            Integer elevationGainM,
            Integer calories,
            String source,
            Double z1Min,
            Double z2Min,
            Double z3Min,
            Double z4Min,
            Double z5Min,
            Double strain21,
            Double trimp,
            Double efficiencyFactor,
            Double decouplingPct) {
        this.id = id;
        this.activityName = activityName;
        this.sport = sport;
        this.trainingDate = trainingDate;
        this.distanceKm = distanceKm;
        this.durationSeconds = durationSeconds;
        this.movingTimeSeconds = movingTimeSeconds;
        this.averagePaceSecondsPerKm = averagePaceSecondsPerKm;
        this.averageSpeedKmh = averageSpeedKmh;
        this.averageHeartRate = averageHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.averagePowerWatts = averagePowerWatts;
        this.normalizedPowerWatts = normalizedPowerWatts;
        this.averageCadence = averageCadence;
        this.elevationGainM = elevationGainM;
        this.calories = calories;
        this.source = source;
        this.z1Min = z1Min;
        this.z2Min = z2Min;
        this.z3Min = z3Min;
        this.z4Min = z4Min;
        this.z5Min = z5Min;
        this.strain21 = strain21;
        this.trimp = trimp;
        this.efficiencyFactor = efficiencyFactor;
        this.decouplingPct = decouplingPct;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getTrainingDate() { return trainingDate; }
    public void setTrainingDate(String trainingDate) { this.trainingDate = trainingDate; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getMovingTimeSeconds() { return movingTimeSeconds; }
    public void setMovingTimeSeconds(Integer movingTimeSeconds) { this.movingTimeSeconds = movingTimeSeconds; }

    public Integer getAveragePaceSecondsPerKm() { return averagePaceSecondsPerKm; }
    public void setAveragePaceSecondsPerKm(Integer averagePaceSecondsPerKm) { this.averagePaceSecondsPerKm = averagePaceSecondsPerKm; }

    public Double getAverageSpeedKmh() { return averageSpeedKmh; }
    public void setAverageSpeedKmh(Double averageSpeedKmh) { this.averageSpeedKmh = averageSpeedKmh; }

    public Integer getAverageHeartRate() { return averageHeartRate; }
    public void setAverageHeartRate(Integer averageHeartRate) { this.averageHeartRate = averageHeartRate; }

    public Integer getMaxHeartRate() { return maxHeartRate; }
    public void setMaxHeartRate(Integer maxHeartRate) { this.maxHeartRate = maxHeartRate; }

    public Integer getAveragePowerWatts() { return averagePowerWatts; }
    public void setAveragePowerWatts(Integer averagePowerWatts) { this.averagePowerWatts = averagePowerWatts; }

    public Integer getNormalizedPowerWatts() { return normalizedPowerWatts; }
    public void setNormalizedPowerWatts(Integer normalizedPowerWatts) { this.normalizedPowerWatts = normalizedPowerWatts; }

    public Integer getAverageCadence() { return averageCadence; }
    public void setAverageCadence(Integer averageCadence) { this.averageCadence = averageCadence; }

    public Integer getElevationGainM() { return elevationGainM; }
    public void setElevationGainM(Integer elevationGainM) { this.elevationGainM = elevationGainM; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Double getZ1Min() { return z1Min; }
    public void setZ1Min(Double z1Min) { this.z1Min = z1Min; }

    public Double getZ2Min() { return z2Min; }
    public void setZ2Min(Double z2Min) { this.z2Min = z2Min; }

    public Double getZ3Min() { return z3Min; }
    public void setZ3Min(Double z3Min) { this.z3Min = z3Min; }

    public Double getZ4Min() { return z4Min; }
    public void setZ4Min(Double z4Min) { this.z4Min = z4Min; }

    public Double getZ5Min() { return z5Min; }
    public void setZ5Min(Double z5Min) { this.z5Min = z5Min; }

    public Double getStrain21() { return strain21; }
    public void setStrain21(Double strain21) { this.strain21 = strain21; }

    public Double getTrimp() { return trimp; }
    public void setTrimp(Double trimp) { this.trimp = trimp; }

    public Double getEfficiencyFactor() { return efficiencyFactor; }
    public void setEfficiencyFactor(Double efficiencyFactor) { this.efficiencyFactor = efficiencyFactor; }

    public Double getDecouplingPct() { return decouplingPct; }
    public void setDecouplingPct(Double decouplingPct) { this.decouplingPct = decouplingPct; }
}
