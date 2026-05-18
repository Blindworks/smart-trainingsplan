package com.trainingsplan.controller;

import com.trainingsplan.dto.BuddyRunCreateRequest;
import com.trainingsplan.dto.BuddyRunDto;
import com.trainingsplan.dto.BuddyRunParticipantDto;
import com.trainingsplan.dto.BuddySuggestionDto;
import com.trainingsplan.entity.BuddyRun;
import com.trainingsplan.entity.BuddyRunParticipant;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserBuddyPreferences;
import com.trainingsplan.repository.UserBuddyPreferencesRepository;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.BuddyMatchService;
import com.trainingsplan.service.BuddyRunService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/buddy-runs")
public class BuddyRunController {

    private final BuddyRunService buddyRunService;
    private final BuddyMatchService buddyMatchService;
    private final UserRepository userRepository;
    private final UserBuddyPreferencesRepository preferencesRepository;
    private final SecurityUtils securityUtils;

    public BuddyRunController(BuddyRunService buddyRunService,
                              BuddyMatchService buddyMatchService,
                              UserRepository userRepository,
                              UserBuddyPreferencesRepository preferencesRepository,
                              SecurityUtils securityUtils) {
        this.buddyRunService = buddyRunService;
        this.buddyMatchService = buddyMatchService;
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/matches")
    public ResponseEntity<?> matches() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserBuddyPreferences prefs = preferencesRepository.findByUser(user).orElse(null);
        if (prefs == null || !prefs.isBuddyDiscoverable()) {
            return ResponseEntity.ok(Map.of(
                    "optedIn", false,
                    "matches", List.of()
            ));
        }
        List<BuddySuggestionDto> matches = buddyMatchService.suggestBuddiesForUser(user, prefs);
        return ResponseEntity.ok(Map.of(
                "optedIn", true,
                "matches", matches
        ));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BuddyRunCreateRequest req) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            BuddyRun br = buddyRunService.createBuddyRun(user, req);
            return ResponseEntity.status(HttpStatus.CREATED).body(BuddyRunDto.fromEntity(br));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/open")
    public ResponseEntity<?> openRuns(@RequestParam(value = "lat", required = false) Double lat,
                                       @RequestParam(value = "lon", required = false) Double lon,
                                       @RequestParam(value = "radiusKm", required = false) Double radiusKm) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<BuddyRunDto> list = buddyRunService.findOpenRunsForUser(user, lat, lon, radiusKm).stream()
                .map(BuddyRunDto::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> upcoming() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<BuddyRunDto> list = buddyRunService.findUpcomingForUser(user).stream()
                .map(BuddyRunDto::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<BuddyRunDto> list = buddyRunService.findCreatedByUser(user).stream()
                .map(BuddyRunDto::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            BuddyRun br = buddyRunService.getById(id);
            if (!buddyRunService.canUserSee(br, user)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            return ResponseEntity.ok(BuddyRunDto.fromEntity(br));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> join(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            BuddyRunParticipant p = buddyRunService.joinBuddyRun(id, user);
            return ResponseEntity.ok(BuddyRunParticipantDto.fromEntity(p));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            buddyRunService.withdraw(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<?> invite(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Object idObj = body.get("userId");
        if (idObj == null) return ResponseEntity.badRequest().body("userId required");
        Long targetId;
        try { targetId = Long.valueOf(idObj.toString()); }
        catch (NumberFormatException e) { return ResponseEntity.badRequest().body("invalid userId"); }
        User target = userRepository.findById(targetId).orElse(null);
        if (target == null) return ResponseEntity.notFound().build();
        try {
            BuddyRunParticipant p = buddyRunService.inviteUser(id, user, targetId, target);
            return ResponseEntity.status(HttpStatus.CREATED).body(BuddyRunParticipantDto.fromEntity(p));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Object acceptObj = body.get("accept");
        boolean accept = acceptObj != null && Boolean.parseBoolean(acceptObj.toString());
        try {
            BuddyRunParticipant p = buddyRunService.respondToInvite(id, user, accept);
            return ResponseEntity.ok(BuddyRunParticipantDto.fromEntity(p));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            buddyRunService.cancelBuddyRun(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/suggested-buddies")
    public ResponseEntity<?> suggestedBuddies(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            BuddyRun br = buddyRunService.getById(id);
            if (br.getCreator() == null || !br.getCreator().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            List<BuddySuggestionDto> suggestions = buddyMatchService.suggestBuddies(id);
            return ResponseEntity.ok(suggestions);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
