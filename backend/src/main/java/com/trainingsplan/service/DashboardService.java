package com.trainingsplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.DashboardDto;
import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.Recommendation;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final int DAYS = 28;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final DailyMetricsRepository dailyMetricsRepository;
    private final ActivityMetricsRepository activityMetricsRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final DailyMetricsService dailyMetricsService;
    private final ObjectMapper objectMapper;

    public DashboardService(
            DailyMetricsRepository dailyMetricsRepository,
            ActivityMetricsRepository activityMetricsRepository,
            CompletedTrainingRepository completedTrainingRepository,
            DailyMetricsService dailyMetricsService,
            ObjectMapper objectMapper
    ) {
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.activityMetricsRepository = activityMetricsRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.dailyMetricsService = dailyMetricsService;
        this.objectMapper = objectMapper;
    }

    public DashboardDto getDashboard(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(DAYS - 1L);

        dailyMetricsService.computeToday(user);

        List<DailyMetrics> dailyMetrics = dailyMetricsRepository
                .findByUserIdAndDateBetween(user.getId(), startDate, today);

        Map<LocalDate, DailyMetrics> dailyByDate = new HashMap<>();
        for (DailyMetrics daily : dailyMetrics) {
            dailyByDate.put(daily.getDate(), daily);
        }

        DailyMetrics latestWithStrain = dailyMetrics.stream()
                .filter(d -> d.getDailyStrain21() != null)
                .max(Comparator.comparing(DailyMetrics::getDate))
                .orElse(null);

        DailyMetrics latestWithReadiness = dailyMetrics.stream()
                .filter(d -> d.getReadinessScore() != null)
                .max(Comparator.comparing(DailyMetrics::getDate))
                .orElse(null);

        DailyMetrics latestWithAcwr = dailyMetrics.stream()
                .filter(d -> d.getAcwr() != null)
                .max(Comparator.comparing(DailyMetrics::getDate))
                .orElse(null);

        List<DashboardDto.LoadTrendPointDto> loadTrend = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            DailyMetrics daily = dailyByDate.get(date);
            double strain = daily != null && daily.getDailyStrain21() != null ? daily.getDailyStrain21() : 0.0;
            loadTrend.add(new DashboardDto.LoadTrendPointDto(date, strain));
        }

        List<ActivityMetrics> efMetrics = activityMetricsRepository
                .findWithEfByUserIdAndDateRange(user.getId(), startDate, today);
        Map<LocalDate, List<Double>> efByDate = new HashMap<>();
        for (ActivityMetrics metrics : efMetrics) {
            if (metrics.getEfficiencyFactor() == null || metrics.getCompletedTraining() == null) {
                continue;
            }
            LocalDate date = metrics.getCompletedTraining().getTrainingDate();
            efByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(metrics.getEfficiencyFactor());
        }

        List<DashboardDto.EfTrendPointDto> efTrend = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            efTrend.add(new DashboardDto.EfTrendPointDto(date, average(efByDate.get(date))));
        }

        List<ActivityMetrics> driftMetrics = activityMetricsRepository
                .findEligibleDecouplingByUserIdAndDateRange(user.getId(), startDate, today);
        Map<LocalDate, List<Double>> driftByDate = new HashMap<>();
        for (ActivityMetrics metrics : driftMetrics) {
            if (metrics.getDecouplingPct() == null || metrics.getCompletedTraining() == null) {
                continue;
            }
            LocalDate date = metrics.getCompletedTraining().getTrainingDate();
            driftByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(metrics.getDecouplingPct());
        }

        List<DashboardDto.DriftTrendPointDto> driftTrend = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            driftTrend.add(new DashboardDto.DriftTrendPointDto(date, average(driftByDate.get(date))));
        }

        CompletedTraining lastCompleted = completedTrainingRepository
                .findTopByUserIdAndSportContainingIgnoreCaseOrderByTrainingDateDescUploadDateDesc(user.getId(), "run")
                .orElseGet(() -> completedTrainingRepository
                        .findTopByUserIdOrderByTrainingDateDescUploadDateDesc(user.getId())
                        .orElse(null));

        DashboardDto.LastRunDto lastRun = buildLastRun(lastCompleted, dailyByDate);

        double strain21 = latestWithStrain != null && latestWithStrain.getDailyStrain21() != null
                ? latestWithStrain.getDailyStrain21()
                : 0.0;

        int readinessScore = latestWithReadiness != null && latestWithReadiness.getReadinessScore() != null
                ? latestWithReadiness.getReadinessScore()
                : 0;

        Recommendation recommendation = latestWithReadiness != null
                ? latestWithReadiness.getRecommendation()
                : null;

        String readinessRecommendation = recommendation != null ? recommendation.name() : "EASY";

        double acwr = latestWithAcwr != null && latestWithAcwr.getAcwr() != null
                ? latestWithAcwr.getAcwr()
                : 0.0;

        AcwrFlag acwrFlag = latestWithAcwr != null ? latestWithAcwr.getAcwrFlag() : null;

        DashboardDto.LoadStatusDto loadStatus = new DashboardDto.LoadStatusDto(
                acwr,
                acwrFlag != null ? acwrFlag.name() : "BLUE"
        );

        return new DashboardDto(
                strain21,
                readinessScore,
                readinessRecommendation,
                loadStatus,
                loadTrend,
                efTrend,
                driftTrend,
                lastRun
        );
    }

    private DashboardDto.LastRunDto buildLastRun(CompletedTraining completedTraining, Map<LocalDate, DailyMetrics> dailyByDate) {
        if (completedTraining == null) {
            return new DashboardDto.LastRunDto(LocalDate.now(), 0.0, 0.0, 0.0, 0.0, List.of());
        }

        ActivityMetrics activityMetrics = activityMetricsRepository
                .findByCompletedTrainingId(completedTraining.getId())
                .orElse(null);

        double strain = activityMetrics != null && activityMetrics.getStrain21() != null
                ? activityMetrics.getStrain21()
                : 0.0;

        double driftPct = activityMetrics != null && activityMetrics.getDecouplingPct() != null
                ? activityMetrics.getDecouplingPct()
                : 0.0;

        double z4Min = activityMetrics != null && activityMetrics.getZ4Min() != null
                ? activityMetrics.getZ4Min()
                : zoneMinutesFromSeconds(completedTraining.getTimeInHrZone4Seconds());

        double z5Min = activityMetrics != null && activityMetrics.getZ5Min() != null
                ? activityMetrics.getZ5Min()
                : zoneMinutesFromSeconds(completedTraining.getTimeInHrZone5Seconds());

        List<String> coachBullets = List.of();
        DailyMetrics daily = dailyByDate.get(completedTraining.getTrainingDate());
        if (daily != null && daily.getCoachBulletsJson() != null && !daily.getCoachBulletsJson().isBlank()) {
            try {
                coachBullets = objectMapper.readValue(daily.getCoachBulletsJson(), STRING_LIST);
            } catch (Exception ignored) {
                coachBullets = List.of();
            }
        }

        return new DashboardDto.LastRunDto(
                completedTraining.getTrainingDate(),
                strain,
                driftPct,
                z4Min,
                z5Min,
                coachBullets
        );
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (Double value : values) {
            if (value != null) {
                sum += value;
            }
        }
        return sum / values.size();
    }

    private double zoneMinutesFromSeconds(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return 0.0;
        }
        return seconds / 60.0;
    }
}
