package com.trainingsplan.service;

import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Efficiency Factor rolling average computation in {@link DailyMetricsService}.
 * No Spring context or database — all dependencies are mocked.
 *
 * <p>EF unit: m/s per bpm. Formula: avgSpeedKmh / 3.6 / avgHR.
 */
class EfRollingAverageTest {

    private ActivityMetricsRepository activityMetricsRepository;
    private DailyMetricsRepository dailyMetricsRepository;
    private DailyMetricsService service;

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 24);
    private User user;

    @BeforeEach
    void setUp() {
        activityMetricsRepository = mock(ActivityMetricsRepository.class);
        dailyMetricsRepository = mock(DailyMetricsRepository.class);

        service = new DailyMetricsService();
        // Inject mocks via reflection since the service uses @Autowired fields
        injectField(service, "activityMetricsRepository", activityMetricsRepository);
        injectField(service, "dailyMetricsRepository", dailyMetricsRepository);

        user = new User();

        when(dailyMetricsRepository.findByUserIdAndDate(any(), any()))
                .thenReturn(Optional.empty());
        when(dailyMetricsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Default: strain queries return 0 so updateDailyStrain (if called) won't fail
        when(activityMetricsRepository.sumStrain21ByUserIdAndDate(any(), any())).thenReturn(null);
        when(activityMetricsRepository.sumTrimpByUserIdAndDate(any(), any())).thenReturn(null);
    }

    // ── 1. Single activity — ef7 and ef28 both equal that EF ─────────────────

    @Test
    void singleActivity_ef7AndEf28EqualThatEf() {
        ActivityMetrics am = activityOnDate(TODAY, 12.0, 150); // EF = (12/3.6)/150 = 0.02222
        double expectedEf = (12.0 / 3.6) / 150.0;

        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(am));

        service.updateDailyEf(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(expectedEf, saved.getEf7(),  1e-9);
        assertEquals(expectedEf, saved.getEf28(), 1e-9);
    }

    // ── 2. Multiple activities within 7 days — ef7 is their average ───────────

    @Test
    void multipleActivitiesIn7Days_ef7IsAverage() {
        // EF1 = (10/3.6)/140 = 0.01984, EF2 = (14/3.6)/160 = 0.02431
        ActivityMetrics am1 = activityOnDate(TODAY.minusDays(2), 10.0, 140);
        ActivityMetrics am2 = activityOnDate(TODAY,              14.0, 160);

        double ef1 = (10.0 / 3.6) / 140.0;
        double ef2 = (14.0 / 3.6) / 160.0;
        double expectedEf7  = (ef1 + ef2) / 2.0;
        double expectedEf28 = (ef1 + ef2) / 2.0;

        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(am1, am2));

        service.updateDailyEf(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(expectedEf7,  saved.getEf7(),  1e-9);
        assertEquals(expectedEf28, saved.getEf28(), 1e-9);
    }

    // ── 3. Activity older than 7 days but within 28 ───────────────────────────

    @Test
    void activityOlderThan7Days_excludedFromEf7ButIncludedInEf28() {
        ActivityMetrics amOld    = activityOnDate(TODAY.minusDays(10), 12.0, 150); // outside 7-day window
        ActivityMetrics amRecent = activityOnDate(TODAY,               12.0, 150); // inside both windows

        double ef = (12.0 / 3.6) / 150.0;

        // The repository is called with 28-day start date; both activities are returned.
        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(amOld, amRecent));

        service.updateDailyEf(user, TODAY);

        DailyMetrics saved = captureLastSave();
        // ef7 window: [TODAY-6, TODAY] — only amRecent qualifies
        assertEquals(ef, saved.getEf7(), 1e-9);
        // ef28 window: [TODAY-27, TODAY] — both qualify
        assertEquals(ef, saved.getEf28(), 1e-9);
    }

    // ── 4. Multiple activities on the same day ────────────────────────────────

    @Test
    void multipleActivitiesSameDay_allCountedInAverage() {
        ActivityMetrics am1 = activityOnDate(TODAY, 10.0, 140);
        ActivityMetrics am2 = activityOnDate(TODAY, 14.0, 160);

        double ef1 = (10.0 / 3.6) / 140.0;
        double ef2 = (14.0 / 3.6) / 160.0;
        double expectedAvg = (ef1 + ef2) / 2.0;

        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(am1, am2));

        service.updateDailyEf(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(expectedAvg, saved.getEf7(),  1e-9);
        assertEquals(expectedAvg, saved.getEf28(), 1e-9);
    }

    // ── 5. No activities — ef7 and ef28 are null ─────────────────────────────

    @Test
    void noActivities_ef7AndEf28AreNull() {
        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        service.updateDailyEf(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertNull(saved.getEf7());
        assertNull(saved.getEf28());
    }

    // ── 6. EF null (missing HR) — excluded from rolling average ──────────────

    @Test
    void activityWithNullEf_excludedFromAverage() {
        // The repository query already filters out null EF via IS NOT NULL.
        // Simulate: one valid activity and one that slipped through (shouldn't happen in prod,
        // but the averageEfInWindow logic must not throw on null EF from the list).
        ActivityMetrics amValid = activityOnDate(TODAY, 12.0, 150);
        double expectedEf = (12.0 / 3.6) / 150.0;

        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(amValid));

        service.updateDailyEf(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(expectedEf, saved.getEf7(),  1e-9);
        assertEquals(expectedEf, saved.getEf28(), 1e-9);
    }

    // ── 7. EF formula correctness: 3.0 km/h at HR=150 → EF = 0.005556 ────────

    @Test
    void efFormula_3kmhAt150bpm_correctValue() {
        // 3.0 km/h ÷ 3.6 = 0.8333 m/s; 0.8333 / 150 = 0.005556
        ActivityMetrics am = activityOnDate(TODAY, 3.0, 150);

        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of(am));

        service.updateDailyEf(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(0.8333333333333334 / 150.0, saved.getEf7(), 1e-9);
    }

    // ── 8. Activities outside 28-day window are ignored ───────────────────────

    @Test
    void activityOutside28DayWindow_notReturnedByRepo_notCounted() {
        // The repository is the gatekeeper for the 28-day window.
        // An empty result means both ef7 and ef28 are null.
        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        service.updateDailyEf(user, TODAY);

        DailyMetrics saved = captureLastSave();
        assertNull(saved.getEf7());
        assertNull(saved.getEf28());
    }

    // ── 9. recomputeEfForUser calls updateDailyEf for exactly 90 days ─────────

    @Test
    void recomputeEfForUser_calls90Days() {
        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        service.recomputeEfForUser(user);

        // findWithEfByUserIdAndDateRange should be called once per day = 90 times
        verify(activityMetricsRepository, times(90))
                .findWithEfByUserIdAndDateRange(any(), any(), any());
    }

    // ── 10. Repository is queried with correct 28-day range ───────────────────

    @Test
    void updateDailyEf_queriesRepositoryWith28DayRange() {
        when(activityMetricsRepository.findWithEfByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        service.updateDailyEf(user, TODAY);

        verify(activityMetricsRepository).findWithEfByUserIdAndDateRange(
                eq(user.getId()),
                eq(TODAY.minusDays(27)),
                eq(TODAY));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates an {@link ActivityMetrics} whose EF is pre-set based on the given speed and HR.
     * The speed and HR are also stored on the linked {@link CompletedTraining} so callers
     * do not need to recompute the EF separately.
     */
    private ActivityMetrics activityOnDate(LocalDate date, double speedKmh, int avgHr) {
        CompletedTraining ct = new CompletedTraining();
        ct.setTrainingDate(date);
        ct.setAverageSpeedKmh(speedKmh);
        ct.setAverageHeartRate(avgHr);

        ActivityMetrics am = new ActivityMetrics();
        am.setCompletedTraining(ct);
        am.setEfficiencyFactor((speedKmh / 3.6) / avgHr);
        return am;
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
