package com.trainingsplan.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates running pace zones (Tempobereiche) based on a reference race performance.
 *
 * <p>Zone boundaries are defined as multipliers of the lactate threshold pace (T-pace),
 * consistent with the Strava/Garmin approach. T-pace is derived from the reference race
 * using Jack Daniels' VDOT formula at 88% of VO2max.
 *
 * <p>Zone multipliers relative to threshold pace (higher multiplier = slower pace):
 * <ul>
 *   <li>Z1 Regeneration: &gt; 1.29 × T-pace</li>
 *   <li>Z2 Ausdauer:     1.14 – 1.29 × T-pace</li>
 *   <li>Z3 Tempo:        1.06 – 1.14 × T-pace</li>
 *   <li>Z4 Schwelle:     0.99 – 1.06 × T-pace</li>
 *   <li>Z5 VO2max:       &lt; 0.99 × T-pace</li>
 * </ul>
 */
@Service
public class PaceZoneService {

    private static final String[] ZONE_NAMES = {
        "Regeneration", "Ausdauer", "Tempo", "Schwelle", "VO2max", "Anaerob"
    };

    private static final String[] ZONE_DESCRIPTIONS = {
        "Sehr entspanntes Laufen. Vor oder nach hartem Training.",
        "Gemütliches Laufen (Konversationstempo). Großteil der Laufleistung.",
        "Gleichmäßiges Tempo – oft Marathontempo oder etwas schneller.",
        "Tempo, das bis zu 60 Minuten gehalten werden kann (Schwellentempo).",
        "Sehr hartes Tempo zur Verbesserung der maximalen Sauerstoffaufnahme.",
        "Maximale Kurzzeitbelastung. Kurze Intervalle und Sprints über der VO2max-Kapazität."
    };

    /**
     * Zone boundaries as [fastestMultiplier, slowestMultiplier] relative to threshold pace.
     * Higher multiplier = slower pace (more sec/km). null = no bound.
     */
    private static final Double[] ZONE_FASTEST_MULT = { 1.29, 1.14, 1.06, 0.99, 0.90, null };
    private static final Double[] ZONE_SLOWEST_MULT = { null, 1.29, 1.14, 1.06, 0.99, 0.90 };

    /**
     * Calculates the lactate threshold pace (T-pace) from a reference race performance.
     * Uses the Daniels/Gilbert VDOT model: T-pace = pace at 88% of VO2max.
     *
     * @param distanceM   reference race distance in meters
     * @param timeSeconds reference race time in seconds
     * @return threshold pace in seconds per km
     */
    public int calculateThresholdPace(double distanceM, int timeSeconds) {
        double timeMinutes = timeSeconds / 60.0;
        double speedMPerMin = distanceM / timeMinutes;

        // Daniels VO2 demand at race pace
        double vo2AtPace = -4.60 + 0.182258 * speedMPerMin + 0.000104 * speedMPerMin * speedMPerMin;

        // Daniels %VO2max fraction at race duration
        double vo2Fraction = 0.8
                + 0.1894393 * Math.exp(-0.012778 * timeMinutes)
                + 0.2989558 * Math.exp(-0.1932605 * timeMinutes);

        double vo2max = vo2AtPace / vo2Fraction;

        // T-pace = pace at 88% VO2max (Jack Daniels definition)
        double targetVo2 = 0.88 * vo2max;

        // Invert the quadratic: 0.000104*v² + 0.182258*v + (-4.60 - targetVo2) = 0
        double a = 0.000104;
        double b = 0.182258;
        double c = -4.60 - targetVo2;
        double discriminant = b * b - 4 * a * c;
        double thresholdSpeedMPerMin = (-b + Math.sqrt(discriminant)) / (2 * a);

        // Convert m/min → sec/km
        return (int) Math.round(60_000.0 / thresholdSpeedMPerMin);
    }

    /**
     * Returns the five pace zones calculated from the given threshold pace.
     *
     * @param thresholdPaceSecPerKm lactate threshold pace in seconds per km
     * @return list of 5 {@link PaceZoneDto}, sorted from slowest (Z1) to fastest (Z5)
     */
    public List<PaceZoneDto> calculateZones(int thresholdPaceSecPerKm) {
        List<PaceZoneDto> zones = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Double fastestMult = ZONE_FASTEST_MULT[i];
            Double slowestMult = ZONE_SLOWEST_MULT[i];

            // In pace (sec/km): higher value = slower. Zone's "fastest" boundary has lowest sec/km.
            Integer fastestPace = (fastestMult != null)
                    ? (int) Math.round(fastestMult * thresholdPaceSecPerKm) : null;
            Integer slowestPace = (slowestMult != null)
                    ? (int) Math.round(slowestMult * thresholdPaceSecPerKm) : null;

            zones.add(new PaceZoneDto(i + 1, ZONE_NAMES[i], ZONE_DESCRIPTIONS[i], fastestPace, slowestPace));
        }
        return zones;
    }

    /**
     * Immutable representation of a single pace zone.
     *
     * @param zone                  zone number 1–5 (1 = slowest/recovery, 5 = fastest/VO2max)
     * @param name                  German zone name
     * @param description           short description of the zone's purpose
     * @param fastestPaceSecPerKm   fastest (lower) boundary in sec/km; null for Z5 (no limit)
     * @param slowestPaceSecPerKm   slowest (upper) boundary in sec/km; null for Z1 (no limit)
     */
    public record PaceZoneDto(
            int zone,
            String name,
            String description,
            Integer fastestPaceSecPerKm,
            Integer slowestPaceSecPerKm
    ) {}

    /**
     * Full pace zone configuration including reference performance and all five zones.
     */
    public record PaceZonesDto(
            Double referenceDistanceM,
            Integer referenceTimeSeconds,
            String referenceLabel,
            int thresholdPaceSecPerKm,
            List<PaceZoneDto> zones
    ) {}
}
