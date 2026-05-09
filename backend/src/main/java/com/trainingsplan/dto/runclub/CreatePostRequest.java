package com.trainingsplan.dto.runclub;

public class CreatePostRequest {

    private String content;
    private Long linkedActivityId;
    private Long linkedCommunityRouteId;
    private Long linkedGroupEventId;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getLinkedActivityId() { return linkedActivityId; }
    public void setLinkedActivityId(Long linkedActivityId) { this.linkedActivityId = linkedActivityId; }

    public Long getLinkedCommunityRouteId() { return linkedCommunityRouteId; }
    public void setLinkedCommunityRouteId(Long linkedCommunityRouteId) { this.linkedCommunityRouteId = linkedCommunityRouteId; }

    public Long getLinkedGroupEventId() { return linkedGroupEventId; }
    public void setLinkedGroupEventId(Long linkedGroupEventId) { this.linkedGroupEventId = linkedGroupEventId; }
}
