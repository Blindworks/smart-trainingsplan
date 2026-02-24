package com.trainingsplan.controller;

import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.CompletedTrainingService;
import com.trainingsplan.service.StravaService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/completed-trainings")
@CrossOrigin(origins = "http://localhost:4200")
public class CompletedTrainingController {

    @Autowired
    private CompletedTrainingService completedTrainingService;

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    @Autowired
    private StravaService stravaService;

    @Autowired
    private CompletedTrainingRepository completedTrainingRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFitFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate trainingDate,
            @RequestParam(value = "trainingId", required = false) Long trainingId) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Datei ist leer");
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".fit")) {
                return ResponseEntity.badRequest().body("Nur .FIT-Dateien sind erlaubt");
            }
            
            CompletedTraining training = completedTrainingService.uploadAndParseFitFile(file, trainingDate, trainingId);
            return ResponseEntity.ok(training);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Verarbeiten der FIT-Datei: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unerwarteter Fehler: " + e.getMessage());
        }
    }

    @GetMapping("/by-date")
    public ResponseEntity<List<CompletedTraining>> getCompletedTrainingsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<CompletedTraining> trainings = completedTrainingService.getCompletedTrainingsByDate(date);
        return ResponseEntity.ok(trainings);
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<CompletedTraining>> getCompletedTrainingsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<CompletedTraining> trainings = completedTrainingService.getCompletedTrainingsBetweenDates(startDate, endDate);
        return ResponseEntity.ok(trainings);
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<ActivityMetrics> getActivityMetrics(@PathVariable Long id) {
        return activityMetricsRepository.findByCompletedTrainingId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/compute-strava-metrics")
    public ResponseEntity<?> computeStravaMetrics(@PathVariable Long id) {
        try {
            ActivityMetrics metrics = stravaService.computeMetricsForCompletedTraining(id);
            if (metrics == null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("Streams konnten nicht abgerufen werden (kein HR-Sensor?)");
            }
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Berechnen der Metriken: " + e.getMessage());
        }
    }

    /**
     * Returns eligible aerobic decoupling data for the authenticated user.
     * When {@code startDate}/{@code endDate} are provided the result is filtered to that window
     * and sorted chronologically (ASC); otherwise the last {@code limit} activities are returned.
     *
     * <p>As a lazy migration, orphaned Strava activities (user_id = NULL) are first claimed.
     */
    @Transactional
    @GetMapping("/decoupling-history")
    public ResponseEntity<List<Map<String, Object>>> getDecouplingHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "20") int limit) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        completedTrainingRepository.claimOrphanedStravaActivities(user);
        Long userId = user.getId();

        List<ActivityMetrics> entries;
        if (startDate != null && endDate != null) {
            entries = activityMetricsRepository.findEligibleDecouplingByUserIdAndDateRange(
                    userId, startDate, endDate);
        } else {
            entries = activityMetricsRepository.findEligibleDecouplingByUserId(
                    userId, PageRequest.of(0, Math.min(limit, 50)));
            Collections.reverse(entries); // DESC → ASC for chart
        }

        List<Map<String, Object>> result = new ArrayList<>(entries.size());
        for (ActivityMetrics am : entries) {
            CompletedTraining ct = am.getCompletedTraining();
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("date", ct.getTrainingDate().toString());
            dto.put("activityName", ct.getActivityName() != null ? ct.getActivityName() : ct.getSport());
            dto.put("sport", ct.getSport());
            dto.put("decouplingPct", am.getDecouplingPct());
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompletedTraining> getCompletedTrainingById(@PathVariable Long id) {
        // Diese Methode kann später implementiert werden
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompletedTraining(@PathVariable Long id) {
        // Diese Methode kann später implementiert werden
        return ResponseEntity.notFound().build();
    }
}