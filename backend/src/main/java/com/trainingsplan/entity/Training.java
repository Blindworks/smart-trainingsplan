package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    @Column(length = 2000)
    private String description;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id")
    @JsonIgnore
    private TrainingPlan trainingPlan;

    public Training() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public String getWorkPace() { return workPace; }
    public void setWorkPace(String workPace) { this.workPace = workPace; }

    public Integer getWorkTimeSeconds() { return workTimeSeconds; }
    public void setWorkTimeSeconds(Integer workTimeSeconds) { this.workTimeSeconds = workTimeSeconds; }

    public Integer getWorkDistanceMeters() { return workDistanceMeters; }
    public void setWorkDistanceMeters(Integer workDistanceMeters) { this.workDistanceMeters = workDistanceMeters; }

    public String getRecoveryPace() { return recoveryPace; }
    public void setRecoveryPace(String recoveryPace) { this.recoveryPace = recoveryPace; }

    public Integer getRecoveryTimeSeconds() { return recoveryTimeSeconds; }
    public void setRecoveryTimeSeconds(Integer recoveryTimeSeconds) { this.recoveryTimeSeconds = recoveryTimeSeconds; }

    public Integer getRecoveryDistanceMeters() { return recoveryDistanceMeters; }
    public void setRecoveryDistanceMeters(Integer recoveryDistanceMeters) { this.recoveryDistanceMeters = recoveryDistanceMeters; }

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
