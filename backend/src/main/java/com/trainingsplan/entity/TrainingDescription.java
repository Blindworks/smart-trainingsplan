package com.trainingsplan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "training_descriptions")
public class TrainingDescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 5000)
    private String detailedInstructions;

    @Column(length = 1000)
    private String warmupInstructions;

    @Column(length = 1000)
    private String cooldownInstructions;

    @Column(length = 2000)
    private String equipment;

    @Column(length = 1000)
    private String tips;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "difficulty_level")
    private String difficultyLevel;

    @Column(name = "work_pace", length = 50)
    private String workPace;

    @Column(name = "work_time_seconds")
    private Integer workTimeSeconds;

    @Column(name = "work_distance_meters")
    private Integer workDistanceMeters;

    @Column(name = "recovery_pace", length = 50)
    private String recoveryPace;

    @Column(name = "recovery_time_seconds")
    private Integer recoveryTimeSeconds;

    @Column(name = "recovery_distance_meters")
    private Integer recoveryDistanceMeters;

    public TrainingDescription() {}

    public TrainingDescription(String name, String detailedInstructions) {
        this.name = name;
        this.detailedInstructions = detailedInstructions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetailedInstructions() {
        return detailedInstructions;
    }

    public void setDetailedInstructions(String detailedInstructions) {
        this.detailedInstructions = detailedInstructions;
    }

    public String getWarmupInstructions() {
        return warmupInstructions;
    }

    public void setWarmupInstructions(String warmupInstructions) {
        this.warmupInstructions = warmupInstructions;
    }

    public String getCooldownInstructions() {
        return cooldownInstructions;
    }

    public void setCooldownInstructions(String cooldownInstructions) {
        this.cooldownInstructions = cooldownInstructions;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getWorkPace() {
        return workPace;
    }

    public void setWorkPace(String workPace) {
        this.workPace = workPace;
    }

    public Integer getWorkTimeSeconds() {
        return workTimeSeconds;
    }

    public void setWorkTimeSeconds(Integer workTimeSeconds) {
        this.workTimeSeconds = workTimeSeconds;
    }

    public Integer getWorkDistanceMeters() {
        return workDistanceMeters;
    }

    public void setWorkDistanceMeters(Integer workDistanceMeters) {
        this.workDistanceMeters = workDistanceMeters;
    }

    public String getRecoveryPace() {
        return recoveryPace;
    }

    public void setRecoveryPace(String recoveryPace) {
        this.recoveryPace = recoveryPace;
    }

    public Integer getRecoveryTimeSeconds() {
        return recoveryTimeSeconds;
    }

    public void setRecoveryTimeSeconds(Integer recoveryTimeSeconds) {
        this.recoveryTimeSeconds = recoveryTimeSeconds;
    }

    public Integer getRecoveryDistanceMeters() {
        return recoveryDistanceMeters;
    }

    public void setRecoveryDistanceMeters(Integer recoveryDistanceMeters) {
        this.recoveryDistanceMeters = recoveryDistanceMeters;
    }
}