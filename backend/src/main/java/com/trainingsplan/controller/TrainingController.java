package com.trainingsplan.controller;

import com.trainingsplan.entity.Training;
import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.repository.TrainingPlanRepository;
import com.trainingsplan.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/trainings")
public class TrainingController {

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    @GetMapping
    public ResponseEntity<List<Training>> getAllTrainings() {
        return ResponseEntity.ok(trainingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Training> getTrainingById(@PathVariable Long id) {
        Training training = trainingService.findById(id);
        if (training != null) return ResponseEntity.ok(training);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<Training>> getTrainingsByPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(trainingService.findByTrainingPlanId(planId));
    }

    @PostMapping
    public ResponseEntity<Training> createTraining(
            @Valid @RequestBody Training training,
            @RequestParam(value = "planId", required = false) Long planId) {
        if (planId != null) {
            trainingPlanRepository.findById(planId).ifPresent(training::setTrainingPlan);
        }
        return ResponseEntity.ok(trainingService.save(training));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Training> updateTraining(@PathVariable Long id,
                                                   @Valid @RequestBody Training training) {
        if (trainingService.findById(id) == null) return ResponseEntity.notFound().build();
        training.setId(id);
        return ResponseEntity.ok(trainingService.save(training));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTraining(@PathVariable Long id) {
        if (trainingService.findById(id) == null) return ResponseEntity.notFound().build();
        trainingService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
