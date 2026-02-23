package com.trainingsplan.service;

import com.trainingsplan.service.hrzone.HeartRateZoneConfig;
import com.trainingsplan.service.hrzone.HrZone;
import com.trainingsplan.service.hrzone.ZoneTimeResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calculates time spent in each heart rate zone from a discrete HR stream.
 *
 * <p>Algorithm: for each consecutive pair of samples (i, i+1), the interval
 * Δt = time[i+1] − time[i] is attributed to the zone of HR[i].
 * The last sample contributes no time (no next sample to bound it).
 *
 * <p>This class has no Spring dependencies in its core logic and can be
 * unit-tested without an application context.
 */
@Service
public class ZoneTimeCalculator {

    /**
     * @param timeSeconds  monotonically increasing seconds-since-start for each sample
     * @param heartRates   HR in bpm at each sample point (may contain nulls / zeros)
     * @param config       zone boundary config derived from user's hrMax
     * @return computed result, or {@link ZoneTimeResult#unknown()} if input is insufficient
     */
    public ZoneTimeResult calculate(List<Integer> timeSeconds,
                                    List<Integer> heartRates,
                                    HeartRateZoneConfig config) {
        if (timeSeconds == null || heartRates == null
                || timeSeconds.size() < 2
                || timeSeconds.size() != heartRates.size()) {
            return ZoneTimeResult.unknown();
        }

        int n = timeSeconds.size();
        double[] zoneSec = new double[HrZone.values().length]; // one bucket per zone
        double validDurationSec = 0;

        for (int i = 0; i < n - 1; i++) {
            Integer t0 = timeSeconds.get(i);
            Integer t1 = timeSeconds.get(i + 1);
            if (t0 == null || t1 == null) continue;

            double dt = t1 - t0;
            if (dt <= 0) continue; // skip non-monotonic or duplicate timestamps

            Integer hr = heartRates.get(i);
            if (hr == null || hr <= 0) continue; // missing / invalid HR → skip interval

            validDurationSec += dt;

            HrZone zone = config.zoneFor(hr);
            if (zone != null) {
                zoneSec[zone.ordinal()] += dt;
            }
            // HR below Z1 lower bound: counts toward coverage but not any zone bucket
        }

        double totalDurationSec = computeTotalDuration(timeSeconds);
        double coverage = (totalDurationSec > 0)
                ? Math.min(validDurationSec / totalDurationSec, 1.0)
                : 0.0;

        return new ZoneTimeResult(
                zoneSec[0] / 60.0,
                zoneSec[1] / 60.0,
                zoneSec[2] / 60.0,
                zoneSec[3] / 60.0,
                zoneSec[4] / 60.0,
                coverage
        );
    }

    private double computeTotalDuration(List<Integer> timeSeconds) {
        int n = timeSeconds.size();
        // Walk from each end to find the first/last non-null value
        Integer first = null, last = null;
        for (int i = 0; i < n; i++) {
            if (timeSeconds.get(i) != null) { first = timeSeconds.get(i); break; }
        }
        for (int i = n - 1; i >= 0; i--) {
            if (timeSeconds.get(i) != null) { last = timeSeconds.get(i); break; }
        }
        if (first == null || last == null || last <= first) return 0;
        return last - first;
    }
}
