package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "run_club_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "user_id"}))
public class RunClubMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    @JsonIgnore
    private RunClub club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunClubMemberRole role = RunClubMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunClubMembershipStatus status = RunClubMembershipStatus.ACTIVE;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    public RunClubMembership() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RunClub getClub() { return club; }
    public void setClub(RunClub club) { this.club = club; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public RunClubMemberRole getRole() { return role; }
    public void setRole(RunClubMemberRole role) { this.role = role; }

    public RunClubMembershipStatus getStatus() { return status; }
    public void setStatus(RunClubMembershipStatus status) { this.status = status; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
