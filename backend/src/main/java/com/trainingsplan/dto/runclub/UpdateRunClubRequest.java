package com.trainingsplan.dto.runclub;

import com.trainingsplan.entity.RunClubJoinPolicy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/** All fields optional — only non-null values are applied during update. */
public class UpdateRunClubRequest {

    private String name;
    private String description;
    private String locationCity;
    private String locationCountry;
    private Double latitude;
    private Double longitude;
    private String meetingPointName;
    private String meetingSchedule;
    private LocalTime meetingTime;
    private Set<DayOfWeek> meetingDays;
    private RunClubJoinPolicy joinPolicy;
    private String socialInstagram;
    private String socialWebsite;
    private String socialStrava;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public String getSocialInstagram() { return socialInstagram; }
    public void setSocialInstagram(String socialInstagram) { this.socialInstagram = socialInstagram; }

    public String getSocialWebsite() { return socialWebsite; }
    public void setSocialWebsite(String socialWebsite) { this.socialWebsite = socialWebsite; }

    public String getSocialStrava() { return socialStrava; }
    public void setSocialStrava(String socialStrava) { this.socialStrava = socialStrava; }
}
