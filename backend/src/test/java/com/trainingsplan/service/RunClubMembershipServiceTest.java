package com.trainingsplan.service;

import com.trainingsplan.entity.*;
import com.trainingsplan.port.ImageStoragePort;
import com.trainingsplan.repository.GroupEventRepository;
import com.trainingsplan.repository.RunClubMembershipRepository;
import com.trainingsplan.repository.RunClubRepository;
import com.trainingsplan.service.runclub.RunClubMembershipService;
import com.trainingsplan.service.runclub.RunClubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RunClubMembershipService} join policies, last-admin protection, and kick.
 */
class RunClubMembershipServiceTest {

    private RunClubRepository runClubRepository;
    private RunClubMembershipRepository membershipRepository;
    private RunClubService runClubService;
    private RunClubMembershipService service;

    private User alice;
    private User bob;
    private RunClub openClub;
    private RunClub requestClub;

    @BeforeEach
    void setUp() {
        runClubRepository = mock(RunClubRepository.class);
        membershipRepository = mock(RunClubMembershipRepository.class);
        GroupEventRepository groupEventRepository = mock(GroupEventRepository.class);
        ImageStoragePort imageStoragePort = mock(ImageStoragePort.class);

        runClubService = new RunClubService(runClubRepository, membershipRepository,
                groupEventRepository, imageStoragePort);
        service = new RunClubMembershipService(membershipRepository, runClubService);

        alice = buildUser(1L, "alice");
        alice.setRole(UserRole.USER);

        bob = buildUser(2L, "bob");
        bob.setRole(UserRole.USER);

        openClub = buildApprovedClub(10L, RunClubJoinPolicy.OPEN);
        requestClub = buildApprovedClub(20L, RunClubJoinPolicy.REQUEST);
    }

    // ---- join OPEN ----

    @Test
    void join_openClub_immediatelyActive() {
        when(runClubRepository.findById(10L)).thenReturn(Optional.of(openClub));
        when(membershipRepository.findByClubAndUser(openClub, alice)).thenReturn(Optional.empty());
        when(membershipRepository.save(any())).thenAnswer(inv -> {
            RunClubMembership m = inv.getArgument(0);
            m.setClub(openClub);
            m.setUser(alice);
            return m;
        });
        when(membershipRepository.countByClubAndStatus(any(), eq(RunClubMembershipStatus.ACTIVE))).thenReturn(1L);

        var dto = service.join(alice, 10L);

        assertEquals(RunClubMembershipStatus.ACTIVE, dto.getStatus());
    }

    // ---- join REQUEST ----

    @Test
    void join_requestClub_statusIsPending() {
        when(runClubRepository.findById(20L)).thenReturn(Optional.of(requestClub));
        when(membershipRepository.findByClubAndUser(requestClub, alice)).thenReturn(Optional.empty());
        when(membershipRepository.save(any())).thenAnswer(inv -> {
            RunClubMembership m = inv.getArgument(0);
            m.setClub(requestClub);
            m.setUser(alice);
            return m;
        });
        when(membershipRepository.countByClubAndStatus(any(), eq(RunClubMembershipStatus.ACTIVE))).thenReturn(0L);

        var dto = service.join(alice, 20L);

        assertEquals(RunClubMembershipStatus.PENDING, dto.getStatus());
    }

    @Test
    void join_alreadyActiveMember_throwsConflict() {
        RunClubMembership existing = activeMembership(openClub, alice, RunClubMemberRole.MEMBER);
        when(runClubRepository.findById(10L)).thenReturn(Optional.of(openClub));
        when(membershipRepository.findByClubAndUser(openClub, alice)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.join(alice, 10L));
    }

    // ---- leave ----

