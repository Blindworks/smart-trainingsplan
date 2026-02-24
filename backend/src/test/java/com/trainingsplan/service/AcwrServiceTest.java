package com.trainingsplan.service;

import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.DailyMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Acute:Chronic Workload Ratio computation in {@link LoadModelService}.
 * No Spring context or database — all dependencies are mocked.
 *
 * <p>Definitions:
 * <ul>
 *   <li>acute7   = sum of daily_strain21 for the 7 days ending on {@code date} (inclusive)</li>
 *   <li>chronic28 = sum28 / 4  (28-day sum divided by 4, same scale as acute7)</li>
 *   <li>acwr     = acute7 / chronic28  (null when chronic28 == 0)</li>
 * </ul>
 *
 * <p>Flag thresholds:
 * <ul>
 *   <li>BLUE   – acwr &lt; 0.8</li>
 *   <li>GREEN  – 0.8 ≤ acwr ≤ 1.3</li>
 *   <li>ORANGE – 1.3 &lt; acwr ≤ 1.6</li>
 *   <li>RED    – acwr &gt; 1.6</li>
 * </ul>
 */
class AcwrServiceTest {

    private DailyMetricsRepository dailyMetricsRepository;
    private LoadModelService service;

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 24);
    private User user;

    @BeforeEach
    void setUp() {
        dailyMetricsRepository = mock(DailyMetricsRepository.class);

        service = new LoadModelService();
        // Inject mock via reflection since the service uses @Autowired field injection
        injectField(service, "dailyMetricsRepository", dailyMetricsRepository);

        user = new User();

        when(dailyMetricsRepository.findByUserIdAndDate(any(), any()))
                .thenReturn(Optional.empty());
        when(dailyMetricsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── 1. Constant load → ACWR = 1.0 → GREEN ────────────────────────────────

    /**
     * 28 days × 10.0 strain:
     * acute7   = 7 × 10 = 70.0
     * sum28    = 28 × 10 = 280.0
     * chronic28 = 280 / 4 = 70.0
     * acwr     = 70 / 70 = 1.0 → GREEN
     */
    @Test
    void constantLoad_acwrIsOne_greenFlag() {
        List<DailyMetrics> window = window28(10.0, 10.0);
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(window);

        service.updateAcwr(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(70.0, saved.getAcute7(), 1e-9);
        assertEquals(70.0, saved.getChronic28(), 1e-9);
        assertEquals(1.0,  saved.getAcwr(), 1e-9);
        assertEquals(AcwrFlag.GREEN, saved.getAcwrFlag());
        assertEquals("Optimale Belastung", saved.getAcwrMessage());
    }

    // ── 2. Spike to exactly ACWR = 1.6 → ORANGE (boundary inclusive) ─────────

    /**
     * Days -27 to -7: 10.0 each (21 days); days -6 to 0: 20.0 each (7 days):
     * acute7   = 7 × 20 = 140.0
     * sum28    = 21 × 10 + 7 × 20 = 210 + 140 = 350.0
     * chronic28 = 350 / 4 = 87.5
     * acwr     = 140 / 87.5 = 1.6 → ORANGE (acwr ≤ 1.6 is ORANGE, not RED)
     */
    @Test
    void spikeToExactly1_6_orangeFlag_boundaryInclusive() {
        List<DailyMetrics> window = window28(10.0, 20.0);
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(window);

        service.updateAcwr(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(140.0,  saved.getAcute7(), 1e-9);
        assertEquals(87.5,   saved.getChronic28(), 1e-9);
        assertEquals(1.6,    saved.getAcwr(), 1e-9);
        assertEquals(AcwrFlag.ORANGE, saved.getAcwrFlag());
        assertEquals("Erhöhte Belastung – Verletzungsrisiko beachten", saved.getAcwrMessage());
    }

    // ── 3. Taper → ACWR ≈ 0.667 → BLUE ──────────────────────────────────────

    /**
     * Days -27 to -7: 20.0 each (21 days); days -6 to 0: 12.0 each (7 days):
     * acute7   = 7 × 12 = 84.0
     * sum28    = 21 × 20 + 7 × 12 = 420 + 84 = 504.0
     * chronic28 = 504 / 4 = 126.0
     * acwr     = 84 / 126 ≈ 0.6667 → BLUE
     */
    @Test
    void taper_lowAcwr_blueFlag() {
        List<DailyMetrics> window = window28(20.0, 12.0);
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(window);

        service.updateAcwr(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(84.0,  saved.getAcute7(), 1e-9);
        assertEquals(126.0, saved.getChronic28(), 1e-9);
        assertEquals(84.0 / 126.0, saved.getAcwr(), 1e-9);
        assertEquals(AcwrFlag.BLUE, saved.getAcwrFlag());
        assertEquals("Unterbelastung – Training steigern", saved.getAcwrMessage());
    }

    // ── 4. High spike → RED ───────────────────────────────────────────────────

    /**
     * Days -27 to -7: 10.0 each (21 days); days -6 to 0: 25.0 each (7 days):
     * acute7   = 7 × 25 = 175.0
     * sum28    = 21 × 10 + 7 × 25 = 210 + 175 = 385.0
     * chronic28 = 385 / 4 = 96.25
     * acwr     = 175 / 96.25 ≈ 1.8182 → RED
     */
    @Test
    void highSpike_acwrAbove1_6_redFlag() {
        List<DailyMetrics> window = window28(10.0, 25.0);
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(window);

        service.updateAcwr(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(175.0,  saved.getAcute7(), 1e-9);
        assertEquals(96.25,  saved.getChronic28(), 1e-9);
        assertEquals(175.0 / 96.25, saved.getAcwr(), 1e-9);
        assertEquals(AcwrFlag.RED, saved.getAcwrFlag());
        assertEquals("Hohes Verletzungsrisiko – Belastung reduzieren", saved.getAcwrMessage());
    }

    // ── 5. Zero chronic (no records) → ACWR null, flag null, message null ─────

    /**
     * Window returns empty list → acute7 = 0, chronic28 = 0 → acwr = null.
     * Flag and message must also be null.
     */
    @Test
    void noRecords_chronicIsZero_acwrNullAndFlagNull() {
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.updateAcwr(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(0.0, saved.getAcute7(), 1e-9);
        assertEquals(0.0, saved.getChronic28(), 1e-9);
        assertNull(saved.getAcwr());
        assertNull(saved.getAcwrFlag());
        assertNull(saved.getAcwrMessage());
    }

    // ── 6. Exactly ACWR = 1.3 → GREEN (upper boundary) ───────────────────────

    /**
     * Days -27 to -7: 9.0 each (21 days); days -6 to 0: 13.0 each (7 days):
     * acute7   = 7 × 13 = 91.0
     * sum28    = 21 × 9 + 7 × 13 = 189 + 91 = 280.0
     * chronic28 = 280 / 4 = 70.0
     * acwr     = 91 / 70 = 1.3 exactly → GREEN (acwr ≤ 1.3 is still GREEN)
     */
    @Test
    void acwrExactly1_3_greenFlag_upperBoundaryInclusive() {
        List<DailyMetrics> window = window28(9.0, 13.0);
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(window);

        service.updateAcwr(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(91.0, saved.getAcute7(), 1e-9);
        assertEquals(70.0, saved.getChronic28(), 1e-9);
        assertEquals(1.3,  saved.getAcwr(), 1e-9);
        assertEquals(AcwrFlag.GREEN, saved.getAcwrFlag());
        assertEquals("Optimale Belastung", saved.getAcwrMessage());
    }

    // ── 7. ACWR just above 1.3 → ORANGE ──────────────────────────────────────

    /**
     * Days -27 to -7: 9.0 each (21 days); days -6 to 0: 13.1 each (7 days):
     * acute7   = 7 × 13.1 = 91.7
     * sum28    = 21 × 9 + 7 × 13.1 = 189 + 91.7 = 280.7
     * chronic28 = 280.7 / 4 = 70.175
     * acwr     = 91.7 / 70.175 ≈ 1.3068 → ORANGE (> 1.3)
     */
    @Test
    void acwrJustAbove1_3_orangeFlag() {
        List<DailyMetrics> window = window28(9.0, 13.1);
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(window);

        service.updateAcwr(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(91.7,   saved.getAcute7(), 1e-9);
        assertEquals(70.175, saved.getChronic28(), 1e-9);
        assertEquals(91.7 / 70.175, saved.getAcwr(), 1e-9);
        assertEquals(AcwrFlag.ORANGE, saved.getAcwrFlag());
        assertEquals("Erhöhte Belastung – Verletzungsrisiko beachten", saved.getAcwrMessage());
    }

    // ── 8. Days with null strain treated as zero ──────────────────────────────

    /**
     * Mix of null-strain and valid-strain records.
     * Null-strain records must be skipped (treated as 0), not throw NullPointerException.
     *
     * Window: 7 days × 10.0 within acute window, 21 days with null strain in the older part.
     * acute7   = 7 × 10 = 70.0
     * sum28    = 0 (nulls) + 70 = 70.0
     * chronic28 = 70 / 4 = 17.5
     * acwr     = 70 / 17.5 = 4.0 → RED
     */
    @Test
    void nullStrainDays_treatedAsZero_noNullPointerException() {
        List<DailyMetrics> window = new ArrayList<>();
        // 21 older days with null strain
        for (int i = 27; i >= 7; i--) {
            DailyMetrics dm = new DailyMetrics();
            dm.setDate(TODAY.minusDays(i));
            dm.setDailyStrain21(null);
            window.add(dm);
        }
        // 7 recent days with valid strain
        for (int i = 6; i >= 0; i--) {
            window.add(metricsOnDate(TODAY.minusDays(i), 10.0));
        }

        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(window);

        service.updateAcwr(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(70.0,  saved.getAcute7(), 1e-9);
        assertEquals(17.5,  saved.getChronic28(), 1e-9);
        assertEquals(4.0,   saved.getAcwr(), 1e-9);
        assertEquals(AcwrFlag.RED, saved.getAcwrFlag());
    }

    // ── 9. recomputeAcwrForUser calls repository 90 times ────────────────────

    /**
     * recomputeAcwrForUser iterates today-89 through today (inclusive = 90 days).
     * Each call to updateAcwr issues exactly one findByUserIdAndDateBetween query.
     */
    @Test
    void recomputeAcwrForUser_callsRepository90Times() {
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.recomputeAcwrForUser(user);

        verify(dailyMetricsRepository, times(90))
                .findByUserIdAndDateBetween(any(), any(), any());
    }

    // ── 10. Repository queried with correct 28-day date range ─────────────────

    /**
     * updateAcwr must query the 28-day window as [date-27, date] (inclusive).
     */
    @Test
    void updateAcwr_queriesRepositoryWith28DayRange() {
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.updateAcwr(user, TODAY);

        verify(dailyMetricsRepository).findByUserIdAndDateBetween(
                eq(user.getId()),
                eq(TODAY.minusDays(27)),
                eq(TODAY));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DailyMetrics metricsOnDate(LocalDate date, double strain) {
        DailyMetrics dm = new DailyMetrics();
        dm.setDate(date);
        dm.setDailyStrain21(strain);
        return dm;
    }

    /**
     * Builds a list of 28 DailyMetrics where the first 21 days (index 27 down to 7)
     * have {@code strainOld} and the last 7 days (index 6 down to 0) have {@code strainRecent}.
     */
    private List<DailyMetrics> window28(double strainOld, double strainRecent) {
        List<DailyMetrics> list = new ArrayList<>();
        for (int i = 27; i >= 7; i--) {
            list.add(metricsOnDate(TODAY.minusDays(i), strainOld));
        }
        for (int i = 6; i >= 0; i--) {
            list.add(metricsOnDate(TODAY.minusDays(i), strainRecent));
        }
        return list;
    }

    private DailyMetrics captureLastSave() {
        ArgumentCaptor<DailyMetrics> captor = ArgumentCaptor.forClass(DailyMetrics.class);
        verify(dailyMetricsRepository, atLeastOnce()).save(captor.capture());
        List<DailyMetrics> allValues = captor.getAllValues();
        return allValues.get(allValues.size() - 1);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }
}
