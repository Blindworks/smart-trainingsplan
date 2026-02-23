package com.trainingsplan.service;

import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Maintains aggregated daily strain in {@code daily_metrics}.
 * Recomputes the total by summing all strain21 values for the day,
 * ensuring idempotency (safe on re-import of the same activity).
 */
@Service
public class DailyMetricsService {

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    @Autowired
    private DailyMetricsRepository dailyMetricsRepository;

    /**
     * Recomputes and upserts the daily strain21 and TRIMP aggregates for {@code user} on {@code date}.
     */
    public void updateDailyStrain(User user, LocalDate date) {
        Double totalStrain = activityMetricsRepository.sumStrain21ByUserIdAndDate(user.getId(), date);
        if (totalStrain == null) {
            totalStrain = 0.0;
        }

        Double totalTrimp = activityMetricsRepository.sumTrimpByUserIdAndDate(user.getId(), date);

        DailyMetrics daily = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date)
                .orElse(new DailyMetrics());

        daily.setUser(user);
        daily.setDate(date);
        daily.setDailyStrain21(totalStrain);
        daily.setDailyTrimp(totalTrimp);
        dailyMetricsRepository.save(daily);
    }
}