    @Test
    void leave_lastAdmin_blocked() {
        RunClubMembership adminMembership = activeMembership(openClub, alice, RunClubMemberRole.ADMIN);
        when(runClubRepository.findById(10L)).thenReturn(Optional.of(openClub));
        when(membershipRepository.findByClubAndUser(openClub, alice)).thenReturn(Optional.of(adminMembership));
        when(membershipRepository.countByClubAndStatusAndRole(
                openClub, RunClubMembershipStatus.ACTIVE, RunClubMemberRole.ADMIN)).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.leave(alice, 10L));
        assertTrue(ex.getMessage().contains("last admin"));
    }

    @Test
    void leave_memberCanLeaveFreely() {
        RunClubMembership membership = activeMembership(openClub, bob, RunClubMemberRole.MEMBER);
        when(runClubRepository.findById(10L)).thenReturn(Optional.of(openClub));
        when(membershipRepository.findByClubAndUser(openClub, bob)).thenReturn(Optional.of(membership));

        service.leave(bob, 10L);

        assertEquals(RunClubMembershipStatus.LEFT, membership.getStatus());
    }

    // ---- kick ----

    @Test
    void kick_byAdmin_succeeds() {
        RunClubMembership adminMembership = activeMembership(openClub, alice, RunClubMemberRole.ADMIN);
        RunClubMembership memberMembership = activeMembership(openClub, bob, RunClubMemberRole.MEMBER);
        memberMembership.setId(99L);

        when(runClubRepository.findById(10L)).thenReturn(Optional.of(openClub));
        when(membershipRepository.findById(99L)).thenReturn(Optional.of(memberMembership));
        when(membershipRepository.findByClubAndUser(openClub, alice)).thenReturn(Optional.of(adminMembership));
        when(membershipRepository.existsByClubAndUserAndStatus(openClub, alice, RunClubMembershipStatus.ACTIVE))
                .thenReturn(true);

        service.kick(alice, 99L);

        assertEquals(RunClubMembershipStatus.LEFT, memberMembership.getStatus());
    }

    @Test
    void kick_byNonAdmin_throwsAccessDenied() {
        RunClubMembership memberMembership = activeMembership(openClub, bob, RunClubMemberRole.MEMBER);
        memberMembership.setId(99L);

        when(membershipRepository.findById(99L)).thenReturn(Optional.of(memberMembership));
        when(membershipRepository.findByClubAndUser(openClub, alice)).thenReturn(Optional.empty());
        when(membershipRepository.existsByClubAndUserAndStatus(openClub, alice, RunClubMembershipStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.kick(alice, 99L));
    }

    // ---- demote last admin protection ----

    @Test
    void demote_lastAdmin_blocked() {
        RunClubMembership adminMembership = activeMembership(openClub, alice, RunClubMemberRole.ADMIN);
        adminMembership.setId(5L);

        // alice is also the one making the request (but there's only 1 admin)
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(adminMembership));
        when(membershipRepository.findByClubAndUser(openClub, alice)).thenReturn(Optional.of(adminMembership));
        when(membershipRepository.existsByClubAndUserAndStatus(openClub, alice, RunClubMembershipStatus.ACTIVE))
                .thenReturn(true);
        when(membershipRepository.countByClubAndStatusAndRole(
                openClub, RunClubMembershipStatus.ACTIVE, RunClubMemberRole.ADMIN)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.demote(alice, 5L));
    }

    // ---- helpers ----

    private User buildUser(Long id, String username) {
        User u = new User(username, username + "@example.com", LocalDateTime.now());
        u.setId(id);
        return u;
    }

    private RunClub buildApprovedClub(Long id, RunClubJoinPolicy policy) {
        RunClub c = new RunClub();
        c.setId(id);
        c.setName("Club " + id);
        c.setSlug("club-" + id);
        c.setStatus(RunClubStatus.APPROVED);
        c.setJoinPolicy(policy);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    private RunClubMembership activeMembership(RunClub club, User user, RunClubMemberRole role) {
        RunClubMembership m = new RunClubMembership();
        m.setClub(club);
        m.setUser(user);
        m.setRole(role);
        m.setStatus(RunClubMembershipStatus.ACTIVE);
        m.setJoinedAt(LocalDateTime.now());
        return m;
    }
}
