package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "strava_token")
public class StravaToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long athleteId;

    @Column(length = 1000)
    private String accessToken;

    @Column(length = 1000)
    private String refreshToken;

    private Long expiresAt;

    private String athleteName;
    private String athleteCity;
    private String profileMedium;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAthleteId() { return athleteId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
    public String getAthleteName() { return athleteName; }
    public void setAthleteName(String athleteName) { this.athleteName = athleteName; }
    public String getAthleteCity() { return athleteCity; }
    public void setAthleteCity(String athleteCity) { this.athleteCity = athleteCity; }
    public String getProfileMedium() { return profileMedium; }
    public void setProfileMedium(String profileMedium) { this.profileMedium = profileMedium; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
