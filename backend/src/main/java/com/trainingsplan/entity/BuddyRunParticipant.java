package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "buddy_run_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_buddy_run_part_run_user", columnNames = {"buddy_run_id", "user_id"}))
public class BuddyRunParticipant {

    public enum Status { INVITED, JOINED, DECLINED, WITHDRAWN }
    public enum Role { CREATOR, PARTICIPANT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buddy_run_id", nullable = false)
    @JsonIgnore
    private BuddyRun buddyRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.JOINED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.PARTICIPANT;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public BuddyRunParticipant() {}

    @PrePersist
    void prePersist() {
        if (invitedAt == null) invitedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BuddyRun getBuddyRun() { return buddyRun; }
    public void setBuddyRun(BuddyRun buddyRun) { this.buddyRun = buddyRun; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
