package com.trainingsplan.dto.runclub;

public class RunClubStatsDto {

    private int memberCount;
    private double totalKmLifetime;
    private double totalKmLast30d;
    private int totalActivities;
    private double totalEventKm;
    private int totalEventCount;

    public RunClubStatsDto() {}

    public RunClubStatsDto(int memberCount, double totalKmLifetime, double totalKmLast30d,
                           int totalActivities, double totalEventKm, int totalEventCount) {
        this.memberCount = memberCount;
        this.totalKmLifetime = totalKmLifetime;
        this.totalKmLast30d = totalKmLast30d;
        this.totalActivities = totalActivities;
        this.totalEventKm = totalEventKm;
        this.totalEventCount = totalEventCount;
    }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public double getTotalKmLifetime() { return totalKmLifetime; }
    public void setTotalKmLifetime(double totalKmLifetime) { this.totalKmLifetime = totalKmLifetime; }

    public double getTotalKmLast30d() { return totalKmLast30d; }
    public void setTotalKmLast30d(double totalKmLast30d) { this.totalKmLast30d = totalKmLast30d; }

    public int getTotalActivities() { return totalActivities; }
    public void setTotalActivities(int totalActivities) { this.totalActivities = totalActivities; }

    public double getTotalEventKm() { return totalEventKm; }
    public void setTotalEventKm(double totalEventKm) { this.totalEventKm = totalEventKm; }

    public int getTotalEventCount() { return totalEventCount; }
    public void setTotalEventCount(int totalEventCount) { this.totalEventCount = totalEventCount; }
}
