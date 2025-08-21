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
}