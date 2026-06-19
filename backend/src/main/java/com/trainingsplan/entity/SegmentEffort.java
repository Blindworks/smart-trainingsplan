package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "segment_efforts")
public class SegmentEffort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    @JsonIgnore
    private SegmentChallenge challenge;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EffortKind kind;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EffortCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 1;

    @Column(name = "elapsed_seconds", nullable = false)
    private Integer elapsedSeconds;

    @Column(name = "avg_speed_kmh")
    private Double avgSpeedKmh;

    @Column(name = "avg_pace_seconds_per_km")
    private Integer avgPaceSecondsPerKm;

    @Column(name = "avg_power_w")
    private Integer avgPowerW;

    @Column(name = "avg_hr")
    private Integer avgHr;

    @Column(name = "track_json", columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String trackJson;

    @Column(name = "source_format", length = 20)
    private String sourceFormat;

    @Column(name = "claimed_by_user_id")
    private Long claimedByUserId;

    @Column(name = "edit_token", length = 64)
    @JsonIgnore
    private String editToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EffortStatus status = EffortStatus.VALID;

    @Column(name = "ip_hash", length = 64)
    @JsonIgnore
    private String ipHash;

    @Column(name = "dedupe_key", length = 64)
    @JsonIgnore
    private String dedupeKey;

    @Column(name = "file_hash", length = 64)
    @JsonIgnore
    private String fileHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SegmentEffort() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SegmentChallenge getChallenge() { return challenge; }
    public void setChallenge(SegmentChallenge challenge) { this.challenge = challenge; }
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }
    public EffortKind getKind() { return kind; }
    public void setKind(EffortKind kind) { this.kind = kind; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public EffortCategory getCategory() { return category; }
    public void setCategory(EffortCategory category) { this.category = category; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Integer getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(Integer elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
    public Double getAvgSpeedKmh() { return avgSpeedKmh; }
    public void setAvgSpeedKmh(Double avgSpeedKmh) { this.avgSpeedKmh = avgSpeedKmh; }
    public Integer getAvgPaceSecondsPerKm() { return avgPaceSecondsPerKm; }
    public void setAvgPaceSecondsPerKm(Integer avgPaceSecondsPerKm) { this.avgPaceSecondsPerKm = avgPaceSecondsPerKm; }
    public Integer getAvgPowerW() { return avgPowerW; }
    public void setAvgPowerW(Integer avgPowerW) { this.avgPowerW = avgPowerW; }
    public Integer getAvgHr() { return avgHr; }
    public void setAvgHr(Integer avgHr) { this.avgHr = avgHr; }
    public String getTrackJson() { return trackJson; }
    public void setTrackJson(String trackJson) { this.trackJson = trackJson; }
    public String getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(String sourceFormat) { this.sourceFormat = sourceFormat; }
    public Long getClaimedByUserId() { return claimedByUserId; }
    public void setClaimedByUserId(Long claimedByUserId) { this.claimedByUserId = claimedByUserId; }
    public String getEditToken() { return editToken; }
    public void setEditToken(String editToken) { this.editToken = editToken; }
    public EffortStatus getStatus() { return status; }
    public void setStatus(EffortStatus status) { this.status = status; }
    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
