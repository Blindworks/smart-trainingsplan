package com.trainingsplan.service;

import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.entity.Training;
import com.trainingsplan.entity.TrainingWeek;
import com.trainingsplan.repository.TrainingPlanRepository;
import com.trainingsplan.repository.CompetitionRepository;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.repository.TrainingWeekRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

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

    private ObjectMapper objectMapper = new ObjectMapper();

    public List<TrainingPlan> findAll() {
        return trainingPlanRepository.findAll();
    }

    public TrainingPlan findById(Long id) {
        return trainingPlanRepository.findById(id).orElse(null);
    }

    public List<TrainingPlan> findByCompetitionId(Long competitionId) {
        return trainingPlanRepository.findByCompetitionId(competitionId);
    }

    public TrainingPlan save(TrainingPlan trainingPlan) {
        return trainingPlanRepository.save(trainingPlan);
    }

    public void deleteById(Long id) {
        trainingPlanRepository.deleteById(id);
    }

    public TrainingPlan uploadTrainingPlan(MultipartFile file, String name, String description, Long competitionId) throws Exception {
        Competition competition = competitionRepository.findById(competitionId).orElse(null);
        if (competition == null) {
            throw new RuntimeException("Competition not found");
        }

        String jsonContent = new String(file.getBytes());
        TrainingPlan trainingPlan = new TrainingPlan(name, description, jsonContent);
        trainingPlan.setCompetition(competition);
        
        TrainingPlan savedPlan = trainingPlanRepository.save(trainingPlan);
        
        parseAndCreateTrainings(savedPlan, jsonContent);
        
        return savedPlan;
    }

    private void parseAndCreateTrainings(TrainingPlan trainingPlan, String jsonContent) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            List<Training> trainings = new ArrayList<>();

            // Check for old format first
            if (rootNode.has("trainings") && rootNode.get("trainings").isArray()) {
                parseOldFormat(rootNode, trainings, trainingPlan);
            }
            // Check for new marathon plan format
            else if (rootNode.has("marathon_plan")) {
                parseMarathonPlanFormat(rootNode.get("marathon_plan"), trainings, trainingPlan);
            }
            // Check for half marathon plan format
            else if (rootNode.has("half_marathon_plan")) {
                parseMarathonPlanFormat(rootNode.get("half_marathon_plan"), trainings, trainingPlan);
            }
            // Check if root is marathon plan directly
            else if (rootNode.has("weeks")) {
                parseMarathonPlanFormat(rootNode, trainings, trainingPlan);
            }

            trainingRepository.saveAll(trainings);
            trainingPlan.setTrainings(trainings);
            trainingPlanRepository.save(trainingPlan);

        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON training plan: " + e.getMessage(), e);
        }
    }

    private void parseOldFormat(JsonNode rootNode, List<Training> trainings, TrainingPlan trainingPlan) {
        for (JsonNode trainingNode : rootNode.get("trainings")) {
            LocalDate trainingDate = LocalDate.parse(trainingNode.get("date").asText());
            
            Training training = new Training();
            training.setName(trainingNode.get("name").asText());
            training.setDescription(trainingNode.get("description").asText(""));
            training.setTrainingDate(trainingDate);
            training.setTrainingType(trainingNode.get("type").asText(""));
            training.setIntensityLevel(trainingNode.get("intensity").asText(""));
            
            if (trainingNode.has("startTime")) {
                training.setStartTime(LocalTime.parse(trainingNode.get("startTime").asText()));
            }
            if (trainingNode.has("duration")) {
                training.setDurationMinutes(trainingNode.get("duration").asInt());
            }
            
            training.setTrainingPlan(trainingPlan);
            
            // Find or create corresponding TrainingWeek
            TrainingWeek trainingWeek = findTrainingWeekForDate(trainingPlan.getCompetition(), trainingDate);
            if (trainingWeek != null) {
                training.setTrainingWeek(trainingWeek);
            }
            
            trainings.add(training);
        }
    }

    private void parseMarathonPlanFormat(JsonNode planNode, List<Training> trainings, TrainingPlan trainingPlan) {
        JsonNode weeksNode = planNode.get("weeks");
        if (weeksNode == null || !weeksNode.isArray()) {
            throw new RuntimeException("Invalid marathon plan format: missing weeks array");
        }

        LocalDate competitionDate = trainingPlan.getCompetition().getDate();
        
        // Finde den Sonntag der Wettkampfwoche (meist ist der Wettkampf schon ein Sonntag)
        LocalDate competitionSunday = competitionDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        // Finde die höchste Wochennummer im Plan (= Wettkampfwoche)
        int maxWeekNumber = 0;
        for (JsonNode weekNode : weeksNode) {
            int weekNumber = weekNode.get("week").asInt();
            maxWeekNumber = Math.max(maxWeekNumber, weekNumber);
        }
        
        for (JsonNode weekNode : weeksNode) {
            int weekNumber = weekNode.get("week").asInt();
            JsonNode scheduleNode = weekNode.get("schedule");
            
            if (scheduleNode != null) {
                // Berechne das Start-Datum dieser Trainingswoche rückwärts vom Wettkampf
                // maxWeekNumber (z.B. 12) = Wettkampfwoche = 0 Wochen zurück
                // weekNumber 11 = 1 Woche vor Wettkampf, weekNumber 1 = 11 Wochen vor Wettkampf
                int weeksBeforeCompetition = maxWeekNumber - weekNumber;
                LocalDate weekSunday = competitionSunday.minusWeeks(weeksBeforeCompetition);
                LocalDate weekMonday = weekSunday.minusDays(6);
                
                // Nur Trainings erstellen, die nicht in der Vergangenheit liegen
                if (!weekSunday.isBefore(LocalDate.now())) {
                    parseWeekSchedule(scheduleNode, trainings, trainingPlan, weekMonday, weekNumber);
                }
            }
        }
    }

    private void parseWeekSchedule(JsonNode scheduleNode, List<Training> trainings, 
                                 TrainingPlan trainingPlan, LocalDate weekMonday, int weekNumber) {
        
        // Der Montag ist bereits berechnet, Sonntag ist 6 Tage später
        LocalDate weekSunday = weekMonday.plusDays(6);
        
        // Find or create the corresponding TrainingWeek
        TrainingWeek trainingWeek = findOrCreateTrainingWeek(trainingPlan.getCompetition(), weekNumber, weekMonday, weekSunday);
        
        String[] weekdays = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        
        for (int dayIndex = 0; dayIndex < weekdays.length; dayIndex++) {
            String dayName = weekdays[dayIndex];
            JsonNode dayNode = scheduleNode.get(dayName);
            
            if (dayNode != null && dayNode.has("workout")) {
                String workout = dayNode.get("workout").asText();
                String intensity = dayNode.has("intensity") ? dayNode.get("intensity").asText("") : "";
                
                // Skip rest days
                if ("Ruhetag".equals(workout) || intensity.equals("0%")) {
                    continue;
                }
                
                Training training = new Training();
                training.setName(capitalizeFirstLetter(dayName) + " - Woche " + weekNumber);
                training.setDescription(workout);
                training.setTrainingDate(weekMonday.plusDays(dayIndex));
                training.setTrainingType(extractTrainingType(workout));
                training.setIntensityLevel(mapIntensityLevel(intensity));
                training.setTrainingPlan(trainingPlan);
                training.setTrainingWeek(trainingWeek);  // Wichtig: TrainingWeek zuordnen
                
                // Extract duration if mentioned in workout description
                Integer duration = extractDuration(workout);
                if (duration != null) {
                    training.setDurationMinutes(duration);
                }
                
                trainings.add(training);
            }
        }
    }

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
        // Extract duration from workout description
        if (workout.contains("1 h")) return 60;
        if (workout.contains("2 h")) return 120;
        if (workout.contains("1,5 h")) return 90;
        
        // Try to extract km and estimate duration (assuming ~6 min/km average)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*km");
        java.util.regex.Matcher matcher = pattern.matcher(workout);
        if (matcher.find()) {
            int km = Integer.parseInt(matcher.group(1));
            return km * 6; // Approximate 6 minutes per km
        }
        
        return null;
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private TrainingWeek findOrCreateTrainingWeek(Competition competition, int weekNumber, LocalDate startDate, LocalDate endDate) {
        // Try to find existing TrainingWeek by competition and week number
        TrainingWeek existingWeek = trainingWeekRepository.findByCompetitionIdAndWeekNumber(competition.getId(), weekNumber);
        
        if (existingWeek != null) {
            return existingWeek;
        }
        
        // Create new TrainingWeek if not found
        TrainingWeek newWeek = new TrainingWeek();
        newWeek.setWeekNumber(weekNumber);
        newWeek.setStartDate(startDate);
        newWeek.setEndDate(endDate);
        newWeek.setCompetition(competition);
        newWeek.setIsModified(false);
        
        return trainingWeekRepository.save(newWeek);
    }

    private TrainingWeek findTrainingWeekForDate(Competition competition, LocalDate date) {
        return trainingWeekRepository.findByCompetitionIdAndDate(competition.getId(), date);
    }
}