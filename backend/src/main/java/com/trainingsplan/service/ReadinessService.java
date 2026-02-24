package com.trainingsplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.Recommendation;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the Readiness Proxy score (v1) from load and activity metrics.
 *
 * <p>No HRV or sleep data required — uses only Strava/load-derived signals.
 *
 * <h3>Heuristic v1</h3>
 * <pre>
 * Base score: 80
 *
 * Deductions (applied in order):
 *   acwrFlag == RED    → −35  (recommendation capped at EASY)
 *   acwrFlag == ORANGE → −20
 *   yesterday dailyStrain21 > 14 → −15
 *   last eligible decouplingPct > 10% → −10
 *   last eligible decouplingPct > 5%  → −5   (exclusive of >10% branch)
 *   sum(z4Min + z5Min) in last 2 days > 20 → −10
 *
 * Score clamped to [0, 100].
 * </pre>
 *
 * <h3>Recommendation thresholds</h3>
 * <pre>
 *   score &lt; 30  → REST
 *   30–49       → EASY
 *   50–69       → MODERATE
 *   ≥ 70        → HARD
 * </pre>
 *
 * When acwrFlag == RED the recommendation is capped at EASY (cannot be MODERATE or HARD).
 */
@Service
public class ReadinessService {

    @Autowired
    private DailyMetricsRepository dailyMetricsRepository;

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Computes and persists the readiness score for {@code user} on {@code date}.
     *
     * <p>Must be called AFTER {@link LoadModelService#updateAcwr} has persisted today's
     * ACWR data, so that the acwrFlag is available.
     */
    public void compute(User user, LocalDate date) {
        int score = 80;
        List<String> reasons = new ArrayList<>();
        boolean redFlag = false;

        // ── 1. ACWR flag ──────────────────────────────────────────────────────
        AcwrFlag acwrFlag = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date)
                .map(DailyMetrics::getAcwrFlag)
                .orElse(null);

        if (acwrFlag == AcwrFlag.RED) {
            score -= 35;
            reasons.add("Hohes ACWR – Verletzungsrisiko (ROT)");
            redFlag = true;
        } else if (acwrFlag == AcwrFlag.ORANGE) {
            score -= 20;
            reasons.add("Erhöhtes ACWR – Belastung beachten (ORANGE)");
        }

        // ── 2. Yesterday's strain21 ──────────────────────────────────────────
        Double yesterdayStrain = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date.minusDays(1))
                .map(DailyMetrics::getDailyStrain21)
                .orElse(null);

        if (yesterdayStrain != null && yesterdayStrain > 14.0) {
            score -= 15;
            reasons.add("Hohe gestrige Belastung (strain21 > 14)");
        }

        // ── 3. Last eligible decoupling ───────────────────────────────────────
        List<ActivityMetrics> latestDecoupling = activityMetricsRepository
                .findEligibleDecouplingByUserId(user.getId(), PageRequest.of(0, 1));

        if (!latestDecoupling.isEmpty()) {
            Double decPct = latestDecoupling.get(0).getDecouplingPct();
            if (decPct != null && decPct > 10.0) {
                score -= 10;
                reasons.add("Starkes Herzdriften zuletzt (>10%)");
            } else if (decPct != null && decPct > 5.0) {
                score -= 5;
                reasons.add("Leichtes Herzdriften zuletzt (>5%)");
            }
        }

        // ── 4. Z4+Z5 minutes in last 2 days ──────────────────────────────────
        double z45Sum = activityMetricsRepository
                .sumZ4Z5MinByUserIdAndDateRange(user.getId(), date.minusDays(1), date);

        if (z45Sum > 20.0) {
            score -= 10;
            reasons.add("Viele Hochintensivminuten letzte 2 Tage (>20 min Z4/Z5)");
        }

        // ── Clamp ─────────────────────────────────────────────────────────────
        score = Math.max(0, Math.min(100, score));

        // ── Recommendation ────────────────────────────────────────────────────
        Recommendation recommendation;
        if (score < 30) {
            recommendation = Recommendation.REST;
        } else if (score < 50) {
            recommendation = Recommendation.EASY;
        } else if (score < 70) {
            recommendation = Recommendation.MODERATE;
        } else {
            recommendation = Recommendation.HARD;
        }

        // Cap at EASY when RED flag (score math already makes this unlikely,
        // but the spec requires it explicitly)
        if (redFlag && recommendation != Recommendation.REST) {
            recommendation = Recommendation.EASY;
        }

        // ── Serialize reasons ─────────────────────────────────────────────────
        List<String> topReasons = reasons.subList(0, Math.min(3, reasons.size()));
        String reasonsJson;
        try {
            reasonsJson = objectMapper.writeValueAsString(topReasons);
        } catch (JsonProcessingException e) {
            reasonsJson = "[]";
        }

        // ── Persist ───────────────────────────────────────────────────────────
        DailyMetrics daily = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date)
                .orElse(new DailyMetrics());

        daily.setUser(user);
        daily.setDate(date);
        daily.setReadinessScore(score);
        daily.setRecommendation(recommendation);
        daily.setReasonsJson(reasonsJson);
        dailyMetricsRepository.save(daily);
    }

    /**
     * Recomputes readiness for the last 90 days for the given user.
     * Requires ACWR to already be populated for those days.
     */
    public void recomputeForUser(User user) {
        LocalDate today = LocalDate.now();
        for (int i = 89; i >= 0; i--) {
            compute(user, today.minusDays(i));
        }
    }
}
