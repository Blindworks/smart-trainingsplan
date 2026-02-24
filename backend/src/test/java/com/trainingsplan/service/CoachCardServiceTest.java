package com.trainingsplan.service;

import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.Recommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CoachCardService}.
 * No Spring context — the service is stateless and has no dependencies.
 * All tests are deterministic given fixed inputs.
 */
class CoachCardServiceTest {

    private CoachCardService service;

    @BeforeEach
    void setUp() {
        service = new CoachCardService();
    }

    // ── Title tests ───────────────────────────────────────────────────────────

    @Test
    void title_rest() {
        var card = service.generate(Recommendation.REST, AcwrFlag.GREEN, 25, null, null, 0.0);
        assertEquals("Rest / Recovery", card.title());
    }

    @Test
    void title_easy() {
        var card = service.generate(Recommendation.EASY, AcwrFlag.GREEN, 45, null, null, 0.0);
        assertEquals("Easy day recommended", card.title());
    }

    @Test
    void title_moderate() {
        var card = service.generate(Recommendation.MODERATE, AcwrFlag.GREEN, 60, null, null, 0.0);
        assertEquals("Moderate training day", card.title());
    }

    @Test
    void title_hard() {
        var card = service.generate(Recommendation.HARD, AcwrFlag.GREEN, 80, null, null, 0.0);
        assertEquals("Hard training possible", card.title());
    }

    @Test
    void title_nullRecommendation_returnsDefault() {
        var card = service.generate(null, null, 50, null, null, 0.0);
        assertEquals("Training day", card.title());
    }

    // ── Bullet: ACWR RED ──────────────────────────────────────────────────────

    @Test
    void bullet_acwrRed_appearsFirst() {
        var card = service.generate(Recommendation.EASY, AcwrFlag.RED, 40, null, null, 0.0);
        assertTrue(card.bullets().contains("Akute Belastung deutlich über deinem 4-Wochen-Niveau"));
        assertEquals("Akute Belastung deutlich über deinem 4-Wochen-Niveau", card.bullets().get(0));
    }

