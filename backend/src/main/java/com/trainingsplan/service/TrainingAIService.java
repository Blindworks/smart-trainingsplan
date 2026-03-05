package com.trainingsplan.service;

import com.trainingsplan.dto.AITrainingPlanDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class TrainingAIService {

    private final AIPlanGeneratorService aiPlanGeneratorService;

    public TrainingAIService(AIPlanGeneratorService aiPlanGeneratorService) {
        this.aiPlanGeneratorService = aiPlanGeneratorService;
    }

    public AITrainingPlanDTO generateWeeklyPlan(Long userId, LocalDate weekStartDate) {
        return aiPlanGeneratorService.generateWeeklyPlan(userId, weekStartDate);
    }
}
