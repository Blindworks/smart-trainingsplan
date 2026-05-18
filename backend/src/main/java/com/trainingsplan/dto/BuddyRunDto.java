package com.trainingsplan.dto;

import com.trainingsplan.entity.BuddyRun;
import com.trainingsplan.entity.BuddyRunParticipant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class BuddyRunDto {
    public Long id;
    public Long creatorId;
    public String creatorUsername;
    public String creatorProfileImage;
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
    public String visibility;
    public String status;
    public LocalDateTime createdAt;
    public List<BuddyRunParticipantDto> participants;
    public int joinedCount;

    public static BuddyRunDto fromEntity(BuddyRun br) {
        BuddyRunDto dto = new BuddyRunDto();
        dto.id = br.getId();
        if (br.getCreator() != null) {
            dto.creatorId = br.getCreator().getId();
            dto.creatorUsername = br.getCreator().getUsername();
            dto.creatorProfileImage = br.getCreator().getProfileImageFilename();
        }
        dto.title = br.getTitle();
        dto.description = br.getDescription();
        dto.scheduledAt = br.getScheduledAt();
        dto.meetingPointName = br.getMeetingPointName();
        dto.meetingLatitude = br.getMeetingLatitude();
        dto.meetingLongitude = br.getMeetingLongitude();
        if (br.getCommunityRoute() != null) dto.communityRouteId = br.getCommunityRoute().getId();
        dto.distanceKm = br.getDistanceKm();
        dto.expectedDurationMinutes = br.getExpectedDurationMinutes();
        dto.targetPaceMinSecPerKm = br.getTargetPaceMinSecPerKm();
        dto.targetPaceMaxSecPerKm = br.getTargetPaceMaxSecPerKm();
        dto.maxParticipants = br.getMaxParticipants();
        dto.visibility = br.getVisibility() != null ? br.getVisibility().name() : null;
        dto.status = br.getStatus() != null ? br.getStatus().name() : null;
        dto.createdAt = br.getCreatedAt();
        dto.participants = br.getParticipants().stream()
                .map(BuddyRunParticipantDto::fromEntity)
                .collect(Collectors.toList());
        dto.joinedCount = (int) br.getParticipants().stream()
                .filter(p -> p.getStatus() == BuddyRunParticipant.Status.JOINED)
                .count();
        return dto;
    }
}
