package com.trainingsplan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StravaActivityDto {
    private Long id;
    private String name;
    private String type;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("distance")
    private Double distanceMeters;

    @JsonProperty("moving_time")
    private Integer movingTimeSeconds;

    @JsonProperty("elapsed_time")
    private Integer elapsedTimeSeconds;

    @JsonProperty("total_elevation_gain")
    private Double totalElevationGain;

    @JsonProperty("average_speed")
    private Double averageSpeed;

    @JsonProperty("max_speed")
    private Double maxSpeed;

    @JsonProperty("average_heartrate")
    private Double averageHeartrate;

    @JsonProperty("max_heartrate")
    private Double maxHeartrate;

    @JsonProperty("average_watts")
    private Double averageWatts;

    private Double kilojoules;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }
    public Integer getMovingTimeSeconds() { return movingTimeSeconds; }
    public void setMovingTimeSeconds(Integer movingTimeSeconds) { this.movingTimeSeconds = movingTimeSeconds; }
    public Integer getElapsedTimeSeconds() { return elapsedTimeSeconds; }
    public void setElapsedTimeSeconds(Integer elapsedTimeSeconds) { this.elapsedTimeSeconds = elapsedTimeSeconds; }
    public Double getTotalElevationGain() { return totalElevationGain; }
    public void setTotalElevationGain(Double totalElevationGain) { this.totalElevationGain = totalElevationGain; }
    public Double getAverageSpeed() { return averageSpeed; }
    public void setAverageSpeed(Double averageSpeed) { this.averageSpeed = averageSpeed; }
    public Double getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(Double maxSpeed) { this.maxSpeed = maxSpeed; }
    public Double getAverageHeartrate() { return averageHeartrate; }
    public void setAverageHeartrate(Double averageHeartrate) { this.averageHeartrate = averageHeartrate; }
    public Double getMaxHeartrate() { return maxHeartrate; }
    public void setMaxHeartrate(Double maxHeartrate) { this.maxHeartrate = maxHeartrate; }
    public Double getAverageWatts() { return averageWatts; }
    public void setAverageWatts(Double averageWatts) { this.averageWatts = averageWatts; }
    public Double getKilojoules() { return kilojoules; }
    public void setKilojoules(Double kilojoules) { this.kilojoules = kilojoules; }
}
