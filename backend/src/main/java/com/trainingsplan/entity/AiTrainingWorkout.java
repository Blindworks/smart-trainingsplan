package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "ai_training_workout")
public class AiTrainingWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    @JsonIgnore
    private AiTrainingDay day;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AiWorkoutType type;

    @Column(name = "target_zone", length = 100)
    private String targetZone;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "structure_json", columnDefinition = "TEXT")
    private String structureJson;

    public AiTrainingWorkout() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AiTrainingDay getDay() { return day; }
    public void setDay(AiTrainingDay day) { this.day = day; }

    public AiWorkoutType getType() { return type; }
    public void setType(AiWorkoutType type) { this.type = type; }

    public String getTargetZone() { return targetZone; }
    public void setTargetZone(String targetZone) { this.targetZone = targetZone; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStructureJson() { return structureJson; }
    public void setStructureJson(String structureJson) { this.structureJson = structureJson; }
}
