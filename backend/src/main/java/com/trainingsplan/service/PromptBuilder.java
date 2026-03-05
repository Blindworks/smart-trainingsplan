package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.Workout;
import com.trainingsplan.service.PaceZoneService.PaceZoneDto;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds the final LLM prompt for weekly PACR training plan generation.
 *
 * <p>Loads {@code classpath:prompts/weekly_training_plan_prompt.txt} once at startup
 * and replaces the following placeholders at call time:
 * <ul>
 *   <li>{@code {{goal}}}</li>
 *   <li>{@code {{weeksToRace}}}</li>
 *   <li>{@code {{fatigue}}}</li>
 *   <li>{@code {{fitness}}}</li>
 *   <li>{@code {{efficiency}}}</li>
 *   <li>{@code {{longRunCapacity}}}</li>
 *   <li>{@code {{zones}}}</li>
 *   <li>{@code {{recentWorkouts}}}</li>
 * </ul>
 */
@Component
public class PromptBuilder {

    private static final String TEMPLATE_PATH = "classpath:prompts/weekly_training_plan_prompt.txt";

    private final ResourceLoader resourceLoader;
    private String template;

    public PromptBuilder(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void loadTemplate() {
        Resource resource = resourceLoader.getResource(TEMPLATE_PATH);
        try {
            this.template = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load prompt template: " + TEMPLATE_PATH, e);
        }
    }

    /**
     * Builds the complete LLM prompt by injecting athlete state and recent workouts
     * into the loaded template.
     *
     * @param state          current athlete metrics and race context
     * @param recentWorkouts workouts from the past 2–4 weeks, ordered by date descending
     * @return fully populated prompt string ready to send to an LLM
     */
    public String buildWeeklyPlanPrompt(AthleteState state, List<Workout> recentWorkouts, java.time.LocalDate weekStartDate) {
        return template
                .replace("{{goal}}", state.goal())
                .replace("{{weeksToRace}}", String.valueOf(state.weeksToRace()))
                .replace("{{fatigue}}", String.format("%.1f", state.fatigue()))
                .replace("{{fitness}}", String.format("%.1f", state.fitness()))
                .replace("{{efficiency}}", String.format("%.4f", state.efficiency()))
                .replace("{{longRunCapacity}}", String.valueOf(state.longRunCapacity()))
                .replace("{{zones}}", formatZones(state.zones()))
                .replace("{{recentWorkouts}}", formatWorkouts(recentWorkouts))
                .replace("{{weekStartDate}}", weekStartDate != null ? weekStartDate.toString() : "");
    }

    // -------------------------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------------------------

    private String formatZones(List<PaceZoneDto> zones) {
        if (zones == null || zones.isEmpty()) {
            return "No pace zones configured.";
        }
        StringBuilder sb = new StringBuilder();
        for (PaceZoneDto z : zones) {
            sb.append(String.format("  Z%d %-12s: %s%n",
                    z.zone(),
                    z.name(),
                    formatZoneBounds(z.fastestPaceSecPerKm(), z.slowestPaceSecPerKm())));
        }
        return sb.toString().stripTrailing();
    }

    /**
     * Returns a human-readable pace range string, e.g. {@code "5:30 – 6:10 min/km"}.
     * Open-ended bounds use {@code "<"} or {@code ">"} notation.
     */
    private String formatZoneBounds(Integer fastest, Integer slowest) {
        if (fastest == null && slowest == null) {
            return "no limits";
        }
        if (fastest == null) {
            return "> " + formatPace(slowest) + " min/km";
        }
        if (slowest == null) {
            return "< " + formatPace(fastest) + " min/km";
        }
        return formatPace(fastest) + " – " + formatPace(slowest) + " min/km";
    }

    /** Converts seconds-per-km to {@code "M:SS"} format. */
    private String formatPace(int secPerKm) {
        return String.format("%d:%02d", secPerKm / 60, secPerKm % 60);
    }

    private String formatWorkouts(List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) {
            return "No recent workouts available.";
        }
        StringBuilder sb = new StringBuilder();
        for (Workout w : workouts) {
            sb.append(String.format("  - %s | %s | dist: %s km | dur: %s min | pace: %s | HR: %s bpm%n",
                    w.date(),
                    nvl(w.activityName(), "Unknown"),
                    w.distanceKm()   != null ? String.format("%.2f", w.distanceKm()) : "—",
                    w.durationMinutes() != null ? w.durationMinutes()              : "—",
                    w.averagePaceSecondsPerKm() != null ? formatPace(w.averagePaceSecondsPerKm()) : "—",
                    w.averageHeartRate() != null ? w.averageHeartRate()            : "—"));
        }
        return sb.toString().stripTrailing();
    }

    private static String nvl(String value, String fallback) {
        return value != null ? value : fallback;
    }
}


