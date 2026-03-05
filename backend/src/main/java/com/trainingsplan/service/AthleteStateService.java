package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteStateDTO;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AthleteStateService {

    private static final int LONG_RUN_LOOKBACK_DAYS = 42;
    private static final int DEFAULT_LONG_RUN_CAPACITY_MIN = 60;
    private static final int MAX_LONG_RUN_CAPACITY_MIN = 240;

    private final UserRepository userRepository;
    private final DailyMetricsRepository dailyMetricsRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final DailyMetricsService dailyMetricsService;
    private final PaceZoneService paceZoneService;

    public AthleteStateService(
            UserRepository userRepository,
            DailyMetricsRepository dailyMetricsRepository,
            CompletedTrainingRepository completedTrainingRepository,
            DailyMetricsService dailyMetricsService,
            PaceZoneService paceZoneService
    ) {
        this.userRepository = userRepository;
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.dailyMetricsService = dailyMetricsService;
        this.paceZoneService = paceZoneService;
    }

    /**
     * Aggregates athlete state by numeric user id used in the current persistence model.
     */
    public AthleteStateDTO getAthleteState(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        LocalDate today = LocalDate.now();
        dailyMetricsService.computeToday(user);

        List<DailyMetrics> last28Days = dailyMetricsRepository
                .findByUserIdAndDateBetween(user.getId(), today.minusDays(27), today);

        Map<LocalDate, DailyMetrics> dailyByDate = last28Days.stream()
                .collect(Collectors.toMap(DailyMetrics::getDate, Function.identity(), (a, b) -> b));

        DailyMetrics todayMetrics = dailyByDate.get(today);
        double weeklyLoad = sumDailyStrain(today.minusDays(6), today, dailyByDate);
        double todayTrimp = todayMetrics != null && todayMetrics.getDailyTrimp() != null
                ? todayMetrics.getDailyTrimp()
                : 0.0;
        double weeklyTrimp = sumDailyTrimp(today.minusDays(6), today, dailyByDate);
        double avg28Trimp = sumDailyTrimp(today.minusDays(27), today, dailyByDate) / 28.0;
        int fatigueScore = computeFatigueScore(todayMetrics);
        double efficiencyScore = computeEfficiencyScore(todayMetrics);
        int longRunCapacityMinutes = computeLongRunCapacityMinutes(user.getId(), today);

        AthleteStateDTO dto = new AthleteStateDTO();
        dto.setTrimpMetrics(new AthleteStateDTO.TrimpMetricsDTO(todayTrimp, weeklyTrimp, avg28Trimp));
        dto.setFatigueScore(fatigueScore);
        dto.setEfficiencyScore(efficiencyScore);
        dto.setWeeklyLoad(weeklyLoad);
        dto.setLongRunCapacityMinutes(longRunCapacityMinutes);
        dto.setRunningZones(computeRunningZones(user));
        return dto;
    }

    /**
     * Compatibility adapter for callers passing UUID user ids.
     * Current users are keyed by numeric ids; this method accepts UUID input but cannot resolve
     * it without an explicit uuid->user mapping in the data model.
     */
    public AthleteStateDTO getAthleteState(UUID userId) {
        throw new IllegalArgumentException(
                "UUID user IDs are not supported by the current data model. Use numeric user id."
        );
    }

    private double sumDailyStrain(LocalDate from, LocalDate to, Map<LocalDate, DailyMetrics> dailyByDate) {
        double sum = 0.0;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DailyMetrics dm = dailyByDate.get(cursor);
            if (dm != null && dm.getDailyStrain21() != null) {
                sum += dm.getDailyStrain21();
            }
            cursor = cursor.plusDays(1);
        }
        return sum;
    }

    private double sumDailyTrimp(LocalDate from, LocalDate to, Map<LocalDate, DailyMetrics> dailyByDate) {
        double sum = 0.0;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DailyMetrics dm = dailyByDate.get(cursor);
            if (dm != null && dm.getDailyTrimp() != null) {
                sum += dm.getDailyTrimp();
            }
            cursor = cursor.plusDays(1);
        }
        return sum;
    }

    private int computeFatigueScore(DailyMetrics todayMetrics) {
        if (todayMetrics == null || todayMetrics.getReadinessScore() == null) {
            return 50;
        }
        int fatigue = 100 - todayMetrics.getReadinessScore();
        return Math.max(0, Math.min(100, fatigue));
    }

    private double computeEfficiencyScore(DailyMetrics todayMetrics) {
        if (todayMetrics == null) {
            return 0.0;
        }
        if (todayMetrics.getEf7() != null) {
            return todayMetrics.getEf7();
        }
        if (todayMetrics.getEf28() != null) {
            return todayMetrics.getEf28();
        }
        return 0.0;
    }

    private int computeLongRunCapacityMinutes(Long userId, LocalDate today) {
        LocalDate from = today.minusDays(LONG_RUN_LOOKBACK_DAYS - 1L);
        List<CompletedTraining> recentTrainings = completedTrainingRepository
                .findByUserIdAndTrainingDateBetweenOrderByTrainingDate(userId, from, today);

        int maxRunMinutes = recentTrainings.stream()
                .filter(this::isRunningTraining)
                .mapToInt(this::extractDurationMinutes)
                .max()
                .orElse(DEFAULT_LONG_RUN_CAPACITY_MIN);

        return Math.min(maxRunMinutes, MAX_LONG_RUN_CAPACITY_MIN);
    }

    private boolean isRunningTraining(CompletedTraining training) {
        String sport = training.getSport();
        return sport != null && sport.toLowerCase().contains("run");
    }

    private int extractDurationMinutes(CompletedTraining training) {
        Integer movingTime = training.getMovingTimeSeconds();
        if (movingTime != null && movingTime > 0) {
            return Math.max(1, movingTime / 60);
        }
        Integer duration = training.getDurationSeconds();
        if (duration != null && duration > 0) {
            return Math.max(1, duration / 60);
        }
        return 0;
    }

    private List<PaceZoneService.PaceZoneDto> computeRunningZones(User user) {
        Integer thresholdPace = user.getThresholdPaceSecPerKm();
        if (thresholdPace == null || thresholdPace <= 0) {
            return Collections.emptyList();
        }
        return paceZoneService.calculateZones(thresholdPace);
    }
}
