package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteStateDTO;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AthleteStateServiceTest {

    private UserRepository userRepository;
    private DailyMetricsRepository dailyMetricsRepository;
    private CompletedTrainingRepository completedTrainingRepository;
    private DailyMetricsService dailyMetricsService;
    private PaceZoneService paceZoneService;
    private AthleteStateService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        dailyMetricsRepository = mock(DailyMetricsRepository.class);
        completedTrainingRepository = mock(CompletedTrainingRepository.class);
        dailyMetricsService = mock(DailyMetricsService.class);
        paceZoneService = mock(PaceZoneService.class);

        service = new AthleteStateService(
                userRepository,
                dailyMetricsRepository,
                completedTrainingRepository,
                dailyMetricsService,
                paceZoneService
        );
    }

    @Test
    void getAthleteState_aggregatesRequestedSignals() {
        LocalDate today = LocalDate.now();
        User user = new User();
        user.setId(11L);
        user.setThresholdPaceSecPerKm(300);

        DailyMetrics dmToday = new DailyMetrics();
        dmToday.setDate(today);
        dmToday.setDailyStrain21(9.0);
        dmToday.setDailyTrimp(80.0);
        dmToday.setReadinessScore(70);
        dmToday.setEf7(0.023);

        DailyMetrics dmYesterday = new DailyMetrics();
        dmYesterday.setDate(today.minusDays(1));
        dmYesterday.setDailyStrain21(11.0);
        dmYesterday.setDailyTrimp(60.0);

        CompletedTraining run = new CompletedTraining();
        run.setSport("RUN");
        run.setMovingTimeSeconds(7200);

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(dailyMetricsRepository.findByUserIdAndDateBetween(
                eq(11L), eq(today.minusDays(27)), eq(today)))
                .thenReturn(List.of(dmYesterday, dmToday));
        when(completedTrainingRepository.findByUserIdAndTrainingDateBetweenOrderByTrainingDate(
                eq(11L), any(), eq(today)))
                .thenReturn(List.of(run));
        when(paceZoneService.calculateZones(300))
                .thenReturn(List.of(
                        new PaceZoneService.PaceZoneDto(1, "Z1", "desc", null, 360),
                        new PaceZoneService.PaceZoneDto(2, "Z2", "desc", 330, 360)
                ));

        AthleteStateDTO dto = service.getAthleteState(11L);

        verify(dailyMetricsService).computeToday(user);
        assertEquals(20.0, dto.getWeeklyLoad(), 1e-9);
        assertEquals(30, dto.getFatigueScore());
        assertEquals(0.023, dto.getEfficiencyScore(), 1e-9);
        assertEquals(120, dto.getLongRunCapacityMinutes());
        assertEquals(80.0, dto.getTrimpMetrics().getToday(), 1e-9);
        assertEquals(140.0, dto.getTrimpMetrics().getWeeklyTotal(), 1e-9);
        assertEquals(5.0, dto.getTrimpMetrics().getRolling28DayAverage(), 1e-9);
        assertEquals(2, dto.getRunningZones().size());
    }

    @Test
    void getAthleteState_withoutThresholdPace_returnsNoRunningZones() {
        LocalDate today = LocalDate.now();
        User user = new User();
        user.setId(4L);
        user.setThresholdPaceSecPerKm(null);

        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(dailyMetricsRepository.findByUserIdAndDateBetween(
                eq(4L), eq(today.minusDays(27)), eq(today)))
                .thenReturn(List.of());
        when(completedTrainingRepository.findByUserIdAndTrainingDateBetweenOrderByTrainingDate(
                eq(4L), any(), eq(today)))
                .thenReturn(List.of());

        AthleteStateDTO dto = service.getAthleteState(4L);

        assertTrue(dto.getRunningZones().isEmpty());
    }

    @Test
    void getAthleteState_unknownUser_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getAthleteState(99L));
    }
}
