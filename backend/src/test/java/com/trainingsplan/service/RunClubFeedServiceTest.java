package com.trainingsplan.service;

import com.trainingsplan.dto.runclub.CreatePostRequest;
import com.trainingsplan.entity.*;
import com.trainingsplan.port.ImageStoragePort;
import com.trainingsplan.repository.*;
import com.trainingsplan.service.runclub.RunClubFeedService;
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
 * Unit tests for {@link RunClubFeedService}: visibility rules and linked-entity validation.
 */
class RunClubFeedServiceTest {

    private RunClubPostRepository postRepository;
    private RunClubPostLikeRepository likeRepository;
    private RunClubPostCommentRepository commentRepository;
    private RunClubMembershipRepository membershipRepository;
    private RunClubFeedService feedService;

    private User alice;
    private User bob;
    private RunClub club;

    @BeforeEach
    void setUp() {
        postRepository = mock(RunClubPostRepository.class);
        likeRepository = mock(RunClubPostLikeRepository.class);
        commentRepository = mock(RunClubPostCommentRepository.class);
        membershipRepository = mock(RunClubMembershipRepository.class);

        RunClubRepository runClubRepository = mock(RunClubRepository.class);
        GroupEventRepository groupEventRepository = mock(GroupEventRepository.class);
        ImageStoragePort imageStoragePort = mock(ImageStoragePort.class);
        RunClubService runClubService = new RunClubService(runClubRepository, membershipRepository,
                groupEventRepository, imageStoragePort);

        feedService = new RunClubFeedService(postRepository, likeRepository, commentRepository, runClubService);

        alice = buildUser(1L, "alice");
        bob = buildUser(2L, "bob");

        club = new RunClub();
        club.setId(10L);
        club.setName("Test Club");
        club.setSlug("test-club");
        club.setStatus(RunClubStatus.APPROVED);
        club.setCreatedAt(LocalDateTime.now());

        when(runClubRepository.findById(10L)).thenReturn(Optional.of(club));
    }

    // ---- visibility ----

    @Test
    void createPost_byNonMember_throwsAccessDenied() {
        when(membershipRepository.existsByClubAndUserAndStatus(club, bob, RunClubMembershipStatus.ACTIVE))
                .thenReturn(false);

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello!");

        assertThrows(AccessDeniedException.class, () -> feedService.createPost(bob, 10L, request));
    }

    @Test
    void createPost_byActiveMember_succeeds() {
        when(membershipRepository.existsByClubAndUserAndStatus(club, alice, RunClubMembershipStatus.ACTIVE))
                .thenReturn(true);
        when(postRepository.save(any())).thenAnswer(inv -> {
            RunClubPost p = inv.getArgument(0);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });
        when(likeRepository.countByPostId(any())).thenReturn(0L);
        when(commentRepository.countByPostIdAndDeletedAtIsNull(any())).thenReturn(0L);
        when(likeRepository.existsByPostIdAndUserId(any(), any())).thenReturn(false);

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello fellow runners!");

        var dto = feedService.createPost(alice, 10L, request);

        assertNotNull(dto);
        assertEquals("Hello fellow runners!", dto.getContent());
        assertFalse(dto.isDeleted());
    }

    @Test
    void createPost_emptyContent_throwsIllegalArgument() {
        when(membershipRepository.existsByClubAndUserAndStatus(club, alice, RunClubMembershipStatus.ACTIVE))
                .thenReturn(true);

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("  ");

        assertThrows(IllegalArgumentException.class, () -> feedService.createPost(alice, 10L, request));
    }

    // ---- linked entity validation ----

    @Test
    void createPost_twoLinkedEntities_throwsIllegalArgument() {
        when(membershipRepository.existsByClubAndUserAndStatus(club, alice, RunClubMembershipStatus.ACTIVE))
                .thenReturn(true);

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Check these out!");
        request.setLinkedActivityId(1L);
        request.setLinkedCommunityRouteId(2L);  // two links — invalid

        assertThrows(IllegalArgumentException.class, () -> feedService.createPost(alice, 10L, request));
    }

    @Test
    void createPost_oneLinkedEntity_isValid() {
        when(membershipRepository.existsByClubAndUserAndStatus(club, alice, RunClubMembershipStatus.ACTIVE))
                .thenReturn(true);
        when(postRepository.save(any())).thenAnswer(inv -> {
            RunClubPost p = inv.getArgument(0);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });
        when(likeRepository.countByPostId(any())).thenReturn(0L);
        when(commentRepository.countByPostIdAndDeletedAtIsNull(any())).thenReturn(0L);
        when(likeRepository.existsByPostIdAndUserId(any(), any())).thenReturn(false);

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Check this route!");
        request.setLinkedCommunityRouteId(5L);

        var dto = feedService.createPost(alice, 10L, request);

        assertEquals(5L, dto.getLinkedCommunityRouteId());
        assertNull(dto.getLinkedActivityId());
    }

    // ---- delete ----

    @Test
    void deletePost_byAuthor_softDeletes() {
        RunClubPost post = buildPost(alice);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(membershipRepository.findByClubAndUser(club, alice)).thenReturn(Optional.empty());

        feedService.deletePost(alice, 1L);

        assertNotNull(post.getDeletedAt());
        verify(postRepository).save(post);
    }

    @Test
    void deletePost_byUnrelatedUser_throwsAccessDenied() {
        RunClubPost post = buildPost(alice);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(membershipRepository.findByClubAndUser(club, bob)).thenReturn(Optional.empty());
        when(membershipRepository.existsByClubAndUserAndStatus(club, bob, RunClubMembershipStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> feedService.deletePost(bob, 1L));
    }

    // ---- helpers ----

    private User buildUser(Long id, String username) {
        User u = new User(username, username + "@example.com", LocalDateTime.now());
        u.setId(id);
        u.setRole(UserRole.USER);
        return u;
    }

    private RunClubPost buildPost(User author) {
        RunClubPost post = new RunClubPost();
        post.setId(1L);
        post.setClub(club);
        post.setAuthor(author);
        post.setContent("Test content");
        post.setCreatedAt(LocalDateTime.now());
        return post;
    }
}
