package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_buddy_preferences")
public class UserBuddyPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "buddy_discoverable", nullable = false)
    private boolean buddyDiscoverable = false;

    @Column(name = "search_radius_km", nullable = false)
    private int searchRadiusKm = 10;

    @Column(name = "pace_tolerance_percent", nullable = false)
    private int paceTolerancePercent = 15;

    /** CSV of weekday tokens, e.g. "MO,DI,MI,DO,FR,SA,SO" */
    @Column(name = "available_weekdays", length = 64)
    private String availableWeekdays;

    /** e.g. "06:00-09:00,18:00-21:00" */
    @Column(name = "available_time_ranges", length = 255)
    private String availableTimeRanges;

    @Column(name = "auto_match_enabled", nullable = false)
    private boolean autoMatchEnabled = false;

    /** Optional buddy-run-specific pace in sec/km. When set, overrides the global User.paceRef* for buddy matching. */
    @Column(name = "target_pace_sec_per_km")
    private Integer targetPaceSecPerKm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public boolean isBuddyDiscoverable() { return buddyDiscoverable; }
    public void setBuddyDiscoverable(boolean buddyDiscoverable) { this.buddyDiscoverable = buddyDiscoverable; }
    public int getSearchRadiusKm() { return searchRadiusKm; }
    public void setSearchRadiusKm(int searchRadiusKm) { this.searchRadiusKm = searchRadiusKm; }
    public int getPaceTolerancePercent() { return paceTolerancePercent; }
    public void setPaceTolerancePercent(int paceTolerancePercent) { this.paceTolerancePercent = paceTolerancePercent; }
    public String getAvailableWeekdays() { return availableWeekdays; }
    public void setAvailableWeekdays(String availableWeekdays) { this.availableWeekdays = availableWeekdays; }
    public String getAvailableTimeRanges() { return availableTimeRanges; }
    public void setAvailableTimeRanges(String availableTimeRanges) { this.availableTimeRanges = availableTimeRanges; }
    public boolean isAutoMatchEnabled() { return autoMatchEnabled; }
    public void setAutoMatchEnabled(boolean autoMatchEnabled) { this.autoMatchEnabled = autoMatchEnabled; }
    public Integer getTargetPaceSecPerKm() { return targetPaceSecPerKm; }
    public void setTargetPaceSecPerKm(Integer targetPaceSecPerKm) { this.targetPaceSecPerKm = targetPaceSecPerKm; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
