package com.trainingsplan.dto;

import com.trainingsplan.entity.BuddyRunParticipant;

import java.time.LocalDateTime;

public class BuddyRunParticipantDto {
    public Long id;
    public Long userId;
    public String username;
    public String profileImageFilename;
    public String status;
    public String role;
    public LocalDateTime invitedAt;
    public LocalDateTime respondedAt;

    public static BuddyRunParticipantDto fromEntity(BuddyRunParticipant p) {
        BuddyRunParticipantDto dto = new BuddyRunParticipantDto();
        dto.id = p.getId();
        if (p.getUser() != null) {
            dto.userId = p.getUser().getId();
            dto.username = p.getUser().getUsername();
            dto.profileImageFilename = p.getUser().getProfileImageFilename();
        }
        dto.status = p.getStatus() != null ? p.getStatus().name() : null;
        dto.role = p.getRole() != null ? p.getRole().name() : null;
        dto.invitedAt = p.getInvitedAt();
        dto.respondedAt = p.getRespondedAt();
        return dto;
    }
}
