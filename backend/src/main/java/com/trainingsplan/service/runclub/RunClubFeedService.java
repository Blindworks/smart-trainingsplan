package com.trainingsplan.service.runclub;

import com.trainingsplan.dto.runclub.CreatePostRequest;
import com.trainingsplan.dto.runclub.RunClubCommentDto;
import com.trainingsplan.dto.runclub.RunClubPostDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.RunClubPostCommentRepository;
import com.trainingsplan.repository.RunClubPostLikeRepository;
import com.trainingsplan.repository.RunClubPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RunClubFeedService {

    private static final Logger log = LoggerFactory.getLogger(RunClubFeedService.class);
    private static final int POST_CONTENT_MAX_LENGTH = 5000;

    private final RunClubPostRepository postRepository;
    private final RunClubPostLikeRepository likeRepository;
    private final RunClubPostCommentRepository commentRepository;
    private final RunClubService runClubService;

    public RunClubFeedService(RunClubPostRepository postRepository,
                               RunClubPostLikeRepository likeRepository,
                               RunClubPostCommentRepository commentRepository,
                               RunClubService runClubService) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.runClubService = runClubService;
    }

    @Transactional(readOnly = true)
    public Page<RunClubPostDto> listPosts(User currentUser, Long clubId, int page) {
        RunClub club = runClubService.requireClub(clubId);
        requireActiveMember(club, currentUser);

        return postRepository.findActiveByClubId(clubId, PageRequest.of(page, 20))
                .map(p -> toPostDto(p, currentUser));
    }

    public RunClubPostDto createPost(User author, Long clubId, CreatePostRequest request) {
        RunClub club = runClubService.requireClub(clubId);
        requireActiveMember(club, author);

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Post content cannot be empty");
        }
        if (request.getContent().length() > POST_CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("Post content exceeds maximum length");
        }
        validateLinkedEntities(request);

        RunClubPost post = new RunClubPost();
        post.setClub(club);
        post.setAuthor(author);
        post.setContent(request.getContent());
        post.setLinkedActivityId(request.getLinkedActivityId());
        post.setLinkedCommunityRouteId(request.getLinkedCommunityRouteId());
        post.setLinkedGroupEventId(request.getLinkedGroupEventId());
        post.setCreatedAt(LocalDateTime.now());

        return toPostDto(postRepository.save(post), author);
    }

    public void deletePost(User requestingUser, Long postId) {
        RunClubPost post = requirePost(postId);
        boolean isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(requestingUser.getId());
        boolean isClubAdmin = runClubService.isClubAdmin(post.getClub(), requestingUser);
        boolean isPacrAdmin = requestingUser.getRole() == UserRole.ADMIN;

        if (!isAuthor && !isClubAdmin && !isPacrAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed to delete this post");
        }
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
        log.info("Post {} soft-deleted by user {}", postId, requestingUser.getId());
    }

    public RunClubPostDto likePost(User user, Long postId) {
        RunClubPost post = requireActivePost(postId);
        requireActiveMember(post.getClub(), user);

        if (!likeRepository.existsByPostIdAndUserId(postId, user.getId())) {
            likeRepository.save(new RunClubPostLike(post, user));
        }
        return toPostDto(post, user);
    }

    public RunClubPostDto unlikePost(User user, Long postId) {
        RunClubPost post = requireActivePost(postId);
        likeRepository.findByPostAndUser(post, user)
                .ifPresent(likeRepository::delete);
        return toPostDto(post, user);
    }

    @Transactional(readOnly = true)
    public List<RunClubCommentDto> listComments(User currentUser, Long postId) {
        RunClubPost post = requireActivePost(postId);
        requireActiveMember(post.getClub(), currentUser);

        return commentRepository.findActiveByPostId(postId).stream()
                .map(this::toCommentDto)
                .toList();
    }

    public RunClubCommentDto addComment(User author, Long postId, String content) {
        RunClubPost post = requireActivePost(postId);
        requireActiveMember(post.getClub(), author);

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Comment exceeds maximum length");
        }

        RunClubPostComment comment = new RunClubPostComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());

        return toCommentDto(commentRepository.save(comment));
    }

    public void deleteComment(User requestingUser, Long commentId) {
        RunClubPostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        boolean isAuthor = comment.getAuthor() != null
                && comment.getAuthor().getId().equals(requestingUser.getId());
        boolean isClubAdmin = runClubService.isClubAdmin(comment.getPost().getClub(), requestingUser);
        boolean isPacrAdmin = requestingUser.getRole() == UserRole.ADMIN;

        if (!isAuthor && !isClubAdmin && !isPacrAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed to delete this comment");
        }
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    // ---- DTO mapping ----

    RunClubPostDto toPostDto(RunClubPost post, User currentUser) {
        RunClubPostDto dto = new RunClubPostDto();
        dto.setId(post.getId());
        dto.setClubId(post.getClub().getId());
        if (post.getAuthor() != null) {
            dto.setAuthorId(post.getAuthor().getId());
            dto.setAuthorUsername(post.getAuthor().getUsername());
            dto.setAuthorProfileImageFilename(post.getAuthor().getProfileImageFilename());
        }
        dto.setContent(post.isDeleted() ? null : post.getContent());
        dto.setImageFilename(post.isDeleted() ? null : post.getImageFilename());
        dto.setLinkedActivityId(post.getLinkedActivityId());
        dto.setLinkedCommunityRouteId(post.getLinkedCommunityRouteId());
        dto.setLinkedGroupEventId(post.getLinkedGroupEventId());
        dto.setLikeCount((int) likeRepository.countByPostId(post.getId()));
        dto.setCommentCount((int) commentRepository.countByPostIdAndDeletedAtIsNull(post.getId()));
        if (currentUser != null) {
            dto.setLikedByCurrentUser(likeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId()));
        }
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setDeleted(post.isDeleted());
        return dto;
    }

    private RunClubCommentDto toCommentDto(RunClubPostComment comment) {
        RunClubCommentDto dto = new RunClubCommentDto();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPost().getId());
        if (comment.getAuthor() != null) {
            dto.setAuthorId(comment.getAuthor().getId());
            dto.setAuthorUsername(comment.getAuthor().getUsername());
            dto.setAuthorProfileImageFilename(comment.getAuthor().getProfileImageFilename());
        }
        dto.setContent(comment.isDeleted() ? null : comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setDeleted(comment.isDeleted());
        return dto;
    }

    // ---- Helpers ----

    private RunClubPost requirePost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
    }

    private RunClubPost requireActivePost(Long postId) {
        RunClubPost post = requirePost(postId);
        if (post.isDeleted()) throw new IllegalArgumentException("Post has been deleted");
        return post;
    }

    private void requireActiveMember(RunClub club, User user) {
        if (!runClubService.isActiveMember(club, user)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only active club members can access the feed");
        }
    }

    private void validateLinkedEntities(CreatePostRequest request) {
        int linkedCount = 0;
        if (request.getLinkedActivityId() != null) linkedCount++;
        if (request.getLinkedCommunityRouteId() != null) linkedCount++;
        if (request.getLinkedGroupEventId() != null) linkedCount++;
        if (linkedCount > 1) {
            throw new IllegalArgumentException("A post can only link to one entity at a time");
        }
    }
}
