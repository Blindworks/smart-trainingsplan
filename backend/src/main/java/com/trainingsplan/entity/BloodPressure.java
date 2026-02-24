package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "blood_pressure_measurements")
public class BloodPressure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "measured_at", nullable = false)
    private LocalDate measuredAt;

    @Column(name = "systolic_pressure", nullable = false)
    private Integer systolicPressure;

    @Column(name = "diastolic_pressure", nullable = false)
    private Integer diastolicPressure;

    @Column(name = "pulse_at_measurement")
    private Integer pulseAtMeasurement;

    @Column(name = "notes", length = 500)
    private String notes;

    public BloodPressure() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getMeasuredAt() { return measuredAt; }
    public void setMeasuredAt(LocalDate measuredAt) { this.measuredAt = measuredAt; }

    public Integer getSystolicPressure() { return systolicPressure; }
    public void setSystolicPressure(Integer systolicPressure) { this.systolicPressure = systolicPressure; }

    public Integer getDiastolicPressure() { return diastolicPressure; }
    public void setDiastolicPressure(Integer diastolicPressure) { this.diastolicPressure = diastolicPressure; }

    public Integer getPulseAtMeasurement() { return pulseAtMeasurement; }
    public void setPulseAtMeasurement(Integer pulseAtMeasurement) { this.pulseAtMeasurement = pulseAtMeasurement; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
