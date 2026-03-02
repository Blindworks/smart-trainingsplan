package com.trainingsplan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.DayOfWeek;

@Entity
@Table(name = "trainings")
public class Training {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "training_description_id")
    private TrainingDescription trainingDescription;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "intensity_level")
    private String intensityLevel;

    @Column(name = "training_type")
    private String trainingType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id")
    @JsonIgnore
    private TrainingPlan trainingPlan;

    public Training() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TrainingDescription getTrainingDescription() { return trainingDescription; }
    public void setTrainingDescription(TrainingDescription trainingDescription) {
        this.trainingDescription = trainingDescription;
    }

    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getIntensityLevel() { return intensityLevel; }
    public void setIntensityLevel(String intensityLevel) { this.intensityLevel = intensityLevel; }

    public String getTrainingType() { return trainingType; }
    public void setTrainingType(String trainingType) { this.trainingType = trainingType; }

    public TrainingPlan getTrainingPlan() { return trainingPlan; }
    public void setTrainingPlan(TrainingPlan trainingPlan) { this.trainingPlan = trainingPlan; }

    @JsonProperty("trainingPlanName")
    public String getTrainingPlanName() {
        return trainingPlan != null ? trainingPlan.getName() : null;
    }

    @JsonProperty("trainingPlanId")
    public Long getTrainingPlanId() {
        return trainingPlan != null ? trainingPlan.getId() : null;
    }
}
