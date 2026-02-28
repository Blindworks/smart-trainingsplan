package com.trainingsplan.service;

import com.trainingsplan.dto.TrainingPlanDto;
import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.entity.Training;
import com.trainingsplan.entity.TrainingDescription;
import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.entity.TrainingWeek;
import com.trainingsplan.repository.CompetitionRegistrationRepository;
import com.trainingsplan.repository.CompetitionRepository;
import com.trainingsplan.repository.TrainingDescriptionRepository;
import com.trainingsplan.repository.TrainingPlanRepository;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.repository.TrainingWeekRepository;
import com.trainingsplan.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingPlanService {

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private TrainingWeekRepository trainingWeekRepository;

    @Autowired
    private TrainingDescriptionRepository trainingDescriptionRepository;

    @Autowired
    private CompetitionRegistrationRepository registrationRepository;

    @Autowired
    private SecurityUtils securityUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Basic CRUD
    // -------------------------------------------------------------------------

    public List<TrainingPlan> findAll() {
        return trainingPlanRepository.findAll();
    }

    public TrainingPlan findById(Long id) {
        return trainingPlanRepository.findById(id).orElse(null);
    }

    public TrainingPlan save(TrainingPlan trainingPlan) {
        return trainingPlanRepository.save(trainingPlan);
    }

    public void deleteById(Long id) {
        trainingPlanRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Upload plan directly linked to a competition
    // -------------------------------------------------------------------------

    public TrainingPlan uploadTrainingPlan(MultipartFile file, String name, String description, Long competitionId) throws Exception {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("Competition not found: " + competitionId));

        clearExistingTrainings(competition, null);

        String jsonContent = new String(file.getBytes());
        TrainingPlan trainingPlan = new TrainingPlan(name, description, jsonContent);
        trainingPlan.setTrainingCount(countTrainingsInJson(jsonContent));
        TrainingPlan savedPlan = trainingPlanRepository.save(trainingPlan);

        // Store plan on the user's registration
        updateRegistrationPlan(competition, savedPlan);

        parseAndCreateTrainings(savedPlan, competition, jsonContent);
        return savedPlan;
    }

    // -------------------------------------------------------------------------
    // Template operations
    // -------------------------------------------------------------------------

    /**
     * Saves the plan JSON as a library template. No Training records are created
     * because templates carry no competition date — dates are resolved on assignment.
     */
    public TrainingPlanDto uploadAsTemplate(MultipartFile file, String name, String description) throws Exception {
        String jsonContent = new String(file.getBytes());

        // Validate JSON is parseable before persisting
        objectMapper.readTree(jsonContent);

        TrainingPlan template = new TrainingPlan(name, description, jsonContent);
        template.setTrainingCount(countTrainingsInJson(jsonContent));

        return new TrainingPlanDto(trainingPlanRepository.save(template));
    }

    /**
     * Returns all plans (all plans are implicitly templates in the new architecture).
     */
    public List<TrainingPlanDto> findAllTemplates() {
        return trainingPlanRepository.findAll()
                .stream()
                .map(TrainingPlanDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Assigns an existing plan to a competition and generates Training records.
     * Dates in old-format plans are shifted so the last training aligns with the
     * competition date. No new TrainingPlan record is created — the source plan
     * is reused directly.
     */
    public TrainingPlanDto assignPlanToCompetition(Long planId, Long competitionId) throws Exception {
        TrainingPlan sourcePlan = trainingPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Training plan not found: " + planId));
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("Competition not found: " + competitionId));

        clearExistingTrainings(competition, null);

        String jsonContent = sourcePlan.getJsonContent();
        String effectiveJson = prepareJsonForCompetition(jsonContent, competition.getDate());

        updateRegistrationPlan(competition, sourcePlan);

        parseAndCreateTrainings(sourcePlan, competition, effectiveJson);
        return new TrainingPlanDto(sourcePlan);
    }

    // -------------------------------------------------------------------------
    // JSON preparation helpers
    // -------------------------------------------------------------------------

    /**
     * For old-format JSON (has a top-level "trainings" array with absolute
     * dates) the dates are shifted so the last training lands on the competition
     * date. Marathon/half-marathon formats compute their own dates from the
     * competition date inside parseMarathonPlanFormat, so they are returned
     * unchanged.
     */
    private String prepareJsonForCompetition(String jsonContent, LocalDate competitionDate) throws Exception {
        JsonNode root = objectMapper.readTree(jsonContent);
        if (root.has("trainings") && root.get("trainings").isArray()) {
            return shiftDatesToCompetition(jsonContent, competitionDate);
        }
        return jsonContent;
    }

    /**
     * Rewrites every date in the top-level "trainings" array so that the maximum
     * date in the original plan maps to competitionDate, preserving relative
     * spacing between trainings.
     */
    private String shiftDatesToCompetition(String jsonContent, LocalDate competitionDate) throws Exception {
        JsonNode rootForMax = objectMapper.readTree(jsonContent);
        JsonNode trainingsForMax = rootForMax.get("trainings");

        LocalDate maxDate = LocalDate.MIN;
        for (JsonNode t : trainingsForMax) {
            LocalDate d = LocalDate.parse(t.path("date").asText());
            if (d.isAfter(maxDate)) {
                maxDate = d;
            }
        }

        long offsetDays = ChronoUnit.DAYS.between(maxDate, competitionDate);

        ObjectNode mutable = (ObjectNode) objectMapper.readTree(jsonContent);
        ArrayNode arr = (ArrayNode) mutable.get("trainings");
        for (int i = 0; i < arr.size(); i++) {
            ObjectNode t = (ObjectNode) arr.get(i);
            LocalDate shifted = LocalDate.parse(t.path("date").asText()).plusDays(offsetDays);
            t.put("date", shifted.toString());
        }
        return objectMapper.writeValueAsString(mutable);
    }

    /**
     * Counts trainings described in the JSON without persisting anything.
     * Returns null when the count cannot be determined (e.g. marathon-format
     * where the exact count depends on the competition date).
     */
    private Integer countTrainingsInJson(String jsonContent) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            if (root.has("trainings") && root.get("trainings").isArray()) {
                return root.get("trainings").size();
            }
            // Marathon / half-marathon formats — count across all week schedules
            JsonNode planNode = root.has("marathon_plan") ? root.get("marathon_plan")
                    : root.has("half_marathon_plan") ? root.get("half_marathon_plan")
                    : root.has("weeks") ? root : null;

            if (planNode != null && planNode.has("weeks")) {
                int count = 0;
                String[] weekdays = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
                for (JsonNode weekNode : planNode.get("weeks")) {
                    JsonNode schedule = weekNode.get("schedule");
                    if (schedule == null) continue;
                    for (String day : weekdays) {
                        JsonNode dayNode = schedule.get(day);
                        if (dayNode != null && dayNode.has("workout")) {
                            String workout = dayNode.get("workout").asText("");
                            String intensity = dayNode.has("intensity") ? dayNode.get("intensity").asText("") : "";
                            if (!"Ruhetag".equals(workout) && !"0%".equals(intensity)) {
                                count++;
                            }
                        }
                    }
                }
                return count;
            }
        } catch (Exception ignored) {
            // JSON parsing failure — return null rather than crash
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Training record creation
    // -------------------------------------------------------------------------

    private void parseAndCreateTrainings(TrainingPlan trainingPlan, Competition competition, String jsonContent) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            List<Training> trainings = new ArrayList<>();

            if (rootNode.has("trainings") && rootNode.get("trainings").isArray()) {
                parseOldFormat(rootNode, trainings, trainingPlan, competition);
            } else if (rootNode.has("marathon_plan")) {
                parseMarathonPlanFormat(rootNode.get("marathon_plan"), trainings, trainingPlan, competition);
            } else if (rootNode.has("half_marathon_plan")) {
                parseMarathonPlanFormat(rootNode.get("half_marathon_plan"), trainings, trainingPlan, competition);
            } else if (rootNode.has("weeks")) {
                parseMarathonPlanFormat(rootNode, trainings, trainingPlan, competition);
            }

            trainingRepository.saveAll(trainings);

        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON training plan: " + e.getMessage(), e);
        }
    }

    private void parseOldFormat(JsonNode rootNode, List<Training> trainings,
                                 TrainingPlan trainingPlan, Competition competition) {
        for (JsonNode trainingNode : rootNode.get("trainings")) {
            LocalDate trainingDate = LocalDate.parse(trainingNode.path("date").asText());

            Training training = new Training();
            training.setName(trainingNode.path("name").asText("Training"));
            training.setTrainingDescription(findOrCreateTrainingDescription(trainingNode.path("description").asText("")));
            training.setTrainingDate(trainingDate);
            training.setTrainingType(trainingNode.path("type").asText(""));
            training.setIntensityLevel(trainingNode.path("intensity").asText(""));

            if (trainingNode.has("duration")) {
                training.setDurationMinutes(trainingNode.get("duration").asInt());
            }

            training.setTrainingPlan(trainingPlan);

            TrainingWeek trainingWeek = findTrainingWeekForDate(competition, trainingDate);
            if (trainingWeek != null) {
                training.setTrainingWeek(trainingWeek);
            }

            trainings.add(training);
        }
    }

    private void parseMarathonPlanFormat(JsonNode planNode, List<Training> trainings,
                                          TrainingPlan trainingPlan, Competition competition) {
        JsonNode weeksNode = planNode.get("weeks");
        if (weeksNode == null || !weeksNode.isArray()) {
            throw new RuntimeException("Invalid marathon plan format: missing weeks array");
        }

        LocalDate competitionDate = competition.getDate();
        LocalDate competitionSunday = competitionDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        int maxWeekNumber = 0;
        for (JsonNode weekNode : weeksNode) {
            int weekNumber = weekNode.get("week").asInt();
            maxWeekNumber = Math.max(maxWeekNumber, weekNumber);
        }

        for (JsonNode weekNode : weeksNode) {
            int weekNumber = weekNode.get("week").asInt();
            JsonNode scheduleNode = weekNode.get("schedule");

            if (scheduleNode != null) {
                int weeksBeforeCompetition = maxWeekNumber - weekNumber;
                LocalDate weekSunday = competitionSunday.minusWeeks(weeksBeforeCompetition);
                LocalDate weekMonday = weekSunday.minusDays(6);

                parseWeekSchedule(scheduleNode, trainings, trainingPlan, competition, weekMonday, weekNumber);
            }
        }
    }

    private void parseWeekSchedule(JsonNode scheduleNode, List<Training> trainings,
                                   TrainingPlan trainingPlan, Competition competition,
                                   LocalDate weekMonday, int weekNumber) {
        LocalDate weekSunday = weekMonday.plusDays(6);
        TrainingWeek trainingWeek = findOrCreateTrainingWeek(competition, weekNumber, weekMonday, weekSunday);

        String[] weekdays = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        for (int dayIndex = 0; dayIndex < weekdays.length; dayIndex++) {
            String dayName = weekdays[dayIndex];
            JsonNode dayNode = scheduleNode.get(dayName);

            if (dayNode != null && dayNode.has("workout")) {
                String workout = dayNode.get("workout").asText();
                String intensity = dayNode.has("intensity") ? dayNode.get("intensity").asText("") : "";

                if ("Ruhetag".equals(workout) || "0%".equals(intensity)) {
                    continue;
                }

                Training training = new Training();
                training.setName(capitalizeFirstLetter(dayName) + " - Woche " + weekNumber);
                training.setTrainingDescription(findOrCreateTrainingDescription(workout));
                training.setTrainingDate(weekMonday.plusDays(dayIndex));
                training.setTrainingType(extractTrainingType(workout));
                training.setIntensityLevel(mapIntensityLevel(intensity));
                training.setTrainingPlan(trainingPlan);
                training.setTrainingWeek(trainingWeek);

                Integer duration = extractDuration(workout);
                if (duration != null) {
                    training.setDurationMinutes(duration);
                }

                trainings.add(training);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private String extractTrainingType(String workout) {
        String workoutLower = workout.toLowerCase();
        if (workoutLower.contains("krafttraining")) return "strength";
        if (workoutLower.contains("intervall")) return "interval";
        if (workoutLower.contains("wettkampf")) return "race";
        if (workoutLower.contains("fahrtspiel")) return "fartlek";
        if (workoutLower.contains("schwimmen")) return "swimming";
        if (workoutLower.contains("radfahren")) return "cycling";
        if (workoutLower.contains("dauerlauf") || workoutLower.contains("km")) return "endurance";
        return "general";
    }

    private String mapIntensityLevel(String intensityPercent) {
        if (intensityPercent.isEmpty() || intensityPercent.equals("0%")) return "rest";
        try {
            int intensity = Integer.parseInt(intensityPercent.replace("%", ""));
            if (intensity >= 90) return "high";
            if (intensity >= 75) return "medium";
            if (intensity >= 65) return "low";
            return "recovery";
        } catch (NumberFormatException e) {
            return "medium";
        }
    }

    private Integer extractDuration(String workout) {
        if (workout.contains("1 h")) return 60;
        if (workout.contains("2 h")) return 120;
        if (workout.contains("1,5 h")) return 90;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*km");
        java.util.regex.Matcher matcher = pattern.matcher(workout);
        if (matcher.find()) {
            int km = Integer.parseInt(matcher.group(1));
            return km * 6;
        }
        return null;
    }

    private TrainingDescription findOrCreateTrainingDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        String normalized = description.trim();
        List<TrainingDescription> existing = trainingDescriptionRepository.findAllByNameOrderByIdAsc(normalized);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        TrainingDescription newDescription = new TrainingDescription();
        newDescription.setName(normalized);
        newDescription.setDetailedInstructions(normalized);
        return trainingDescriptionRepository.save(newDescription);
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private TrainingWeek findOrCreateTrainingWeek(Competition competition, int weekNumber,
                                                   LocalDate startDate, LocalDate endDate) {
        List<TrainingWeek> existingWeeks = trainingWeekRepository.findByCompetitionIdAndWeekNumberOrderByIdAsc(
                competition.getId(), weekNumber);
        if (!existingWeeks.isEmpty()) {
            return existingWeeks.get(0);
        }
        TrainingWeek newWeek = new TrainingWeek();
        newWeek.setWeekNumber(weekNumber);
        newWeek.setStartDate(startDate);
        newWeek.setEndDate(endDate);
        newWeek.setCompetition(competition);
        newWeek.setIsModified(false);
        return trainingWeekRepository.save(newWeek);
    }

    private TrainingWeek findTrainingWeekForDate(Competition competition, LocalDate date) {
        List<TrainingWeek> weeks = trainingWeekRepository.findByCompetitionIdAndDate(competition.getId(), date);
        return weeks.isEmpty() ? null : weeks.get(0);
    }

    private void updateRegistrationPlan(Competition competition, TrainingPlan plan) {
        var user = securityUtils.getCurrentUser();
        if (user == null) return;
        CompetitionRegistration reg = registrationRepository
                .findByCompetitionIdAndUserId(competition.getId(), user.getId())
                .orElseGet(() -> {
                    CompetitionRegistration newReg = new CompetitionRegistration(competition, user);
                    return registrationRepository.save(newReg);
                });
        reg.setTrainingPlan(plan);
        registrationRepository.save(reg);
    }

    /**
     * Deletes all Training records belonging to a competition before a new plan
     * is assigned, preventing duplicate entries in the calendar view.
     *
     * Covers two cases:
     *  1. Trainings linked via TrainingWeek → Competition (all plan formats).
     *  2. Orphan trainings linked only via TrainingPlan but without a TrainingWeek
     *     (old-format upload when no TrainingWeek existed for the date).
     */
    private void clearExistingTrainings(Competition competition, TrainingPlan previousPlan) {
        List<Training> viaWeek = trainingRepository.findByCompetitionId(competition.getId());
        if (!viaWeek.isEmpty()) {
            trainingRepository.deleteAll(viaWeek);
        }
        if (previousPlan != null) {
            List<Training> orphans = trainingRepository
                    .findByTrainingPlanIdAndTrainingWeekIsNull(previousPlan.getId());
            if (!orphans.isEmpty()) {
                trainingRepository.deleteAll(orphans);
            }
        }
    }
}
