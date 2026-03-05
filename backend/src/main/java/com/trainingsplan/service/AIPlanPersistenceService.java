package com.trainingsplan.service;

import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.entity.AiTrainingPlan;
import com.trainingsplan.entity.User;
import com.trainingsplan.mapper.AITrainingPlanMapper;
import com.trainingsplan.repository.AiTrainingPlanRepository;
import com.trainingsplan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists an {@link AITrainingPlanDTO} produced by {@link TrainingAIService}
 * into the database as {@link AiTrainingPlan}, {@link AiTrainingDay}, and
 * {@link AiTrainingWorkout} entities.
 */
@Service
public class AIPlanPersistenceService {

    @Autowired
    private AiTrainingPlanRepository planRepository;

    @Autowired
    private AITrainingPlanMapper aiTrainingPlanMapper;

    @Autowired
    private UserRepository userRepository;

    /**
     * Converts the DTO to entities and saves them in a single transaction.
     *
     * @param dto    the plan produced by {@link TrainingAIService}
     * @param userId the owner of the plan
     * @return the saved plan as a DTO (with server-assigned id and createdAt)
     */
    @Transactional
    public AITrainingPlanDTO save(AITrainingPlanDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        AiTrainingPlan plan = aiTrainingPlanMapper.toEntity(dto);
        plan.setUser(user);
        AiTrainingPlan savedPlan = planRepository.save(plan);
        return aiTrainingPlanMapper.toDTO(savedPlan);
    }

    /**
     * Returns a single plan by id.
     *
     * @param planId the UUID of the plan
     * @return the plan as a DTO
     */
    @Transactional(readOnly = true)
    public AITrainingPlanDTO getById(String planId) {
        AiTrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
        return aiTrainingPlanMapper.toDTO(plan);
    }

    /**
     * Deletes a plan and all its days and workouts (cascade).
     *
     * @param planId the UUID of the plan to delete
     * @param userId the user requesting deletion — ownership is verified
     */
    @Transactional
    public void delete(String planId, Long userId) {
        AiTrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
        if (!plan.getUser().getId().equals(userId)) {
            throw new SecurityException("Plan does not belong to user: " + userId);
        }
        planRepository.delete(plan);
    }
}
