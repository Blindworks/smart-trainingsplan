package com.trainingsplan.controller;

import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-metrics")
@CrossOrigin(origins = "http://localhost:4200")
public class DailyMetricsController {

    private final DailyMetricsRepository dailyMetricsRepository;
    private final SecurityUtils securityUtils;

    public DailyMetricsController(DailyMetricsRepository dailyMetricsRepository, SecurityUtils securityUtils) {
        this.dailyMetricsRepository = dailyMetricsRepository;
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
}
