package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "run_club_posts")
public class RunClubPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    @JsonIgnore
    private RunClub club;

    /** Nullable for DSGVO: author is set to null when user is deleted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @JsonIgnore
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_filename", length = 255)
    private String imageFilename;

    @Column(name = "linked_activity_id")
    private Long linkedActivityId;

    @Column(name = "linked_community_route_id")
    private Long linkedCommunityRouteId;

    @Column(name = "linked_group_event_id")
    private Long linkedGroupEventId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Soft-delete timestamp. Non-null means the post has been removed by admin moderation. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @JsonIgnore
    private List<RunClubPostImage> images = new ArrayList<>();

    public RunClubPost() {}

    public boolean isDeleted() { return deletedAt != null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RunClub getClub() { return club; }
    public void setClub(RunClub club) { this.club = club; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public List<RunClubPostImage> getImages() { return images; }
    public void setImages(List<RunClubPostImage> images) { this.images = images; }
}
