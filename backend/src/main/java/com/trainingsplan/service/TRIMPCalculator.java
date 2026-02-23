package com.trainingsplan.service;

import com.trainingsplan.service.trimp.TRIMPQuality;
import com.trainingsplan.service.trimp.TRIMPResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes Bannister TRIMP from a discrete HR stream.
 *
 * <p>Formula per interval i:
 * <pre>
 *   ΔHR      = clamp((HR[i] − hrRest) / (hrMax − hrRest), 0, 1)
 *   TRIMP    += Δt_min × ΔHR × exp(k × ΔHR)
 * </pre>
 * where k = 1.92 (male / unknown) or 1.67 (female).
 *
 * <p>The sampling interval is handled correctly for irregular streams
 * via Δt = time[i+1] − time[i].
 */
@Service
public class TRIMPCalculator {

    /** Bannister k for male athletes (default when gender unknown). */
    public static final double K_MALE = 1.92;
    /** Bannister k for female athletes. */
    public static final double K_FEMALE = 1.67;

    /** HR-coverage threshold below which quality is rated {@link TRIMPQuality#LOW}. */
    private static final double LOW_QUALITY_THRESHOLD = 0.6;

    /**
     * Selects the Bannister k coefficient based on a stored gender string.
     *
     * @param gender "FEMALE" → 1.67; anything else (including null) → 1.92
     */
    public static double kForGender(String gender) {
        return "FEMALE".equalsIgnoreCase(gender) ? K_FEMALE : K_MALE;
    }

    /**
     * Computes TRIMP from parallel time and HR streams.
     *
     * @param timeSeconds monotonically increasing seconds-since-start for each sample
     * @param heartRates  HR in bpm at each sample (may contain nulls / zeros)
     * @param hrRest      resting heart rate in bpm (must be &gt; 0)
     * @param hrMax       maximum heart rate in bpm (must be &gt; hrRest)
     * @param k           Bannister sex coefficient (see {@link #K_MALE} / {@link #K_FEMALE})
     * @return computed result, or {@code null} if the input is insufficient
     */
    public TRIMPResult calculate(List<Integer> timeSeconds,
                                 List<Integer> heartRates,
                                 int hrRest,
                                 int hrMax,
                                 double k) {
        if (timeSeconds == null || heartRates == null
                || timeSeconds.size() < 2
                || timeSeconds.size() != heartRates.size()) {
            return null;
        }

        double hrRange = hrMax - hrRest;
        if (hrRange <= 0) {
            return null; // invalid config — hrRest must be < hrMax
        }

        int n = timeSeconds.size();
        double trimp = 0.0;
        double validDurationSec = 0.0;

        for (int i = 0; i < n - 1; i++) {
            Integer t0 = timeSeconds.get(i);
            Integer t1 = timeSeconds.get(i + 1);
            if (t0 == null || t1 == null) continue;

            double dt = t1 - t0; // seconds
            if (dt <= 0) continue;

            Integer hr = heartRates.get(i);
            if (hr == null || hr <= 0) continue;

            validDurationSec += dt;

            double dtMin = dt / 60.0;
            double deltaHR = (hr - hrRest) / hrRange;
            deltaHR = Math.max(0.0, Math.min(1.0, deltaHR)); // clamp [0, 1]

            trimp += dtMin * deltaHR * Math.exp(k * deltaHR);
        }

        double totalDurationSec = computeTotalDuration(timeSeconds);
        double coverage = (totalDurationSec > 0)
                ? Math.min(validDurationSec / totalDurationSec, 1.0)
                : 0.0;

        TRIMPQuality quality = (coverage < LOW_QUALITY_THRESHOLD)
                ? TRIMPQuality.LOW
                : TRIMPQuality.OK;

        return new TRIMPResult(trimp, quality, coverage);
    }

    private double computeTotalDuration(List<Integer> timeSeconds) {
        int n = timeSeconds.size();
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
