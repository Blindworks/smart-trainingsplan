package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sleep_data")
public class SleepData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "recorded_at", nullable = false)
    private LocalDate recordedAt;

    @Column(name = "sleep_score")
    private Integer sleepScore;

    @Column(name = "sleep_score_7_days")
    private Integer sleepScore7Days;

    @Column(name = "resting_heart_rate")
    private Integer restingHeartRate;

    @Column(name = "body_battery")
    private Integer bodyBattery;

    @Column(name = "spo2")
    private Double spO2;

    @Column(name = "breathing_rate")
    private Double breathingRate;

    @Column(name = "hrv_status", length = 50)
    private String hrvStatus;

    @Column(name = "sleep_quality", length = 50)
    private String sleepQuality;

    @Column(name = "sleep_duration_minutes")
    private Integer sleepDurationMinutes;

    @Column(name = "sleep_need_minutes")
    private Integer sleepNeedMinutes;

    @Column(name = "bedtime")
    private LocalTime bedtime;

    @Column(name = "wake_time")
    private LocalTime wakeTime;

    public SleepData() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDate recordedAt) { this.recordedAt = recordedAt; }

    public Integer getSleepScore() { return sleepScore; }
    public void setSleepScore(Integer sleepScore) { this.sleepScore = sleepScore; }

    public Integer getSleepScore7Days() { return sleepScore7Days; }
    public void setSleepScore7Days(Integer sleepScore7Days) { this.sleepScore7Days = sleepScore7Days; }

    public Integer getRestingHeartRate() { return restingHeartRate; }
    public void setRestingHeartRate(Integer restingHeartRate) { this.restingHeartRate = restingHeartRate; }

    public Integer getBodyBattery() { return bodyBattery; }
    public void setBodyBattery(Integer bodyBattery) { this.bodyBattery = bodyBattery; }

    public Double getSpO2() { return spO2; }
    public void setSpO2(Double spO2) { this.spO2 = spO2; }

    public Double getBreathingRate() { return breathingRate; }
    public void setBreathingRate(Double breathingRate) { this.breathingRate = breathingRate; }

    public String getHrvStatus() { return hrvStatus; }
    public void setHrvStatus(String hrvStatus) { this.hrvStatus = hrvStatus; }

    public String getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(String sleepQuality) { this.sleepQuality = sleepQuality; }

    public Integer getSleepDurationMinutes() { return sleepDurationMinutes; }
    public void setSleepDurationMinutes(Integer sleepDurationMinutes) { this.sleepDurationMinutes = sleepDurationMinutes; }

    public Integer getSleepNeedMinutes() { return sleepNeedMinutes; }
    public void setSleepNeedMinutes(Integer sleepNeedMinutes) { this.sleepNeedMinutes = sleepNeedMinutes; }

    public LocalTime getBedtime() { return bedtime; }
    public void setBedtime(LocalTime bedtime) { this.bedtime = bedtime; }

    public LocalTime getWakeTime() { return wakeTime; }
    public void setWakeTime(LocalTime wakeTime) { this.wakeTime = wakeTime; }
}
