package com.trainingsplan.controller;

import com.trainingsplan.dto.UserBuddyPreferencesDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserBuddyPreferences;
import com.trainingsplan.repository.UserBuddyPreferencesRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/buddy-preferences")
public class UserBuddyPreferencesController {

    private final UserBuddyPreferencesRepository repository;
    private final SecurityUtils securityUtils;

    public UserBuddyPreferencesController(UserBuddyPreferencesRepository repository, SecurityUtils securityUtils) {
        this.repository = repository;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Transactional
    public ResponseEntity<?> get() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserBuddyPreferences p = repository.findByUser(user).orElseGet(() -> createDefault(user));
        return ResponseEntity.ok(UserBuddyPreferencesDto.fromEntity(p));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<?> update(@RequestBody UserBuddyPreferencesDto dto) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserBuddyPreferences p = repository.findByUser(user).orElseGet(() -> createDefault(user));
        p.setBuddyDiscoverable(dto.buddyDiscoverable);
        if (dto.searchRadiusKm > 0 && dto.searchRadiusKm <= 200) p.setSearchRadiusKm(dto.searchRadiusKm);
        if (dto.paceTolerancePercent >= 0 && dto.paceTolerancePercent <= 100) p.setPaceTolerancePercent(dto.paceTolerancePercent);
        p.setAvailableWeekdays(dto.availableWeekdays);
        p.setAvailableTimeRanges(dto.availableTimeRanges);
        p.setAutoMatchEnabled(dto.autoMatchEnabled);
        // Plausible pace bounds: 2:00 - 12:00 min/km (120 - 720 sec/km); null clears it
        if (dto.targetPaceSecPerKm == null) {
            p.setTargetPaceSecPerKm(null);
        } else if (dto.targetPaceSecPerKm >= 120 && dto.targetPaceSecPerKm <= 720) {
            p.setTargetPaceSecPerKm(dto.targetPaceSecPerKm);
        }
        repository.save(p);
        return ResponseEntity.ok(UserBuddyPreferencesDto.fromEntity(p));
    }

    private UserBuddyPreferences createDefault(User user) {
        UserBuddyPreferences p = new UserBuddyPreferences();
        p.setUser(user);
        return repository.save(p);
    }
}
