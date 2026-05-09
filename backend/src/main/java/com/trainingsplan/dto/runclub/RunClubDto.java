package com.trainingsplan.dto.runclub;

import com.trainingsplan.entity.RunClubJoinPolicy;
import com.trainingsplan.entity.RunClubStatus;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/** Public-facing club summary, safe to return to any authenticated user. */
public class RunClubDto {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String logoFilename;
    private String coverImageFilename;
    private boolean hasLogo;
    private boolean hasCover;
    private String locationCity;
    private String locationCountry;
    private Double latitude;
    private Double longitude;
    private String meetingPointName;
    private String meetingSchedule;
    private LocalTime meetingTime;
    private Set<DayOfWeek> meetingDays;
    private RunClubJoinPolicy joinPolicy;
    private RunClubStatus status;
    private String socialInstagram;
    private String socialWebsite;
    private String socialStrava;
    private boolean verified;
    private LocalDateTime createdAt;
    private Long createdById;
    private String createdByUsername;
    private int memberCount;

    public RunClubDto() {}

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

    public boolean isHasLogo() { return hasLogo; }
    public void setHasLogo(boolean hasLogo) { this.hasLogo = hasLogo; }

    public boolean isHasCover() { return hasCover; }
    public void setHasCover(boolean hasCover) { this.hasCover = hasCover; }

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

    public String getSocialInstagram() { return socialInstagram; }
    public void setSocialInstagram(String socialInstagram) { this.socialInstagram = socialInstagram; }

    public String getSocialWebsite() { return socialWebsite; }
    public void setSocialWebsite(String socialWebsite) { this.socialWebsite = socialWebsite; }

    public String getSocialStrava() { return socialStrava; }
    public void setSocialStrava(String socialStrava) { this.socialStrava = socialStrava; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}
