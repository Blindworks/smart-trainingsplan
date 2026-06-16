package com.trainingsplan.controller;

import com.trainingsplan.entity.ActivityType;
import com.trainingsplan.entity.EffortCategory;
import com.trainingsplan.entity.EffortStatus;
import com.trainingsplan.entity.SegmentChallenge;
import com.trainingsplan.repository.SegmentChallengeRepository;
import com.trainingsplan.service.SegmentChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/challenges")
public class AdminSegmentChallengeController {

    private final SegmentChallengeRepository challengeRepository;
    private final SegmentChallengeService service;

    public AdminSegmentChallengeController(SegmentChallengeRepository challengeRepository,
                                           SegmentChallengeService service) {
        this.challengeRepository = challengeRepository;
        this.service = service;
    }

    /** Create or update a challenge by slug (idempotent on slug). */
    @PostMapping
    public ResponseEntity<?> upsert(@RequestBody SegmentChallenge body) {
        SegmentChallenge c = challengeRepository.findBySlug(body.getSlug()).orElseGet(SegmentChallenge::new);
        c.setSlug(body.getSlug());
        c.setName(body.getName());
        c.setSubtitle(body.getSubtitle());
        c.setEventDate(body.getEventDate());
        c.setStartLat(body.getStartLat());
        c.setStartLng(body.getStartLng());
        c.setEndLat(body.getEndLat());
        c.setEndLng(body.getEndLng());
        c.setDistanceM(body.getDistanceM());
        c.setElevationGainM(body.getElevationGainM());
        c.setAvgGradePct(body.getAvgGradePct());
        c.setMaxGradePct(body.getMaxGradePct());
        c.setPolylineJson(body.getPolylineJson());
        c.setTerrainAssetRef(body.getTerrainAssetRef());
        c.setBoundingBoxJson(body.getBoundingBoxJson());
        c.setActive(body.isActive());
        if (c.getId() == null) {
            c.setCreatedAt(LocalDateTime.now());
        }
        c.setUpdatedAt(LocalDateTime.now());
        challengeRepository.save(c);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", c.getId(), "slug", c.getSlug()));
    }

    /** Upload a named reference effort ("one of the greats"). */
    @PostMapping("/{slug}/reference-efforts")
    public ResponseEntity<?> addReferenceEffort(@PathVariable String slug,
                                                @RequestParam("file") MultipartFile file,
                                                @RequestParam("displayName") String displayName,
                                                @RequestParam("type") ActivityType type,
                                                @RequestParam("category") EffortCategory category,
                                                @RequestParam(value = "knownTimeSeconds", required = false) Integer knownTimeSeconds) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "empty_file"));
        }
        try {
            Long id = service.addReferenceEffort(slug, type, displayName, category,
                    file.getBytes(), file.getOriginalFilename(), knownTimeSeconds);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("effortId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("reason", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("reason", "io_error"));
        }
    }

    /** Hide/reject (moderate) an effort. */
    @PutMapping("/efforts/{id}/status")
    public ResponseEntity<?> setStatus(@PathVariable Long id, @RequestParam EffortStatus status) {
        try {
            service.setEffortStatus(id, status);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
