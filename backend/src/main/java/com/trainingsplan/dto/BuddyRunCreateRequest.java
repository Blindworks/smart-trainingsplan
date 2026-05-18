package com.trainingsplan.dto;

import java.time.LocalDateTime;

public class BuddyRunCreateRequest {
    public String title;
    public String description;
    public LocalDateTime scheduledAt;
    public String meetingPointName;
    public Double meetingLatitude;
    public Double meetingLongitude;
    public Long communityRouteId;
    public Double distanceKm;
    public Integer expectedDurationMinutes;
    public Integer targetPaceMinSecPerKm;
    public Integer targetPaceMaxSecPerKm;
    public Integer maxParticipants;
    /** FRIENDS_ONLY | PUBLIC_NEARBY | PRIVATE_INVITE */
    public String visibility;
}
