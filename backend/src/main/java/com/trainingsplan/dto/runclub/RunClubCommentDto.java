package com.trainingsplan.dto.runclub;

import java.time.LocalDateTime;

public class RunClubCommentDto {

    private Long id;
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private String authorProfileImageFilename;
    private String content;
    private LocalDateTime createdAt;
    private boolean deleted;

    public RunClubCommentDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorProfileImageFilename() { return authorProfileImageFilename; }
    public void setAuthorProfileImageFilename(String authorProfileImageFilename) { this.authorProfileImageFilename = authorProfileImageFilename; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
