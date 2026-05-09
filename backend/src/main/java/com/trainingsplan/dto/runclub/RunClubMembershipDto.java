package com.trainingsplan.dto.runclub;

import com.trainingsplan.entity.RunClubMemberRole;
import com.trainingsplan.entity.RunClubMembershipStatus;

import java.time.LocalDateTime;

public class RunClubMembershipDto {

    private Long id;
    private Long clubId;
    private String clubName;
    private String clubSlug;
    private Long userId;
    private String username;
    private String userFirstName;
    private String userLastName;
    private String profileImageFilename;
    private boolean hasProfileImage;
    private boolean creator;
    private RunClubMemberRole role;
    private RunClubMembershipStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime requestedAt;

    public RunClubMembershipDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClubId() { return clubId; }
    public void setClubId(Long clubId) { this.clubId = clubId; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getClubSlug() { return clubSlug; }
    public void setClubSlug(String clubSlug) { this.clubSlug = clubSlug; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProfileImageFilename() { return profileImageFilename; }
    public void setProfileImageFilename(String profileImageFilename) { this.profileImageFilename = profileImageFilename; }

    public String getUserFirstName() { return userFirstName; }
    public void setUserFirstName(String userFirstName) { this.userFirstName = userFirstName; }

    public String getUserLastName() { return userLastName; }
    public void setUserLastName(String userLastName) { this.userLastName = userLastName; }

    public boolean isHasProfileImage() { return hasProfileImage; }
    public void setHasProfileImage(boolean hasProfileImage) { this.hasProfileImage = hasProfileImage; }

    public boolean isCreator() { return creator; }
    public void setCreator(boolean creator) { this.creator = creator; }

    public RunClubMemberRole getRole() { return role; }
    public void setRole(RunClubMemberRole role) { this.role = role; }

    public RunClubMembershipStatus getStatus() { return status; }
    public void setStatus(RunClubMembershipStatus status) { this.status = status; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
