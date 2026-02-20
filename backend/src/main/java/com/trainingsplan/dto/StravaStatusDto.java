package com.trainingsplan.dto;

public class StravaStatusDto {
    private boolean connected;
    private String athleteName;
    private String athleteCity;
    private String profileMedium;

    public StravaStatusDto() {}

    public StravaStatusDto(boolean connected, String athleteName, String athleteCity, String profileMedium) {
        this.connected = connected;
        this.athleteName = athleteName;
        this.athleteCity = athleteCity;
        this.profileMedium = profileMedium;
    }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    public String getAthleteName() { return athleteName; }
    public void setAthleteName(String athleteName) { this.athleteName = athleteName; }
    public String getAthleteCity() { return athleteCity; }
    public void setAthleteCity(String athleteCity) { this.athleteCity = athleteCity; }
    public String getProfileMedium() { return profileMedium; }
    public void setProfileMedium(String profileMedium) { this.profileMedium = profileMedium; }
}
