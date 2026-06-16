package com.trainingsplan.controller;

import com.trainingsplan.dto.SegmentChallengeDto;
import com.trainingsplan.dto.SegmentEffortResultDto;
import com.trainingsplan.dto.SegmentLeaderboardEntryDto;
import com.trainingsplan.dto.SegmentTrackDto;
import com.trainingsplan.entity.ActivityType;
import com.trainingsplan.service.SegmentChallengeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/challenges")
public class PublicSegmentChallengeController {

    private final SegmentChallengeService service;

    public PublicSegmentChallengeController(SegmentChallengeService service) {
        this.service = service;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getChallenge(@PathVariable String slug) {
        try {
            SegmentChallengeDto dto = service.getChallenge(slug);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{slug}/leaderboard")
    public ResponseEntity<?> getLeaderboard(@PathVariable String slug,
                                            @RequestParam(defaultValue = "RIDE") ActivityType type) {
        try {
            List<SegmentLeaderboardEntryDto> board = service.getLeaderboard(slug, type);
            return ResponseEntity.ok(board);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{slug}/efforts/{id}/track")
    public ResponseEntity<?> getTrack(@PathVariable String slug, @PathVariable Long id) {
        try {
            SegmentTrackDto dto = service.getEffortTrack(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{slug}/efforts")
    public ResponseEntity<?> submitEffort(@PathVariable String slug,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam("displayName") String displayName,
                                          @RequestParam("type") ActivityType type,
                                          HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "empty_file"));
        }
        try {
            SegmentEffortResultDto result = service.submitPublicEffort(
                    slug, type, displayName, file.getBytes(), file.getOriginalFilename(),
                    request.getRemoteAddr());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("reason", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("reason", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("reason", "io_error"));
        }
    }
}
