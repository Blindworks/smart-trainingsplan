package com.trainingsplan.controller;

import com.trainingsplan.dto.runclub.RunClubDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.runclub.RunClubService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/run-clubs")
@PreAuthorize("hasRole('ADMIN')")
public class RunClubAdminController {

    private final RunClubService runClubService;
    private final SecurityUtils securityUtils;

    public RunClubAdminController(RunClubService runClubService, SecurityUtils securityUtils) {
        this.runClubService = runClubService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<RunClubDto>> getAll(
            @RequestParam(required = false) String status) {
        User admin = securityUtils.getCurrentUser();
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if ("PENDING".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(runClubService.getPendingForAdmin(admin));
        }
        return ResponseEntity.ok(runClubService.getAllForAdmin(admin));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        User admin = securityUtils.getCurrentUser();
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(runClubService.approve(admin, id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestBody(required = false) Map<String, String> body) {
        User admin = securityUtils.getCurrentUser();
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String reason = body != null ? body.get("reason") : null;
        try {
            return ResponseEntity.ok(runClubService.reject(admin, id, reason));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verify(@PathVariable Long id) {
        User admin = securityUtils.getCurrentUser();
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(runClubService.verify(admin, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> hardDelete(@PathVariable Long id) {
        User admin = securityUtils.getCurrentUser();
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            runClubService.delete(admin, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
