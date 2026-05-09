package com.trainingsplan.service;

import com.trainingsplan.dto.runclub.CreateRunClubRequest;
import com.trainingsplan.dto.runclub.RunClubDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.port.ImageStoragePort;
import com.trainingsplan.repository.GroupEventRepository;
import com.trainingsplan.repository.RunClubMembershipRepository;
import com.trainingsplan.repository.RunClubRepository;
import com.trainingsplan.service.runclub.RunClubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RunClubService} core business logic.
 * No Spring context — all dependencies are mocked.
 */
class RunClubServiceTest {

    private RunClubRepository runClubRepository;
    private RunClubMembershipRepository membershipRepository;
    private GroupEventRepository groupEventRepository;
    private ImageStoragePort imageStoragePort;
    private RunClubService service;

    private User founder;
    private User adminUser;

    @BeforeEach
    void setUp() {
        runClubRepository = mock(RunClubRepository.class);
        membershipRepository = mock(RunClubMembershipRepository.class);
        groupEventRepository = mock(GroupEventRepository.class);
        imageStoragePort = mock(ImageStoragePort.class);

        service = new RunClubService(runClubRepository, membershipRepository,
                groupEventRepository, imageStoragePort);

        founder = new User("alice", "alice@example.com", java.time.LocalDateTime.now());
        founder.setId(1L);

        adminUser = new User("admin", "admin@example.com", java.time.LocalDateTime.now());
        adminUser.setId(99L);
        adminUser.setRole(UserRole.ADMIN);
    }

    // ---- create ----

    @Test
    void create_setsStatusPendingAndSavesFounderMembership() {
        CreateRunClubRequest request = new CreateRunClubRequest();
        request.setName("Runners United");
        request.setJoinPolicy(RunClubJoinPolicy.OPEN);

        when(runClubRepository.existsByName("Runners United")).thenReturn(false);
        when(runClubRepository.existsBySlug(anyString())).thenReturn(false);

        RunClub savedClub = new RunClub();
        savedClub.setId(10L);
        savedClub.setName("Runners United");
        savedClub.setSlug("runners-united");
        savedClub.setStatus(RunClubStatus.PENDING);
        savedClub.setCreatedBy(founder);
        savedClub.setCreatedAt(java.time.LocalDateTime.now());
        when(runClubRepository.save(any(RunClub.class))).thenReturn(savedClub);
        when(membershipRepository.countByClubAndStatus(any(), eq(RunClubMembershipStatus.ACTIVE))).thenReturn(0L);

        RunClubDto dto = service.create(founder, request);

        assertEquals(RunClubStatus.PENDING, dto.getStatus());
        assertEquals("Runners United", dto.getName());

        // Verify a PENDING ADMIN membership was created
        ArgumentCaptor<RunClubMembership> captor = ArgumentCaptor.forClass(RunClubMembership.class);
        verify(membershipRepository).save(captor.capture());
        RunClubMembership membership = captor.getValue();
        assertEquals(RunClubMemberRole.ADMIN, membership.getRole());
        assertEquals(RunClubMembershipStatus.PENDING, membership.getStatus());
    }

    @Test
    void create_failsWhenNameAlreadyExists() {
        CreateRunClubRequest request = new CreateRunClubRequest();
        request.setName("Existing Club");
        when(runClubRepository.existsByName("Existing Club")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(founder, request));
        verify(runClubRepository, never()).save(any());
    }

    @Test
    void create_failsWithBlankName() {
        CreateRunClubRequest request = new CreateRunClubRequest();
        request.setName("  ");

        assertThrows(IllegalArgumentException.class, () -> service.create(founder, request));
    }

    // ---- approval flow ----

    @Test
    void approve_setsStatusApprovedAndActivatesFounderMembership() {
        RunClub club = buildPendingClub();
        when(runClubRepository.findById(10L)).thenReturn(Optional.of(club));
        when(runClubRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RunClubMembership pendingMembership = new RunClubMembership();
        pendingMembership.setRole(RunClubMemberRole.ADMIN);
        pendingMembership.setStatus(RunClubMembershipStatus.PENDING);
        when(membershipRepository.findByClubAndStatus(club, RunClubMembershipStatus.PENDING))
                .thenReturn(java.util.List.of(pendingMembership));
        when(membershipRepository.countByClubAndStatus(any(), eq(RunClubMembershipStatus.ACTIVE))).thenReturn(1L);

        RunClubDto dto = service.approve(adminUser, 10L);

        assertEquals(RunClubStatus.APPROVED, dto.getStatus());
        assertEquals(RunClubMembershipStatus.ACTIVE, pendingMembership.getStatus());
        assertNotNull(pendingMembership.getJoinedAt());
    }

    @Test
    void approve_failsWhenAlreadyApproved() {
        RunClub club = buildPendingClub();
        club.setStatus(RunClubStatus.APPROVED);
        when(runClubRepository.findById(10L)).thenReturn(Optional.of(club));

        assertThrows(IllegalStateException.class, () -> service.approve(adminUser, 10L));
    }

    @Test
    void reject_setsStatusRejectedWithReason() {
        RunClub club = buildPendingClub();
        when(runClubRepository.findById(10L)).thenReturn(Optional.of(club));
        when(runClubRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.countByClubAndStatus(any(), eq(RunClubMembershipStatus.ACTIVE))).thenReturn(0L);

        RunClubDto dto = service.reject(adminUser, 10L, "Spam");

        assertEquals(RunClubStatus.REJECTED, dto.getStatus());
    }

    // ---- slug generation ----

    @Test
    void generateUniqueSlug_basicName() {
        when(runClubRepository.existsBySlug("runners-united")).thenReturn(false);
        assertEquals("runners-united", service.generateUniqueSlug("Runners United", null));
    }

    @Test
    void generateUniqueSlug_handlesUmlauts() {
        when(runClubRepository.existsBySlug("laeufer-ueberall")).thenReturn(false);
        String slug = service.generateUniqueSlug("Läufer Überall", null);
        assertEquals("laeufer-ueberall", slug);
    }

    @Test
    void generateUniqueSlug_addsNumericSuffixOnConflict() {
        when(runClubRepository.existsBySlug("running-club")).thenReturn(true);
        when(runClubRepository.existsBySlug("running-club-2")).thenReturn(false);

        String slug = service.generateUniqueSlug("Running Club", null);
        assertEquals("running-club-2", slug);
    }

    @Test
    void generateUniqueSlug_truncatesLongNames() {
        when(runClubRepository.existsBySlug(anyString())).thenReturn(false);
        String longName = "a".repeat(100);
        String slug = service.generateUniqueSlug(longName, null);
        assertTrue(slug.length() <= 60, "Slug must not exceed 60 chars");
    }

    // ---- helpers ----

    private RunClub buildPendingClub() {
        RunClub club = new RunClub();
        club.setId(10L);
        club.setName("Test Club");
        club.setSlug("test-club");
        club.setStatus(RunClubStatus.PENDING);
        club.setCreatedBy(founder);
        club.setCreatedAt(java.time.LocalDateTime.now());
        return club;
    }
}
