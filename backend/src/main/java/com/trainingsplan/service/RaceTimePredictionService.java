package com.trainingsplan.service;

import com.trainingsplan.entity.BodyMetric;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.repository.BodyMetricRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Computes context-aware race time predictions by combining:
 * <ol>
 *   <li>The athlete's latest VO2Max (Daniels/VDOT base prediction)</li>
 *   <li>Average weekly mileage (last 4 weeks) — affects endurance races</li>
 *   <li>Longest single run in the last 6 weeks — critical for half/marathon</li>
 *   <li>ACWR-based fatigue factor — high acute load slows predicted times</li>
 * </ol>
 *
 * <p>Inspired by Garmin's race-time prediction approach, which considers
 * training load trends and mileage data alongside aerobic capacity.</p>
 */
@Service
public class RaceTimePredictionService {

    // ── Distance configuration ────────────────────────────────────────────────

    private static final String[] KEYS     = {"1km", "5km", "10km", "Halbmarathon", "Marathon"};
    private static final double[] DIST_M   = {1_000, 5_000, 10_000, 21_097.5, 42_195};

    /**
     * Weekly km above which no mileage penalty is applied, per distance.
     * 0 means the distance is not significantly volume-dependent.
     */
    private static final double[] REF_WEEKLY_KM       = {0, 30, 40, 50, 60};
    /** Maximum mileage-based time penalty (fraction, e.g. 0.07 = +7%). */
    private static final double[] MAX_MILEAGE_PENALTY  = {0.00, 0.04, 0.07, 0.12, 0.20};

    /**
     * Optimal long-run distance (km) needed in the last 6 weeks.
     * 0 means not applicable for this distance.
     */
    private static final double[] OPTIMAL_LONG_RUN_KM  = {0, 0, 0, 18, 28};
    /** Maximum long-run-deficit penalty (fraction). */
    private static final double[] MAX_LONG_RUN_PENALTY  = {0, 0, 0, 0.06, 0.12};

    // ── Dependencies ─────────────────────────────────────────────────────────

    @Autowired private BodyMetricRepository        bodyMetricRepository;
    @Autowired private CompletedTrainingRepository completedTrainingRepository;
    @Autowired private DailyMetricsRepository      dailyMetricsRepository;
    @Autowired private Vo2MaxService               vo2MaxService;
    @Autowired private SecurityUtils               securityUtils;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns context-aware race time predictions for the current user.
     *
     * @return map with keys: vo2max, vo2maxDate, avgWeeklyKm, maxLongRunKm,
     *         runsPerWeek, acwr, readinessScore, confidence, predictions.
     *         Empty map if no VO2Max data exists.
     */
    public Map<String, Object> computeForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyMap();

        // 1 ── Latest VO2Max ─────────────────────────────────────────────────
        Optional<BodyMetric> latestMetric = bodyMetricRepository
                .findTopByUserIdAndMetricTypeOrderByRecordedAtDesc(userId, "VO2MAX");
        if (latestMetric.isEmpty()) return Collections.emptyMap();

        double    vo2max     = latestMetric.get().getValue();
        LocalDate vo2maxDate = latestMetric.get().getRecordedAt();

        // 2 ── Recent runs ───────────────────────────────────────────────────
        LocalDate today       = LocalDate.now();
        LocalDate sixWeeksAgo = today.minusWeeks(6);
        LocalDate fourWeeksAgo= today.minusWeeks(4);

        List<CompletedTraining> allRecent = completedTrainingRepository
                .findByUserIdAndTrainingDateBetweenOrderByTrainingDate(userId, sixWeeksAgo, today);

        List<CompletedTraining> recentRuns = allRecent.stream()
                .filter(this::isRunning)
                .toList();

        List<CompletedTraining> last4wRuns = recentRuns.stream()
                .filter(t -> !t.getTrainingDate().isBefore(fourWeeksAgo))
                .toList();

        // 3 ── Training volume stats ─────────────────────────────────────────
        double totalKm4w = last4wRuns.stream()
                .filter(t -> t.getDistanceKm() != null)
                .mapToDouble(CompletedTraining::getDistanceKm)
                .sum();
        double avgWeeklyKm = totalKm4w / 4.0;

        double maxLongRunKm = recentRuns.stream()
                .filter(t -> t.getDistanceKm() != null)
                .mapToDouble(CompletedTraining::getDistanceKm)
                .max()
                .orElse(0);

        double runsPerWeek = last4wRuns.size() / 4.0;

        // 4 ── ACWR / readiness (last 7 days) ────────────────────────────────
        List<DailyMetrics> recentMetrics = dailyMetricsRepository
                .findByUserIdAndDateBetween(userId, today.minusDays(7), today);
        recentMetrics.sort(Comparator.comparing(DailyMetrics::getDate).reversed());

