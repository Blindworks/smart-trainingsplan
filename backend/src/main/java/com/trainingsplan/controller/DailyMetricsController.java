package com.trainingsplan.controller;

import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.DailyMetricsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/daily-metrics")
@CrossOrigin(origins = "http://localhost:4200")
public class DailyMetricsController {

    private final DailyMetricsRepository dailyMetricsRepository;
    private final DailyMetricsService dailyMetricsService;
    private final SecurityUtils securityUtils;

    public DailyMetricsController(DailyMetricsRepository dailyMetricsRepository,
                                   DailyMetricsService dailyMetricsService,
                                   SecurityUtils securityUtils) {
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.dailyMetricsService = dailyMetricsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<DailyMetrics>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = securityUtils.getCurrentUserId();
        List<DailyMetrics> metrics = dailyMetricsRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Recomputes the rolling 7-day and 28-day EF averages for the last 90 days
     * for the currently authenticated user.
     */
    @PostMapping("/recompute-ef")
    public ResponseEntity<Map<String, Object>> recomputeEf() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        dailyMetricsService.recomputeEfForUser(user);
        return ResponseEntity.ok(Map.of(
                "message", "EF rolling averages recomputed",
                "daysProcessed", 90
        ));
    }
}