    @Test
    void bullet_acwrNotRed_noAcwrBullet() {
        var card = service.generate(Recommendation.EASY, AcwrFlag.ORANGE, 55, null, null, 0.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("4-Wochen-Niveau")));
    }

    // ── Bullet: HR drift ─────────────────────────────────────────────────────

    @Test
    void bullet_decouplingAbove10_hrDriftBullet() {
        var card = service.generate(Recommendation.MODERATE, AcwrFlag.GREEN, 60, null, 12.5, 0.0);
        assertTrue(card.bullets().contains(
                "Hohe HR-Drift → Hinweis auf Ermüdung/Hitze/zu hohes Tempo"));
    }

    @Test
    void bullet_decouplingExactly10_noHrDriftBullet() {
        var card = service.generate(Recommendation.MODERATE, AcwrFlag.GREEN, 60, null, 10.0, 0.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("HR-Drift")));
    }

    @Test
    void bullet_decouplingNull_noHrDriftBullet() {
        var card = service.generate(Recommendation.EASY, AcwrFlag.GREEN, 55, null, null, 0.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("HR-Drift")));
    }

    // ── Bullet: Positive signal ───────────────────────────────────────────────

    @Test
    void bullet_score70AndGreen_positiveBullet() {
        var card = service.generate(Recommendation.HARD, AcwrFlag.GREEN, 70, null, null, 0.0);
        assertTrue(card.bullets().contains(
                "Belastung stabil, du kannst Qualität trainieren"));
    }

    @Test
    void bullet_score70ButNotGreen_noPositiveBullet() {
        var card = service.generate(Recommendation.HARD, AcwrFlag.ORANGE, 70, null, null, 0.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("Qualität trainieren")));
    }

    @Test
    void bullet_score69AndGreen_noPositiveBullet() {
        var card = service.generate(Recommendation.MODERATE, AcwrFlag.GREEN, 69, null, null, 0.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("Qualität trainieren")));
    }

    // ── Bullet: REST-specific (high strain) ──────────────────────────────────

    @Test
    void bullet_restAndHighYesterdayStrain_strainBullet() {
        var card = service.generate(Recommendation.REST, AcwrFlag.GREEN, 25, 15.0, null, 0.0);
        assertTrue(card.bullets().contains(
                "Hohe gestrige Belastung – Erholung priorisieren"));
    }

    @Test
    void bullet_restAndYesterdayStrainExactly14_noStrainBullet() {
        var card = service.generate(Recommendation.REST, AcwrFlag.GREEN, 25, 14.0, null, 0.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("gestrige Belastung")));
    }

    @Test
    void bullet_easyAndHighStrain_noStrainBullet() {
        // High strain bullet only triggers for REST recommendation
        var card = service.generate(Recommendation.EASY, AcwrFlag.GREEN, 45, 16.0, null, 0.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("gestrige Belastung")));
    }

    // ── Bullet: REST-specific (high intensity) ────────────────────────────────

    @Test
    void bullet_restAndHighZ45Sum_intensityBullet() {
        var card = service.generate(Recommendation.REST, AcwrFlag.GREEN, 25, null, null, 21.0);
        assertTrue(card.bullets().contains(
                "Hohe Intensität letzte 2 Tage – Pause empfohlen"));
    }

    @Test
    void bullet_restAndZ45SumExactly20_noIntensityBullet() {
        var card = service.generate(Recommendation.REST, AcwrFlag.GREEN, 25, null, null, 20.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("Intensität letzte")));
    }

    @Test
    void bullet_moderateAndHighZ45Sum_noIntensityBullet() {
        // Intensity bullet only triggers for REST recommendation
        var card = service.generate(Recommendation.MODERATE, AcwrFlag.GREEN, 60, null, null, 25.0);
        assertFalse(card.bullets().stream()
                .anyMatch(b -> b.contains("Intensität letzte")));
    }

    // ── Max 3 bullets enforced ────────────────────────────────────────────────

    @Test
    void maxThreeBulletsEnforced() {
        // All conditions true: RED acwr, high strain (REST), high z45 (REST), decoupling>10, green+70
        // Note: RED + score>=70 + GREEN is contradictory in practice, but tests the cap
        var card = service.generate(
                Recommendation.REST,
                AcwrFlag.RED,
                70,       // would trigger positive, but it needs GREEN
                15.0,     // high strain bullet
                12.0,     // decoupling bullet
                21.0);    // high intensity bullet
        assertTrue(card.bullets().size() <= 3);
    }

    @Test
    void rest_allConditionsTrue_exactlyThreeBullets_inCorrectOrder() {
        // REST + RED acwr + high strain + high z45 → 3 bullets max
        // Priority: RED (1st), high strain (2nd), high z45 (3rd) — decoupling gets cut
        var card = service.generate(
                Recommendation.REST,
                AcwrFlag.RED,
                20,
                16.0,   // yesterday strain > 14
                15.0,   // decoupling > 10 (should be 4th, cut off)
                22.0);  // z45 > 20

        assertEquals(3, card.bullets().size());
        assertEquals("Akute Belastung deutlich über deinem 4-Wochen-Niveau", card.bullets().get(0));
        assertEquals("Hohe gestrige Belastung – Erholung priorisieren", card.bullets().get(1));
        assertEquals("Hohe Intensität letzte 2 Tage – Pause empfohlen", card.bullets().get(2));
    }

    @Test
    void noBulletsWhenAllConditionsFalse() {
        // GREEN, score=50 (not >=70), no strain, no decoupling, no z45, EASY
        var card = service.generate(Recommendation.EASY, AcwrFlag.GREEN, 50, null, null, 0.0);
        assertTrue(card.bullets().isEmpty());
    }

    // ── Typical scenarios ──────────────────────────────────────────────────────

    @Test
    void scenario_hardDayGreenLoad() {
        // Good day: score 75, GREEN, no deductions
        var card = service.generate(Recommendation.HARD, AcwrFlag.GREEN, 75, 8.0, null, 5.0);
        assertEquals("Hard training possible", card.title());
        assertEquals(List.of("Belastung stabil, du kannst Qualität trainieren"), card.bullets());
    }

    @Test
    void scenario_restDayRedAcwrHighStrain() {
        // Overloaded: REST, RED, high strain yesterday, decoupling fine
        var card = service.generate(Recommendation.REST, AcwrFlag.RED, 20, 18.0, null, 5.0);
        assertEquals("Rest / Recovery", card.title());
        assertEquals(2, card.bullets().size());
        assertTrue(card.bullets().get(0).contains("4-Wochen-Niveau"));
        assertTrue(card.bullets().get(1).contains("gestrige Belastung"));
    }

    @Test
    void scenario_easyDayHighDecoupling() {
        // Tired: EASY, ORANGE, decoupling 11%
        var card = service.generate(Recommendation.EASY, AcwrFlag.ORANGE, 45, 10.0, 11.0, 8.0);
        assertEquals("Easy day recommended", card.title());
        assertEquals(List.of("Hohe HR-Drift → Hinweis auf Ermüdung/Hitze/zu hohes Tempo"),
                card.bullets());
    }
}