        Double  latestAcwr      = recentMetrics.stream()
                .filter(m -> m.getAcwr() != null)
                .mapToDouble(DailyMetrics::getAcwr)
                .boxed().findFirst().orElse(null);
        Integer latestReadiness = recentMetrics.stream()
                .filter(m -> m.getReadinessScore() != null)
                .mapToInt(DailyMetrics::getReadinessScore)
                .boxed().findFirst().orElse(null);

        double acwrPenalty = computeAcwrPenalty(latestAcwr);

        // 5 ── Confidence ────────────────────────────────────────────────────
        long   vo2maxAgeDays = vo2maxDate != null ? ChronoUnit.DAYS.between(vo2maxDate, today) : 999;
        String confidence    = computeConfidence(vo2maxAgeDays, runsPerWeek, avgWeeklyKm);

        // 6 ── Per-distance predictions ──────────────────────────────────────
        List<Map<String, Object>> predictions = new ArrayList<>();
        for (int i = 0; i < KEYS.length; i++) {
            int baseSeconds = vo2MaxService.predictTimeSeconds(vo2max, DIST_M[i]);

            // Mileage penalty
            double mileagePenalty = 0;
            if (REF_WEEKLY_KM[i] > 0 && avgWeeklyKm < REF_WEEKLY_KM[i]) {
                double shortfall = (REF_WEEKLY_KM[i] - avgWeeklyKm) / REF_WEEKLY_KM[i];
                mileagePenalty = shortfall * MAX_MILEAGE_PENALTY[i];
            }

            // Long-run penalty (half / marathon only)
            double longRunPenalty = 0;
            if (OPTIMAL_LONG_RUN_KM[i] > 0 && maxLongRunKm < OPTIMAL_LONG_RUN_KM[i]) {
                double shortfall = (OPTIMAL_LONG_RUN_KM[i] - maxLongRunKm) / OPTIMAL_LONG_RUN_KM[i];
                longRunPenalty = shortfall * MAX_LONG_RUN_PENALTY[i];
            }

            double totalPenalty    = Math.min(0.40, mileagePenalty + longRunPenalty + acwrPenalty);
            int    adjustedSeconds = (int) Math.round(baseSeconds * (1.0 + totalPenalty));
            int    adjustmentPct   = (int) Math.round(totalPenalty * 100);

            Map<String, Object> pred = new LinkedHashMap<>();
            pred.put("distance",      KEYS[i]);
            pred.put("baseTime",      formatSeconds(baseSeconds));
            pred.put("adjustedTime",  formatSeconds(adjustedSeconds));
            pred.put("adjustmentPct", adjustmentPct);
            predictions.add(pred);
        }

        // 7 ── Assemble result ────────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vo2max",        round(vo2max, 1));
        result.put("vo2maxDate",    vo2maxDate != null ? vo2maxDate.toString() : null);
        result.put("avgWeeklyKm",   round(avgWeeklyKm, 1));
        result.put("maxLongRunKm",  round(maxLongRunKm, 1));
        result.put("runsPerWeek",   round(runsPerWeek, 1));
        result.put("acwr",          latestAcwr  != null ? round(latestAcwr, 2) : null);
        result.put("readinessScore",latestReadiness);
        result.put("confidence",    confidence);
        result.put("predictions",   predictions);
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Additional time penalty due to acute training overload (high ACWR).
     * Undertraining is already captured via the mileage factor.
     */
    private double computeAcwrPenalty(Double acwr) {
        if (acwr == null) return 0;
        if (acwr <= 1.3)  return 0;
        if (acwr <= 1.6)  return (acwr - 1.3) / 0.3 * 0.02;   // 0–2 %
        return Math.min(0.05, 0.02 + (acwr - 1.6) / 0.4 * 0.03); // 2–5 %
    }

    private String computeConfidence(long vo2maxAgeDays, double runsPerWeek, double avgWeeklyKm) {
        if (vo2maxAgeDays > 60 || runsPerWeek < 1.5) return "NIEDRIG";
        if (vo2maxAgeDays > 21 || runsPerWeek < 3.0 || avgWeeklyKm < 20) return "MITTEL";
        return "HOCH";
    }

    private boolean isRunning(CompletedTraining t) {
        String sport = t.getSport();
        if (sport != null) return sport.toLowerCase().contains("run");
        return t.getAveragePaceSecondsPerKm() != null
                || (t.getDistanceKm() != null && t.getAveragePowerWatts() == null);
    }

    private String formatSeconds(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
