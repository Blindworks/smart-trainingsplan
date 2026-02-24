package com.trainingsplan.service;

import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.Recommendation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates rule-based coach card titles and bullets from daily and activity metrics.
 * No LLM involved — fully deterministic.
 *
 * <h3>Title rules</h3>
 * <pre>
 *   REST     → "Rest / Recovery"
 *   EASY     → "Easy day recommended"
 *   MODERATE → "Moderate training day"
 *   HARD     → "Hard training possible"
 * </pre>
 *
 * <h3>Bullet rules (max 3, in priority order)</h3>
 * <pre>
 *   1. acwrFlag == RED             → "Akute Belastung deutlich über deinem 4-Wochen-Niveau"
 *   2. REST && yesterdayStrain > 14 → "Hohe gestrige Belastung – Erholung priorisieren"
 *   3. REST && z45Sum > 20          → "Hohe Intensität letzte 2 Tage – Pause empfohlen"
 *   4. lastDecouplingPct > 10       → "Hohe HR-Drift → Hinweis auf Ermüdung/Hitze/zu hohes Tempo"
 *   5. score >= 70 && GREEN         → "Belastung stabil, du kannst Qualität trainieren"
 * </pre>
 */
@Service
public class CoachCardService {

    public record CoachCard(String title, List<String> bullets) {}

    /**
     * Generates the coach card for a training day.
     *
     * @param recommendation    today's recommendation (REST/EASY/MODERATE/HARD)
     * @param acwrFlag          today's ACWR load zone (may be null when no ACWR data)
     * @param readinessScore    readiness 0–100
     * @param yesterdayStrain   dailyStrain21 of the previous day (null if no data)
     * @param lastDecouplingPct decouplingPct of last eligible run (null if no data)
     * @param z45Sum            total z4+z5 minutes over last 2 days
     */
    public CoachCard generate(
            Recommendation recommendation,
            AcwrFlag acwrFlag,
            int readinessScore,
            Double yesterdayStrain,
            Double lastDecouplingPct,
            double z45Sum) {

        String title = toTitle(recommendation);
        List<String> bullets = buildBullets(
                recommendation, acwrFlag, readinessScore, yesterdayStrain, lastDecouplingPct, z45Sum);
        return new CoachCard(title, bullets);
    }

    private String toTitle(Recommendation recommendation) {
        if (recommendation == null) return "Training day";
        return switch (recommendation) {
            case REST     -> "Rest / Recovery";
            case EASY     -> "Easy day recommended";
            case MODERATE -> "Moderate training day";
            case HARD     -> "Hard training possible";
        };
    }

    private List<String> buildBullets(
            Recommendation recommendation,
            AcwrFlag acwrFlag,
            int readinessScore,
            Double yesterdayStrain,
            Double lastDecouplingPct,
            double z45Sum) {

        List<String> bullets = new ArrayList<>();

        // 1. ACWR overload warning (highest priority, applies always)
        if (acwrFlag == AcwrFlag.RED) {
            bullets.add("Akute Belastung deutlich über deinem 4-Wochen-Niveau");
        }

        // 2. REST-specific: high yesterday strain
        if (recommendation == Recommendation.REST
                && yesterdayStrain != null && yesterdayStrain > 14.0
                && bullets.size() < 3) {
            bullets.add("Hohe gestrige Belastung – Erholung priorisieren");
        }

        // 3. REST-specific: high intensity in last 2 days
        if (recommendation == Recommendation.REST
                && z45Sum > 20.0
                && bullets.size() < 3) {
            bullets.add("Hohe Intensität letzte 2 Tage – Pause empfohlen");
        }

        // 4. HR drift — applies regardless of recommendation
        if (lastDecouplingPct != null && lastDecouplingPct > 10.0 && bullets.size() < 3) {
            bullets.add("Hohe HR-Drift → Hinweis auf Ermüdung/Hitze/zu hohes Tempo");
        }

        // 5. Positive signal when stable and ready
        if (readinessScore >= 70 && acwrFlag == AcwrFlag.GREEN && bullets.size() < 3) {
            bullets.add("Belastung stabil, du kannst Qualität trainieren");
        }

        return bullets;
    }
}
