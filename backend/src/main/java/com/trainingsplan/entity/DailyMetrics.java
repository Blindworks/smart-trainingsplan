package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Aggregated daily training strain for a single user.
 * Recomputed whenever an {@link ActivityMetrics} record is saved for that day.
 * The combination of (user_id, date) is unique.
 */
@Entity
@Table(name = "daily_metrics",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_daily_metrics_user_date",
               columnNames = {"user_id", "date"}))
public class DailyMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** Sum of strain21 values for all activities on this day. */
    @Column(name = "daily_strain21")
    private Double dailyStrain21;

    /** Sum of TRIMP values for all activities on this day. */
    @Column(name = "daily_trimp")
    private Double dailyTrimp;

    /**
     * Rolling 7-day average Efficiency Factor (m/s per bpm).
     * NULL when no eligible activities in the 7-day window ending on {@code date}.
     */
    @Column(name = "ef7")
    private Double ef7;

    /**
     * Rolling 28-day average Efficiency Factor (m/s per bpm).
     * NULL when no eligible activities in the 28-day window ending on {@code date}.
     */
    @Column(name = "ef28")
    private Double ef28;

    public DailyMetrics() {}

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Double getDailyStrain21() { return dailyStrain21; }
    public void setDailyStrain21(Double dailyStrain21) { this.dailyStrain21 = dailyStrain21; }

    public Double getDailyTrimp() { return dailyTrimp; }
    public void setDailyTrimp(Double dailyTrimp) { this.dailyTrimp = dailyTrimp; }

    public Double getEf7() { return ef7; }
    public void setEf7(Double ef7) { this.ef7 = ef7; }

    public Double getEf28() { return ef28; }
    public void setEf28(Double ef28) { this.ef28 = ef28; }
}
