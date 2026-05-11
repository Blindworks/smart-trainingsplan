package com.trainingsplan.dto.runclub;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RunClubPostDto {

    private Long id;
    private Long clubId;
    /** Set in feeds that aggregate posts across multiple clubs (e.g. News Hub). */
    private String clubName;
    /** Set in feeds that aggregate posts across multiple clubs (e.g. News Hub). */
    private String clubSlug;
    /** Set in feeds that aggregate posts across multiple clubs (e.g. News Hub). */
    private String clubLogoFilename;
    private Long authorId;
    @JsonProperty("authorName")
    private String authorUsername;
    @JsonProperty("authorProfileImageUrl")
    private String authorProfileImageFilename;
    private String content;
    private String imageFilename;
    private List<Long> imageIds = new ArrayList<>();
    private Long linkedActivityId;
    private Long linkedCommunityRouteId;
    private Long linkedGroupEventId;
    private int likeCount;
    private int commentCount;
    @JsonProperty("likedByMe")
    private boolean likedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    public RunClubPostDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClubId() { return clubId; }
    public void setClubId(Long clubId) { this.clubId = clubId; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getClubSlug() { return clubSlug; }
    public void setClubSlug(String clubSlug) { this.clubSlug = clubSlug; }

    public String getClubLogoFilename() { return clubLogoFilename; }
    public void setClubLogoFilename(String clubLogoFilename) { this.clubLogoFilename = clubLogoFilename; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    @JsonProperty("authorName")
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    @JsonProperty("authorProfileImageUrl")
    public String getAuthorProfileImageFilename() { return authorProfileImageFilename; }
    public void setAuthorProfileImageFilename(String authorProfileImageFilename) { this.authorProfileImageFilename = authorProfileImageFilename; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageFilename() { return imageFilename; }
    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }

    public List<Long> getImageIds() { return imageIds; }
    public void setImageIds(List<Long> imageIds) { this.imageIds = imageIds; }

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

    @JsonProperty("likedByMe")
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
