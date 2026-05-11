package com.trainingsplan.controller;

import com.trainingsplan.dto.CreateGroupEventRequest;
import com.trainingsplan.dto.GroupEventDto;
import com.trainingsplan.dto.UpdateGroupEventRequest;
import com.trainingsplan.dto.runclub.*;
import com.trainingsplan.entity.GroupEventStatus;
import com.trainingsplan.entity.RunClub;
import com.trainingsplan.entity.RunClubMembershipStatus;
import com.trainingsplan.entity.User;
import org.springframework.security.access.AccessDeniedException;
import com.trainingsplan.repository.GroupEventRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.GroupEventService;
import com.trainingsplan.service.UserService;
import com.trainingsplan.service.runclub.RunClubFeedService;
import com.trainingsplan.service.runclub.RunClubMembershipService;
import com.trainingsplan.service.runclub.RunClubService;
import com.trainingsplan.service.runclub.RunClubStatsService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/run-clubs")
public class RunClubController {

    private final RunClubService runClubService;
    private final RunClubMembershipService membershipService;
    private final RunClubFeedService feedService;
    private final RunClubStatsService statsService;
    private final GroupEventRepository groupEventRepository;
    private final GroupEventService groupEventService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    public RunClubController(RunClubService runClubService,
                             RunClubMembershipService membershipService,
                             RunClubFeedService feedService,
                             RunClubStatsService statsService,
                             GroupEventRepository groupEventRepository,
                             GroupEventService groupEventService,
                             SecurityUtils securityUtils,
                             UserService userService) {
        this.runClubService = runClubService;
        this.membershipService = membershipService;
        this.feedService = feedService;
        this.statsService = statsService;
        this.groupEventRepository = groupEventRepository;
        this.groupEventService = groupEventService;
        this.securityUtils = securityUtils;
        this.userService = userService;
    }

