package com.trainingsplan.service;

import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.DailyMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Computes Acute:Chronic Workload Ratio (ACWR) from daily strain21 aggregates.
 *
 * <p>Definitions (stored in {@code daily_metrics}):
 * <ul>
 *   <li>{@code acute7}   = sum of daily_strain21 for the 7 days ending on {@code date} (inclusive)</li>
 *   <li>{@code chronic28} = sum of daily_strain21 for the 28 days ending on {@code date} / 4
 *       — equivalent to avg(28d) × 7, placing it on the same scale as {@code acute7}</li>
 *   <li>{@code acwr}     = acute7 / chronic28  (null when chronic28 = 0)</li>
 * </ul>
 *
 * <p>Days without a {@code daily_metrics} record contribute zero load.
 *
 * <p>Flag thresholds:
 * <ul>
 *   <li>BLUE   – ACWR &lt; 0.8  (under-training)</li>
 *   <li>GREEN  – 0.8 ≤ ACWR ≤ 1.3  (optimal zone)</li>
 *   <li>ORANGE – 1.3 &lt; ACWR ≤ 1.6  (elevated risk)</li>
 *   <li>RED    – ACWR &gt; 1.6  (high injury risk)</li>
 * </ul>
 */
@Service
public class LoadModelService {

    @Autowired
    private DailyMetricsRepository dailyMetricsRepository;

    /**
     * Recomputes and persists ACWR metrics for {@code user} on {@code date}.
     * Reads the 28-day rolling window of {@code daily_strain21} from the database.
     * Must be called AFTER {@link DailyMetricsService#updateDailyStrain} has persisted
     * today's strain so the window is current.
     */
    public void updateAcwr(User user, LocalDate date) {
        List<DailyMetrics> window28 = dailyMetricsRepository
                .findByUserIdAndDateBetween(user.getId(), date.minusDays(27), date);

        double acute7    = sumStrain(window28, date.minusDays(6), date);
        double sum28     = sumStrain(window28, date.minusDays(27), date);
        double chronic28 = sum28 / 4.0;

        Double acwr     = chronic28 > 0.0 ? acute7 / chronic28 : null;
        AcwrFlag flag   = computeFlag(acwr);
        String message  = computeMessage(flag);

        DailyMetrics daily = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date)
                .orElse(new DailyMetrics());

        daily.setUser(user);
        daily.setDate(date);
        daily.setAcute7(acute7);
        daily.setChronic28(chronic28);
        daily.setAcwr(acwr);
        daily.setAcwrFlag(flag);
        daily.setAcwrMessage(message);
        dailyMetricsRepository.save(daily);
    }

    /**
     * Recomputes ACWR for the last 90 days (today−89 through today) for the given user.
     * Useful for backfilling when the feature is first deployed.
     */
    public void recomputeAcwrForUser(User user) {
        LocalDate today = LocalDate.now();
        for (int i = 89; i >= 0; i--) {
            updateAcwr(user, today.minusDays(i));
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /** Sums {@code dailyStrain21} for all records whose date is within [{@code from}, {@code to}]. */
    private double sumStrain(List<DailyMetrics> records, LocalDate from, LocalDate to) {
        double sum = 0.0;
        for (DailyMetrics dm : records) {
            if (!dm.getDate().isBefore(from) && !dm.getDate().isAfter(to)
                    && dm.getDailyStrain21() != null) {
                sum += dm.getDailyStrain21();
            }
        }
        return sum;
    }

    private AcwrFlag computeFlag(Double acwr) {
        if (acwr == null)  return null;
        if (acwr < 0.8)    return AcwrFlag.BLUE;
        if (acwr <= 1.3)   return AcwrFlag.GREEN;
        if (acwr <= 1.6)   return AcwrFlag.ORANGE;
        return AcwrFlag.RED;
    }

    private String computeMessage(AcwrFlag flag) {
        if (flag == null) return null;
        return switch (flag) {
            case BLUE   -> "Unterbelastung – Training steigern";
            case GREEN  -> "Optimale Belastung";
            case ORANGE -> "Erhöhte Belastung – Verletzungsrisiko beachten";
            case RED    -> "Hohes Verletzungsrisiko – Belastung reduzieren";
        };
    }
}
