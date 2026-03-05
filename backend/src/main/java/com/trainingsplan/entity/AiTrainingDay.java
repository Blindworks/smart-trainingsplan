package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_training_day")
public class AiTrainingDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private AiTrainingPlan plan;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "day_index", nullable = false)
    private Integer dayIndex;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiTrainingWorkout> workouts = new ArrayList<>();

    public AiTrainingDay() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AiTrainingPlan getPlan() { return plan; }
    public void setPlan(AiTrainingPlan plan) { this.plan = plan; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getDayIndex() { return dayIndex; }
    public void setDayIndex(Integer dayIndex) { this.dayIndex = dayIndex; }

    public List<AiTrainingWorkout> getWorkouts() { return workouts; }
    public void setWorkouts(List<AiTrainingWorkout> workouts) { this.workouts = workouts; }
}
