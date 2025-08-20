package com.trainingsplan.controller;

import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.service.TrainingPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/training-plans")
@CrossOrigin(origins = "http://localhost:3000")
public class TrainingPlanController {

    @Autowired
    private TrainingPlanService trainingPlanService;

    @GetMapping
    public ResponseEntity<List<TrainingPlan>> getAllTrainingPlans() {
        return ResponseEntity.ok(trainingPlanService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingPlan> getTrainingPlanById(@PathVariable Long id) {
        TrainingPlan trainingPlan = trainingPlanService.findById(id);
        if (trainingPlan != null) {
            return ResponseEntity.ok(trainingPlan);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<List<TrainingPlan>> getTrainingPlansByCompetition(@PathVariable Long competitionId) {
        return ResponseEntity.ok(trainingPlanService.findByCompetitionId(competitionId));
    }

    @PostMapping
    public ResponseEntity<TrainingPlan> createTrainingPlan(@Valid @RequestBody TrainingPlan trainingPlan) {
        TrainingPlan savedPlan = trainingPlanService.save(trainingPlan);
        return ResponseEntity.ok(savedPlan);
    }

    @PostMapping("/upload")
    public ResponseEntity<TrainingPlan> uploadTrainingPlan(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("name") String name,
                                                          @RequestParam("description") String description,
                                                          @RequestParam("competitionId") Long competitionId) {
        try {
            TrainingPlan trainingPlan = trainingPlanService.uploadTrainingPlan(file, name, description, competitionId);
            return ResponseEntity.ok(trainingPlan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingPlan> updateTrainingPlan(@PathVariable Long id, 
                                                          @Valid @RequestBody TrainingPlan trainingPlan) {
        TrainingPlan existingPlan = trainingPlanService.findById(id);
        if (existingPlan != null) {
            trainingPlan.setId(id);
            TrainingPlan updatedPlan = trainingPlanService.save(trainingPlan);
            return ResponseEntity.ok(updatedPlan);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrainingPlan(@PathVariable Long id) {
        if (trainingPlanService.findById(id) != null) {
            trainingPlanService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}