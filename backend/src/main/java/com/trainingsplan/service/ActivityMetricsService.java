package com.trainingsplan.service;

import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.service.hrzone.HeartRateZoneConfig;
import com.trainingsplan.service.hrzone.ZoneTimeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates HR zone calculation and persists results in {@code activity_metrics}.
 * Called once per FIT file upload (after the CompletedTraining has been saved).
 */
@Service
public class ActivityMetricsService {

    @Autowired
    private ZoneTimeCalculator zoneTimeCalculator;

    @Autowired
    private StrainCalculator strainCalculator;

    @Autowired
    private DailyMetricsService dailyMetricsService;

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    /**
     * Computes HR zone times from the collected stream data and persists the result.
     * Upserts: if a record for the same activity already exists it is overwritten.
     *
     * @param completedTraining the already-persisted activity
     * @param timeSeconds       relative time stream (seconds since activity start)
     * @param heartRates        HR stream in bpm (parallel to timeSeconds; may contain nulls)
     * @param user              the athlete (supplies hrMax for zone boundaries)
     */
    public void calculateAndPersist(CompletedTraining completedTraining,
                                    List<Integer> timeSeconds,
                                    List<Integer> heartRates,
                                    User user) {
        ActivityMetrics metrics = activityMetricsRepository
                .findByCompletedTrainingId(completedTraining.getId())
                .orElse(new ActivityMetrics());

        metrics.setCompletedTraining(completedTraining);

        if (user.getMaxHeartRate() == null || user.getMaxHeartRate() <= 0) {
            // No hrMax configured → cannot compute zones or strain
            metrics.setZonesUnknown(true);
            activityMetricsRepository.save(metrics);
            return;
        }

        HeartRateZoneConfig config = HeartRateZoneConfig.fromHrMax(user.getMaxHeartRate());
        ZoneTimeResult result = zoneTimeCalculator.calculate(timeSeconds, heartRates, config);

        if (result.isUnknown()) {
            metrics.setZonesUnknown(true);
        } else {
            metrics.setZonesUnknown(false);
            metrics.setZ1Min(result.getZ1Min());
            metrics.setZ2Min(result.getZ2Min());
            metrics.setZ3Min(result.getZ3Min());
            metrics.setZ4Min(result.getZ4Min());
            metrics.setZ5Min(result.getZ5Min());
            metrics.setHrDataCoverage(result.getHrDataCoverage());

            double rawLoad = strainCalculator.rawLoad(
                    result.getZ1Min(), result.getZ2Min(), result.getZ3Min(),
                    result.getZ4Min(), result.getZ5Min());
            metrics.setRawLoad(rawLoad);
            metrics.setStrain21(strainCalculator.strain21(rawLoad));
        }

        activityMetricsRepository.save(metrics);
        dailyMetricsService.updateDailyStrain(user, completedTraining.getTrainingDate());
    }
}
