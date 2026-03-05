package com.trainingsplan.controller;

import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.dto.AITrainingPlanGenerateRequest;
import com.trainingsplan.dto.MessageResponse;
import com.trainingsplan.service.AIPlanPersistenceService;
import com.trainingsplan.service.TrainingAIService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/training-plan")
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class AITrainingPlanController {

    private static final Logger log = LoggerFactory.getLogger(AITrainingPlanController.class);

    private final TrainingAIService trainingAIService;
    private final AIPlanPersistenceService aiPlanPersistenceService;

    public AITrainingPlanController(TrainingAIService trainingAIService,
                                    AIPlanPersistenceService aiPlanPersistenceService) {
        this.trainingAIService = trainingAIService;
        this.aiPlanPersistenceService = aiPlanPersistenceService;
    }

    @GetMapping("/{planId}")
    public ResponseEntity<?> getPlan(@PathVariable String planId) {
        try {
            return ResponseEntity.ok(aiPlanPersistenceService.getById(planId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody AITrainingPlanGenerateRequest request) {
        long startNanos = System.nanoTime();
        try {
            AITrainingPlanDTO generatedPlan = trainingAIService.generateWeeklyPlan(
                    request.userId(),
                    request.weekStartDate()
            );
            AITrainingPlanDTO savedPlan = aiPlanPersistenceService.save(generatedPlan, request.userId());
            long generationTimeMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info(
                    "event=ai_training_plan_generated userId={} weekStartDate={} modelName={} generationTimeMs={} planId={}",
                    request.userId(),
                    request.weekStartDate(),
                    savedPlan.getModelName(),
                    generationTimeMs,
                    savedPlan.getId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPlan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
