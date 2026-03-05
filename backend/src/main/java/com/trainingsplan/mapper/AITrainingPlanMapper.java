package com.trainingsplan.mapper;

import com.trainingsplan.dto.AITrainingDayDTO;
import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.dto.AIWorkoutDTO;
import com.trainingsplan.entity.AiTrainingDay;
import com.trainingsplan.entity.AiTrainingPlan;
import com.trainingsplan.entity.AiTrainingWorkout;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AITrainingPlanMapper {

    public AiTrainingPlan toEntity(AITrainingPlanDTO dto) {
        if (dto == null) {
            return null;
        }

        AiTrainingPlan plan = new AiTrainingPlan();
        if (dto.getId() != null) {
            plan.setId(dto.getId());
        }
        if (dto.getCreatedAt() != null) {
            plan.setCreatedAt(dto.getCreatedAt());
        }
        plan.setWeekStartDate(dto.getWeekStartDate());
        plan.setModelName(dto.getModelName());
        plan.setModelVersion(dto.getModelVersion());
        if (dto.getStatus() != null) {
            plan.setStatus(dto.getStatus());
        }

        List<AiTrainingDay> days = new ArrayList<>();
        if (dto.getDays() != null) {
            for (int i = 0; i < dto.getDays().size(); i++) {
                AITrainingDayDTO dayDto = dto.getDays().get(i);
                AiTrainingDay day = toDayEntity(dayDto);
                day.setPlan(plan);
                day.setDayIndex(i);
                days.add(day);
            }
        }
        plan.setDays(days);
        return plan;
    }

    public AITrainingPlanDTO toDTO(AiTrainingPlan entity) {
        if (entity == null) {
            return null;
        }

        AITrainingPlanDTO dto = new AITrainingPlanDTO();
        dto.setId(entity.getId());
        dto.setWeekStartDate(entity.getWeekStartDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModelName(entity.getModelName());
        dto.setModelVersion(entity.getModelVersion());
        dto.setStatus(entity.getStatus());

        List<AITrainingDayDTO> dayDtos = new ArrayList<>();
        if (entity.getDays() != null) {
            for (AiTrainingDay day : entity.getDays()) {
                dayDtos.add(toDayDTO(day));
            }
        }
        dto.setDays(dayDtos);
        return dto;
    }

    private AiTrainingDay toDayEntity(AITrainingDayDTO dto) {
        AiTrainingDay day = new AiTrainingDay();
        day.setDate(dto.getDate());

        List<AiTrainingWorkout> workouts = new ArrayList<>();
        if (dto.getWorkouts() != null) {
            for (AIWorkoutDTO workoutDto : dto.getWorkouts()) {
                AiTrainingWorkout workout = toWorkoutEntity(workoutDto);
                workout.setDay(day);
                workouts.add(workout);
            }
        }
        day.setWorkouts(workouts);
        return day;
    }

    private AITrainingDayDTO toDayDTO(AiTrainingDay entity) {
        AITrainingDayDTO dto = new AITrainingDayDTO();
        dto.setDate(entity.getDate());

        List<AIWorkoutDTO> workoutDtos = new ArrayList<>();
        if (entity.getWorkouts() != null) {
            for (AiTrainingWorkout workout : entity.getWorkouts()) {
                workoutDtos.add(toWorkoutDTO(workout));
            }
        }
        dto.setWorkouts(workoutDtos);
        return dto;
    }

    private AiTrainingWorkout toWorkoutEntity(AIWorkoutDTO dto) {
        AiTrainingWorkout workout = new AiTrainingWorkout();
        workout.setType(dto.getType());
        workout.setTargetZone(dto.getTargetZone());
        workout.setDurationMinutes(dto.getDurationMinutes());
        workout.setDescription(dto.getDescription());
        workout.setStructureJson(dto.getStructure());
        return workout;
    }

    private AIWorkoutDTO toWorkoutDTO(AiTrainingWorkout entity) {
        AIWorkoutDTO dto = new AIWorkoutDTO();
        dto.setType(entity.getType());
        dto.setTargetZone(entity.getTargetZone());
        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setDescription(entity.getDescription());
        dto.setStructure(entity.getStructureJson());
        return dto;
    }
}
