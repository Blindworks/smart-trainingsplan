package com.trainingsplan.service;

import com.trainingsplan.dto.StravaActivityDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

@Service
public class Vo2MaxService {

    /**
     * Estimates running VO2max from one activity using the Daniels/Gilbert VDOT-based approach.
     * Expects distance in meters and moving time in seconds.
     */
    public Optional<Double> calculateFromActivity(StravaActivityDto activity) {
        if (activity == null) {
            return Optional.empty();
        }
        if (!isRunningActivity(activity)) {
            return Optional.empty();
        }
        return calculate(activity.getDistanceMeters(), activity.getMovingTimeSeconds());
    }

    /**
     * Estimates VO2max from a distance-time effort (Daniels/Gilbert VDOT approach).
     *
     * @param distanceMeters    distance in meters
     * @param movingTimeSeconds moving time in seconds
     */
    public Optional<Double> calculate(Double distanceMeters, Integer movingTimeSeconds) {
        if (distanceMeters == null || movingTimeSeconds == null) {
            return Optional.empty();
        }
        if (distanceMeters <= 0 || movingTimeSeconds <= 0) {
            return Optional.empty();
        }

        double timeMinutes = movingTimeSeconds / 60.0;
        double speedMetersPerMinute = distanceMeters / timeMinutes;

        double vo2AtRacePace = vo2AtPace(speedMetersPerMinute);
        double vo2Fraction = 0.8
                + (0.1894393 * Math.exp(-0.012778 * timeMinutes))
                + (0.2989558 * Math.exp(-0.1932605 * timeMinutes));

        if (vo2Fraction <= 0.0) {
            return Optional.empty();
        }

        double vo2Max = vo2AtRacePace / vo2Fraction;
        if (!Double.isFinite(vo2Max) || vo2Max <= 0.0) {
            return Optional.empty();
        }

        return Optional.of(round(vo2Max, 2));
    }

    /**
     * HR-corrected VO2max using the %HRmax → %vVO2max conversion.
     *
     * Formula: VO2max = vo2AtPace / %vVO2max
     * Conversion: %vVO2max = %HRmax − 5 pp  (common physiological approximation)
     *
     * Rationale: at a given pace the body demands a fixed VO2 per kg per minute
     * (vo2AtPace). The fraction of VO2max actually used (%vVO2max) is approximately
     * 5 percentage points below the observed %HRmax. Dividing by %vVO2max yields
     * the estimated VO2max.
     *
     * @param distanceMeters    distance in meters
     * @param movingTimeSeconds moving time in seconds (actual running time, excluding stops)
     * @param avgHeartRate      average heart rate during the effort (bpm)
     * @param maxHeartRate      athlete's maximum heart rate (bpm)
     */
    public Optional<Double> calculateHRCorrected(Double distanceMeters, Integer movingTimeSeconds,
                                                  Integer avgHeartRate, Integer maxHeartRate) {
        if (distanceMeters == null || movingTimeSeconds == null
                || avgHeartRate == null || maxHeartRate == null) {
            return Optional.empty();
        }
        if (distanceMeters <= 0 || movingTimeSeconds <= 0
                || avgHeartRate <= 0 || maxHeartRate <= 0 || avgHeartRate > maxHeartRate) {
            return Optional.empty();
        }

        double timeMinutes = movingTimeSeconds / 60.0;
        double speedMetersPerMinute = distanceMeters / timeMinutes;

        double vo2AtRacePace = vo2AtPace(speedMetersPerMinute);
        double hrFraction = (double) avgHeartRate / maxHeartRate;

        // Convert %HRmax to %vVO2max using the common −5 pp approximation
        double vVO2maxFraction = hrFraction - 0.05;
        if (vVO2maxFraction <= 0.0) {
            return Optional.empty();
        }

        double vo2Max = vo2AtRacePace / vVO2maxFraction;
        if (!Double.isFinite(vo2Max) || vo2Max <= 0.0) {
            return Optional.empty();
        }

        return Optional.of(round(vo2Max, 2));
    }

    private double vo2AtPace(double speedMetersPerMinute) {
        return -4.60 + (0.182258 * speedMetersPerMinute) + (0.000104 * speedMetersPerMinute * speedMetersPerMinute);
    }

    private boolean isRunningActivity(StravaActivityDto activity) {
        String type = normalize(activity.getType());
        String sportType = normalize(activity.getSportType());
        return "run".equals(type) || "run".equals(sportType);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
