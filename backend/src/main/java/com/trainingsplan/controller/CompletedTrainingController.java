package com.trainingsplan.controller;

import com.trainingsplan.dto.ActivityComparisonItemDto;
import com.trainingsplan.dto.ProfileCompletionDto;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.CompletedTrainingService;
import com.trainingsplan.service.StravaService;
import com.trainingsplan.service.UserProfileValidationService;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/completed-trainings")
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

    @Autowired
    private UserProfileValidationService userProfileValidationService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFitFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate trainingDate,
            @RequestParam(value = "trainingId", required = false) Long trainingId) {
        
        try {
            User user = securityUtils.getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
            if (!completion.complete()) {
                return ResponseEntity.badRequest().body(completion);
            }

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
            User user = securityUtils.getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
            if (!completion.complete()) {
                return ResponseEntity.badRequest().body(completion);
            }

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
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return completedTrainingRepository.findByIdAndUserId(id, user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/compare")
    public ResponseEntity<List<ActivityComparisonItemDto>> compareActivities(
            @RequestParam List<Long> ids) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ActivityComparisonItemDto> result = new ArrayList<>();
        List<Long> limitedIds = ids.size() > 6 ? ids.subList(0, 6) : ids;

        for (Long id : limitedIds) {
            Optional<CompletedTraining> ctOpt = completedTrainingRepository.findByIdAndUserId(id, user.getId());
            if (ctOpt.isEmpty()) {
                continue;
            }
            CompletedTraining ct = ctOpt.get();
            Optional<ActivityMetrics> metricsOpt = activityMetricsRepository.findByCompletedTrainingId(ct.getId());

            ActivityComparisonItemDto dto = new ActivityComparisonItemDto();
            dto.setId(ct.getId());
            dto.setActivityName(ct.getActivityName());
            dto.setSport(ct.getSport());
            dto.setTrainingDate(ct.getTrainingDate() != null ? ct.getTrainingDate().toString() : null);
            dto.setDistanceKm(ct.getDistanceKm());
            dto.setDurationSeconds(ct.getDurationSeconds());
            dto.setMovingTimeSeconds(ct.getMovingTimeSeconds());
            dto.setAveragePaceSecondsPerKm(ct.getAveragePaceSecondsPerKm());
            dto.setAverageSpeedKmh(ct.getAverageSpeedKmh());
            dto.setAverageHeartRate(ct.getAverageHeartRate());
            dto.setMaxHeartRate(ct.getMaxHeartRate());
            dto.setAveragePowerWatts(ct.getAveragePowerWatts());
            dto.setNormalizedPowerWatts(ct.getNormalizedPowerWatts());
            dto.setAverageCadence(ct.getAverageCadence());
            dto.setElevationGainM(ct.getElevationGainM());
            dto.setCalories(ct.getCalories());
            dto.setSource(ct.getSource());

            metricsOpt.ifPresent(am -> {
                dto.setZ1Min(am.getZ1Min());
                dto.setZ2Min(am.getZ2Min());
                dto.setZ3Min(am.getZ3Min());
                dto.setZ4Min(am.getZ4Min());
                dto.setZ5Min(am.getZ5Min());
                dto.setStrain21(am.getStrain21());
                dto.setTrimp(am.getTrimp());
                dto.setEfficiencyFactor(am.getEfficiencyFactor());
                dto.setDecouplingPct(am.getDecouplingPct());
            });

            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/training-types")
    public ResponseEntity<List<String>> getTrainingTypes() {
        return ResponseEntity.ok(List.of(
            "Langer Lauf",
            "Intervalle",
            "Tempo",
            "Fahrtspiel",
            "Regenerationslauf",
            "Wettkampf",
            "Krafttraining",
            "Schwimmen",
            "Radfahren",
            "Allgemein"
        ));
    }

    @PatchMapping("/{id}/training-type")
    public ResponseEntity<?> updateTrainingType(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return completedTrainingRepository.findByIdAndUserId(id, user.getId())
                .map(ct -> {
                    ct.setTrainingType(body.get("trainingType"));
                    completedTrainingRepository.save(ct);
                    return ResponseEntity.ok(ct);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompletedTraining(@PathVariable Long id) {
        // Diese Methode kann später implementiert werden
        return ResponseEntity.notFound().build();
    }
}
