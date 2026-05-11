package com.trainingsplan.service.runclub;

import com.trainingsplan.dto.runclub.CreatePostRequest;
import com.trainingsplan.dto.runclub.RunClubCommentDto;
import com.trainingsplan.dto.runclub.RunClubPostDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.port.ImageStoragePort;
import com.trainingsplan.repository.RunClubPostCommentRepository;
import com.trainingsplan.repository.RunClubPostLikeRepository;
import com.trainingsplan.repository.RunClubPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
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
    private final ImageStoragePort imageStorage;

    public RunClubFeedService(RunClubPostRepository postRepository,
                               RunClubPostLikeRepository likeRepository,
                               RunClubPostCommentRepository commentRepository,
                               RunClubService runClubService,
                               ImageStoragePort imageStorage) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.runClubService = runClubService;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public Page<RunClubPostDto> listPosts(User currentUser, Long clubId, int page) {
        RunClub club = runClubService.requireClub(clubId);
        requireActiveMember(club, currentUser);

        return postRepository.findActiveByClubId(clubId, PageRequest.of(page, 20))
                .map(p -> toPostDto(p, currentUser));
    }

    /**
     * Aggregated feed for the News Hub: all posts from clubs where the user holds an
     * ACTIVE membership. Each returned DTO has the {@code clubName/Slug/LogoFilename}
     * fields populated so the hub can render the club header without a second request.
     */
    @Transactional(readOnly = true)
    public Page<RunClubPostDto> listFeedForUser(User currentUser, int page, int size) {
        if (currentUser == null) {
            return Page.empty();
        }
        int safeSize = size <= 0 ? 20 : Math.min(size, 50);
        return postRepository.findFeedForUser(currentUser.getId(), PageRequest.of(page, safeSize))
                .map(p -> {
                    RunClubPostDto dto = toPostDto(p, currentUser);
                    RunClub club = p.getClub();
                    if (club != null) {
                        dto.setClubName(club.getName());
                        dto.setClubSlug(club.getSlug());
                        dto.setClubLogoFilename(club.getLogoFilename());
                    }
                    return dto;
                });
    }

    public RunClubPostDto createPost(User author, Long clubId, CreatePostRequest request) {
        return createPost(author, clubId, request, Collections.emptyList());
    }

    public RunClubPostDto createPost(User author, Long clubId, CreatePostRequest request, List<MultipartFile> images) {
        RunClub club = runClubService.requireClub(clubId);
        requireActiveMember(club, author);

        String content = request.getContent() == null ? "" : request.getContent().trim();
        boolean hasImages = images != null && images.stream().anyMatch(f -> f != null && !f.isEmpty());
        if (content.isEmpty() && !hasImages) {
            throw new IllegalArgumentException("Post must contain text or at least one image");
        }
        if (content.length() > POST_CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("Post content exceeds maximum length");
        }
        validateLinkedEntities(request);

        RunClubPost post = new RunClubPost();
        post.setClub(club);
        post.setAuthor(author);
        post.setContent(content);
        post.setLinkedActivityId(request.getLinkedActivityId());
        post.setLinkedCommunityRouteId(request.getLinkedCommunityRouteId());
        post.setLinkedGroupEventId(request.getLinkedGroupEventId());
        post.setCreatedAt(LocalDateTime.now());

        if (hasImages) {
            int order = 0;
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                String filename = imageStorage.store(file);
                RunClubPostImage img = new RunClubPostImage();
                img.setPost(post);
                img.setFilename(filename);
                img.setSortOrder(order++);
                post.getImages().add(img);
            }
        }

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
        if (!post.isDeleted() && post.getImages() != null) {
            dto.setImageIds(post.getImages().stream().map(RunClubPostImage::getId).toList());
        }
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

    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource loadPostImage(Long postId, Long imageId) {
        RunClubPost post = requirePost(postId);
        if (post.isDeleted()) {
            throw new IllegalArgumentException("Post has been deleted");
        }
        RunClubPostImage match = post.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        return imageStorage.load(match.getFilename());
    }

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
