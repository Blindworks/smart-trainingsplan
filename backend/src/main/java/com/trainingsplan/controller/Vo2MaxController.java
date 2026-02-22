package com.trainingsplan.controller;

import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.service.CompletedTrainingService;
import com.trainingsplan.dto.StravaActivityDto;
import com.trainingsplan.service.Vo2MaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vo2max")
@CrossOrigin(origins = "http://localhost:4200")
public class Vo2MaxController {

    private final Vo2MaxService vo2MaxService;
    private final CompletedTrainingService completedTrainingService;

    public Vo2MaxController(Vo2MaxService vo2MaxService, CompletedTrainingService completedTrainingService) {
        this.vo2MaxService = vo2MaxService;
        this.completedTrainingService = completedTrainingService;
    }

    @PostMapping("/estimate/training")
    public ResponseEntity<?> estimateFromTraining(@RequestBody Vo2MaxTrainingRequest request) {
        if (request.getSport() == null || !request.getSport().toLowerCase().contains("run")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "VO2max wird nur für Laufaktivitäten berechnet."
            ));
        }

        Double distanceMeters = request.getDistanceKm() != null ? request.getDistanceKm() * 1000 : null;
        Optional<Double> vo2Max = vo2MaxService.calculate(distanceMeters, request.getMovingTimeSeconds());
        if (vo2Max.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "VO2max konnte nicht berechnet werden. Distanz oder Zeit ungültig."
            ));
        }

        return ResponseEntity.ok(Map.of("vo2max", vo2Max.get()));
    }

    @PostMapping("/estimate")
    public ResponseEntity<?> estimateFromActivity(@RequestBody StravaActivityDto activity) {
        Optional<Double> vo2Max = vo2MaxService.calculateFromActivity(activity);
        if (vo2Max.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "VO2max konnte nicht berechnet werden. Erwartet wird eine Laufaktivitaet mit gueltiger Distanz und Zeit."
            ));
        }

        return ResponseEntity.ok(Map.of("vo2max", vo2Max.get()));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentVo2Max() {
        Optional<CompletedTraining> trainingOptional = completedTrainingService.getLatestRunningTrainingForCurrentUser();
        if (trainingOptional.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("vo2max", null);
            return ResponseEntity.ok(response);
        }

        CompletedTraining training = trainingOptional.get();
        Integer movingTimeSeconds = training.getMovingTimeSeconds() != null
                ? training.getMovingTimeSeconds()
                : training.getDurationSeconds();
        Double distanceMeters = training.getDistanceKm() != null ? training.getDistanceKm() * 1000 : null;

        Optional<Double> vo2Max = vo2MaxService.calculate(distanceMeters, movingTimeSeconds);
        if (vo2Max.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("vo2max", null);
            return ResponseEntity.ok(response);
        }

        Optional<Double> vo2MaxHR = vo2MaxService.calculateHRCorrected(
                distanceMeters, movingTimeSeconds,
                training.getAverageHeartRate(), training.getMaxHeartRate());

        Map<String, Object> response = new HashMap<>();
        response.put("vo2max", vo2Max.get());
        response.put("vo2maxHRCorrected", vo2MaxHR.orElse(null));
        response.put("avgHeartRate", training.getAverageHeartRate());
        response.put("maxHeartRate", training.getMaxHeartRate());
        response.put("trainingDate", training.getTrainingDate());
        response.put("activityName", training.getActivityName());
        return ResponseEntity.ok(response);
    }

    public static class Vo2MaxTrainingRequest {
        private Double distanceKm;
        private Integer movingTimeSeconds;
        private String sport;

        public Double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
        public Integer getMovingTimeSeconds() { return movingTimeSeconds; }
        public void setMovingTimeSeconds(Integer movingTimeSeconds) { this.movingTimeSeconds = movingTimeSeconds; }
        public String getSport() { return sport; }
        public void setSport(String sport) { this.sport = sport; }
    }
}
