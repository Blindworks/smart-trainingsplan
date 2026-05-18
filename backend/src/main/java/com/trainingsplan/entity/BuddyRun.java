package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buddy_runs")
public class BuddyRun {

    public enum Visibility { FRIENDS_ONLY, PUBLIC_NEARBY, PRIVATE_INVITE }
    public enum Status { OPEN, CONFIRMED, CANCELLED, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnore
    private User creator;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "meeting_point_name", nullable = false, length = 255)
    private String meetingPointName;

    @Column(name = "meeting_latitude")
    private Double meetingLatitude;

    @Column(name = "meeting_longitude")
    private Double meetingLongitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_route_id")
    @JsonIgnore
    private CommunityRoute communityRoute;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "expected_duration_minutes")
    private Integer expectedDurationMinutes;

    @Column(name = "target_pace_min_sec_per_km")
    private Integer targetPaceMinSecPerKm;

    @Column(name = "target_pace_max_sec_per_km")
    private Integer targetPaceMaxSecPerKm;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Visibility visibility = Visibility.FRIENDS_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "buddyRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<BuddyRunParticipant> participants = new ArrayList<>();

    public BuddyRun() {}

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getMeetingPointName() { return meetingPointName; }
    public void setMeetingPointName(String meetingPointName) { this.meetingPointName = meetingPointName; }
    public Double getMeetingLatitude() { return meetingLatitude; }
    public void setMeetingLatitude(Double meetingLatitude) { this.meetingLatitude = meetingLatitude; }
    public Double getMeetingLongitude() { return meetingLongitude; }
    public void setMeetingLongitude(Double meetingLongitude) { this.meetingLongitude = meetingLongitude; }
    public CommunityRoute getCommunityRoute() { return communityRoute; }
    public void setCommunityRoute(CommunityRoute communityRoute) { this.communityRoute = communityRoute; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Integer getExpectedDurationMinutes() { return expectedDurationMinutes; }
    public void setExpectedDurationMinutes(Integer expectedDurationMinutes) { this.expectedDurationMinutes = expectedDurationMinutes; }
    public Integer getTargetPaceMinSecPerKm() { return targetPaceMinSecPerKm; }
    public void setTargetPaceMinSecPerKm(Integer targetPaceMinSecPerKm) { this.targetPaceMinSecPerKm = targetPaceMinSecPerKm; }
    public Integer getTargetPaceMaxSecPerKm() { return targetPaceMaxSecPerKm; }
    public void setTargetPaceMaxSecPerKm(Integer targetPaceMaxSecPerKm) { this.targetPaceMaxSecPerKm = targetPaceMaxSecPerKm; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<BuddyRunParticipant> getParticipants() { return participants; }
    public void setParticipants(List<BuddyRunParticipant> participants) { this.participants = participants; }
}