    // ---- Discovery / Search ----

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "25") Double radiusKm,
            @RequestParam(required = false) String day,
            @RequestParam(required = false) String q) {

        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isRunClubsEnabled()) return featureDisabled();

        List<RunClubDto> result;
        if (q != null && !q.isBlank()) {
            result = runClubService.searchByName(q.trim(), user);
        } else if (city != null && !city.isBlank()) {
            result = runClubService.searchByCity(city.trim(), user);
        } else if (lat != null && lng != null) {
            result = runClubService.searchNearby(lat, lng, radiusKm, user);
        } else if (day != null) {
            try {
                result = runClubService.searchByMeetingDay(DayOfWeek.valueOf(day.toUpperCase()), user);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid day: " + day);
            }
        } else {
            result = runClubService.getAllApprovedForMap(user);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/map")
    public ResponseEntity<?> getForMap() {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(runClubService.getAllApprovedForMap(user));
    }

    @GetMapping("/me/clubs")
    public ResponseEntity<?> getMyClubs() {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(runClubService.getMyClubs(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(runClubService.getById(id, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(runClubService.getBySlug(slug, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- CRUD ----

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRunClubRequest request) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isRunClubsEnabled()) return featureDisabled();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(runClubService.create(user, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateRunClubRequest request) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(runClubService.update(user, id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            runClubService.delete(user, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    // ---- Image upload ----

    @PostMapping("/{id}/logo")
    public ResponseEntity<?> uploadLogo(@PathVariable Long id,
                                        @RequestParam("file") MultipartFile file) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(runClubService.uploadLogo(user, id, file));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cover")
    public ResponseEntity<?> uploadCover(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(runClubService.uploadCover(user, id, file));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<org.springframework.core.io.Resource> getLogo(@PathVariable Long id) {
        return serveClubImage(runClubService.loadLogoImage(id));
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<org.springframework.core.io.Resource> getCover(@PathVariable Long id) {
        return serveClubImage(runClubService.loadCoverImage(id));
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> getImage(@PathVariable String filename) {
        try {
            org.springframework.core.io.Resource resource = runClubService.loadImage(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<org.springframework.core.io.Resource> serveClubImage(com.trainingsplan.service.runclub.RunClubService.ClubImageData data) {
        if (data == null) return ResponseEntity.noContent().build();
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(data.contentType());
        } catch (IllegalArgumentException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofMinutes(10)))
                .body(data.resource());
    }

    // ---- Stats ----

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getStats(@PathVariable Long id) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(statsService.computeStats(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- Members ----

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getMembers(@PathVariable Long id,
                                        @RequestParam(defaultValue = "ACTIVE") String status) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RunClubMembershipStatus membershipStatus = RunClubMembershipStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(membershipService.listMembers(id, membershipStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + status);
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> join(@PathVariable Long id) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isRunClubsEnabled()) return featureDisabled();
        try {
            return ResponseEntity.ok(membershipService.join(user, id));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leave(@PathVariable Long id) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            membershipService.leave(user, id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- Membership management ----

    @PostMapping("/memberships/{membershipId}/approve")
    public ResponseEntity<?> approveJoinRequest(@PathVariable Long membershipId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(membershipService.approveJoinRequest(user, membershipId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/memberships/{membershipId}/reject")
    public ResponseEntity<?> rejectJoinRequest(@PathVariable Long membershipId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            membershipService.rejectJoinRequest(user, membershipId);
            return ResponseEntity.ok().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/memberships/{membershipId}/promote")
    public ResponseEntity<?> promote(@PathVariable Long membershipId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(membershipService.promote(user, membershipId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/memberships/{membershipId}/demote")
    public ResponseEntity<?> demote(@PathVariable Long membershipId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(membershipService.demote(user, membershipId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/memberships/{membershipId}/kick")
    public ResponseEntity<?> kick(@PathVariable Long membershipId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            membershipService.kick(user, membershipId);
            return ResponseEntity.ok().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ---- Feed (posts, likes, comments) ----

    @GetMapping("/{id}/posts")
    public ResponseEntity<?> listPosts(@PathVariable Long id,
                                       @RequestParam(defaultValue = "0") int page) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            Page<RunClubPostDto> posts = feedService.listPosts(user, id, page);
            return ResponseEntity.ok(posts);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/{id}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(@PathVariable Long id,
                                        @RequestParam("content") String content,
                                        @RequestParam(value = "linkedActivityId", required = false) Long linkedActivityId,
                                        @RequestParam(value = "linkedCommunityRouteId", required = false) Long linkedCommunityRouteId,
                                        @RequestParam(value = "linkedGroupEventId", required = false) Long linkedGroupEventId,
                                        @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        CreatePostRequest request = new CreatePostRequest();
        request.setContent(content);
        request.setLinkedActivityId(linkedActivityId);
        request.setLinkedCommunityRouteId(linkedCommunityRouteId);
        request.setLinkedGroupEventId(linkedGroupEventId);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(feedService.createPost(user, id, request, images == null ? List.of() : images));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/posts/{postId}/images/{imageId}")
    public ResponseEntity<org.springframework.core.io.Resource> getPostImage(@PathVariable Long postId,
                                                                              @PathVariable Long imageId) {
        try {
            org.springframework.core.io.Resource resource = feedService.loadPostImage(postId, imageId);
            return ResponseEntity.ok()
                    .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofHours(1)))
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            feedService.deletePost(user, postId);
            return ResponseEntity.ok().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(feedService.likePost(user, postId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<?> unlikePost(@PathVariable Long postId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(feedService.unlikePost(user, postId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> listComments(@PathVariable Long postId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(feedService.listComments(user, postId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId,
                                        @RequestBody Map<String, String> body) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String content = body.get("content");
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(feedService.addComment(user, postId, content));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            feedService.deleteComment(user, commentId);
            return ResponseEntity.ok().build();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- News Hub aggregated feed ----

    /**
     * Aggregated feed for the News Hub: returns posts from every Run Club where the
     * current user holds an ACTIVE membership, newest first. Each item is enriched
     * with the club's name/slug/logo so the hub can render the club header inline.
     */
    @GetMapping("/feed")
    public ResponseEntity<?> getNewsHubFeed(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Page<RunClubPostDto> posts = feedService.listFeedForUser(user, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Upcoming Run-Club events for the News Hub sidebar: events linked to a Run Club
     * where the current user holds an ACTIVE membership, recurring series expanded.
     */
    @GetMapping("/feed/upcoming-events")
    public ResponseEntity<List<GroupEventDto>> getNewsHubClubEvents() {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(groupEventService.getUpcomingClubEventsForMember(user));
    }

    // ---- Events ----

    @GetMapping("/{id}/events")
    public ResponseEntity<?> getClubEvents(@PathVariable Long id) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RunClub club = runClubService.requireClub(id);
            // Club admins also see DRAFT events so they can manage them from the tab.
            boolean isAdmin = runClubService.isClubAdmin(club, user);
            List<?> events = groupEventRepository.findAll().stream()
                    .filter(e -> e.getRunClub() != null && e.getRunClub().getId().equals(id))
                    .filter(e -> isAdmin || e.getStatus() == GroupEventStatus.PUBLISHED)
                    .toList();
            return ResponseEntity.ok(events);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/events")
    public ResponseEntity<?> createClubEvent(@PathVariable Long id,
                                             @RequestBody CreateGroupEventRequest request) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RunClub club = runClubService.requireClub(id);
            runClubService.requireClubAdmin(club, user);
            GroupEventDto dto = groupEventService.createClubEvent(user, club, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only club admins can create events");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/events/{eventId}")
    public ResponseEntity<?> updateClubEvent(@PathVariable Long id,
                                             @PathVariable Long eventId,
                                             @RequestBody UpdateGroupEventRequest request) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RunClub club = runClubService.requireClub(id);
            runClubService.requireClubAdmin(club, user);
            GroupEventDto dto = groupEventService.updateClubEvent(club, eventId, request);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only club admins can update events");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/events/{eventId}/publish")
    public ResponseEntity<?> publishClubEvent(@PathVariable Long id, @PathVariable Long eventId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RunClub club = runClubService.requireClub(id);
            runClubService.requireClubAdmin(club, user);
            GroupEventDto dto = groupEventService.publishClubEvent(club, eventId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/events/{eventId}/cancel")
    public ResponseEntity<?> cancelClubEvent(@PathVariable Long id, @PathVariable Long eventId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RunClub club = runClubService.requireClub(id);
            runClubService.requireClubAdmin(club, user);
            groupEventService.cancelClubEvent(club, eventId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/events/{eventId}")
    public ResponseEntity<?> deleteClubEvent(@PathVariable Long id, @PathVariable Long eventId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RunClub club = runClubService.requireClub(id);
            runClubService.requireClubAdmin(club, user);
            groupEventService.deleteClubEvent(club, eventId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/events/{eventId}")
    public ResponseEntity<?> getClubEventDetail(@PathVariable Long id, @PathVariable Long eventId) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            RunClub club = runClubService.requireClub(id);
            // Members can read drafts only if admin; published always readable
            GroupEventDto dto = groupEventService.getAllClubEvents(club).stream()
                    .filter(e -> e.id().equals(eventId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Event not found"));
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- Settings ----

    @PatchMapping("/me/enabled")
    public ResponseEntity<?> setRunClubsEnabled(@RequestBody Map<String, Boolean> body) {
        User user = requireAuth();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Boolean enabled = body.get("enabled");
        if (enabled == null) return ResponseEntity.badRequest().body("Missing 'enabled' field");
        user.setRunClubsEnabled(enabled);
        userService.save(user);
        return ResponseEntity.ok(Map.of("runClubsEnabled", enabled));
    }

    // ---- Helpers ----

    private User requireAuth() {
        return securityUtils.getCurrentUser();
    }

    private ResponseEntity<Map<String, String>> featureDisabled() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Run clubs feature is not enabled for your account"));
    }
}
