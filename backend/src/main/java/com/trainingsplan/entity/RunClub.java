package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "run_clubs")
public class RunClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_filename", length = 255)
    private String logoFilename;

    @Column(name = "cover_image_filename", length = 255)
    private String coverImageFilename;

    @Column(name = "location_city", length = 100)
    private String locationCity;

    @Column(name = "location_country", length = 100)
    private String locationCountry;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "meeting_point_name", length = 255)
    private String meetingPointName;

    @Column(name = "meeting_schedule", columnDefinition = "TEXT")
    private String meetingSchedule;

    @Column(name = "meeting_time")
    private LocalTime meetingTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "run_club_meeting_days",
            joinColumns = @JoinColumn(name = "club_id"))
    @Column(name = "day_of_week", length = 10)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> meetingDays = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy", nullable = false, length = 20)
    private RunClubJoinPolicy joinPolicy = RunClubJoinPolicy.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunClubStatus status = RunClubStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "social_instagram", length = 255)
    private String socialInstagram;

    @Column(name = "social_website", length = 255)
    private String socialWebsite;

    @Column(name = "social_strava", length = 255)
    private String socialStrava;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @JsonIgnore
    private User approvedBy;

    @Column(nullable = false)
    private boolean verified = false;

    public RunClub() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoFilename() { return logoFilename; }
    public void setLogoFilename(String logoFilename) { this.logoFilename = logoFilename; }

    public String getCoverImageFilename() { return coverImageFilename; }
    public void setCoverImageFilename(String coverImageFilename) { this.coverImageFilename = coverImageFilename; }

    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }

    public String getLocationCountry() { return locationCountry; }
    public void setLocationCountry(String locationCountry) { this.locationCountry = locationCountry; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getMeetingPointName() { return meetingPointName; }
    public void setMeetingPointName(String meetingPointName) { this.meetingPointName = meetingPointName; }

    public String getMeetingSchedule() { return meetingSchedule; }
    public void setMeetingSchedule(String meetingSchedule) { this.meetingSchedule = meetingSchedule; }

    public LocalTime getMeetingTime() { return meetingTime; }
    public void setMeetingTime(LocalTime meetingTime) { this.meetingTime = meetingTime; }

    public Set<DayOfWeek> getMeetingDays() { return meetingDays; }
    public void setMeetingDays(Set<DayOfWeek> meetingDays) { this.meetingDays = meetingDays; }

    public RunClubJoinPolicy getJoinPolicy() { return joinPolicy; }
    public void setJoinPolicy(RunClubJoinPolicy joinPolicy) { this.joinPolicy = joinPolicy; }

    public RunClubStatus getStatus() { return status; }
    public void setStatus(RunClubStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getSocialInstagram() { return socialInstagram; }
    public void setSocialInstagram(String socialInstagram) { this.socialInstagram = socialInstagram; }

    public String getSocialWebsite() { return socialWebsite; }
    public void setSocialWebsite(String socialWebsite) { this.socialWebsite = socialWebsite; }

    public String getSocialStrava() { return socialStrava; }
    public void setSocialStrava(String socialStrava) { this.socialStrava = socialStrava; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
