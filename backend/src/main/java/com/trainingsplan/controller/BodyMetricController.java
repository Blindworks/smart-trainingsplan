package com.trainingsplan.controller;

import com.trainingsplan.entity.BodyMetric;
import com.trainingsplan.service.BodyMetricService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/body-metrics")
public class BodyMetricController {

    private final BodyMetricService bodyMetricService;

    public BodyMetricController(BodyMetricService bodyMetricService) {
        this.bodyMetricService = bodyMetricService;
    }

    /**
     * Returns the latest value for each metric type for the authenticated user.
     * Response example:
     * [
     *   { "metricType": "VO2MAX", "label": "VO2max (Pace-basiert)", "value": 52.3, "unit": "ml/kg/min", "recordedAt": "2025-01-15" },
     *   { "metricType": "VO2MAX_HR_CORRECTED", "label": "VO2max (HF-korrigiert)", "value": 48.7, "unit": "ml/kg/min", "recordedAt": "2025-01-15" }
     * ]
     */
    @GetMapping("/current")
    public ResponseEntity<List<Map<String, Object>>> getCurrentMetrics() {
        List<BodyMetric> metrics = bodyMetricService.getLatestMetricsForCurrentUser();
        List<Map<String, Object>> response = metrics.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Recalculates all body metrics for the current user from their stored
     * CompletedTraining records. Use this to populate metrics for activities
     * uploaded before this feature existed.
     */
    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, Object>> recalculate() {
        int count = bodyMetricService.recalculateForCurrentUser();
        return ResponseEntity.ok(Map.of(
                "message", "Metriken neu berechnet",
                "activitiesProcessed", count
        ));
    }

    private Map<String, Object> toDto(BodyMetric m) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("metricType", m.getMetricType());
        dto.put("label", labelFor(m.getMetricType()));
        dto.put("value", m.getValue());
        dto.put("unit", m.getUnit());
        dto.put("recordedAt", m.getRecordedAt() != null ? m.getRecordedAt().toString() : null);
        dto.put("sourceActivityId", m.getSourceActivityId());
        return dto;
    }

    private String labelFor(String metricType) {
        return switch (metricType) {
            case "VO2MAX" -> "VO2max (Pace-basiert)";
            case "VO2MAX_HR_CORRECTED" -> "VO2max (HF-korrigiert)";
            default -> metricType;
        };
    }
}
