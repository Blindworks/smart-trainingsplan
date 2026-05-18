package com.trainingsplan.dto;

import com.trainingsplan.entity.UserNotification;

import java.time.LocalDateTime;

public class NotificationDto {
    public Long id;
    public String type;
    public String title;
    public String message;
    public String linkPath;
    public Long referenceId;
    public boolean read;
    public LocalDateTime readAt;
    public LocalDateTime createdAt;

    public static NotificationDto fromEntity(UserNotification n) {
        NotificationDto dto = new NotificationDto();
        dto.id = n.getId();
        dto.type = n.getType() != null ? n.getType().name() : null;
        dto.title = n.getTitle();
        dto.message = n.getMessage();
        dto.linkPath = n.getLinkPath();
        dto.referenceId = n.getReferenceId();
        dto.read = n.getReadAt() != null;
        dto.readAt = n.getReadAt();
        dto.createdAt = n.getCreatedAt();
        return dto;
    }
}
