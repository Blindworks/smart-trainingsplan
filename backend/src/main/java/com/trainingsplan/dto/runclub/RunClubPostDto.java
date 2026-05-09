package com.trainingsplan.dto.runclub;

import java.time.LocalDateTime;

public class RunClubPostDto {

    private Long id;
    private Long clubId;
    private Long authorId;
    private String authorUsername;
    private String authorProfileImageFilename;
    private String content;
    private String imageFilename;
    private Long linkedActivityId;
    private Long linkedCommunityRouteId;
    private Long linkedGroupEventId;
    private int likeCount;
    private int commentCount;
    private boolean likedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    public RunClubPostDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClubId() { return clubId; }
    public void setClubId(Long clubId) { this.clubId = clubId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorProfileImageFilename() { return authorProfileImageFilename; }
    public void setAuthorProfileImageFilename(String authorProfileImageFilename) { this.authorProfileImageFilename = authorProfileImageFilename; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageFilename() { return imageFilename; }
    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }

    public Long getLinkedActivityId() { return linkedActivityId; }
    public void setLinkedActivityId(Long linkedActivityId) { this.linkedActivityId = linkedActivityId; }

    public Long getLinkedCommunityRouteId() { return linkedCommunityRouteId; }
    public void setLinkedCommunityRouteId(Long linkedCommunityRouteId) { this.linkedCommunityRouteId = linkedCommunityRouteId; }

    public Long getLinkedGroupEventId() { return linkedGroupEventId; }
    public void setLinkedGroupEventId(Long linkedGroupEventId) { this.linkedGroupEventId = linkedGroupEventId; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
