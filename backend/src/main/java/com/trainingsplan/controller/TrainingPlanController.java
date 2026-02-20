package com.trainingsplan.controller;

import com.trainingsplan.dto.TrainingPlanDto;
import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.service.TrainingPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/training-plans")
@CrossOrigin(origins = "http://localhost:4200")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    public TrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    // -------------------------------------------------------------------------
    // Existing endpoints (return types changed to TrainingPlanDto)
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<TrainingPlanDto>> getAllTrainingPlans() {
        List<TrainingPlanDto> plans = trainingPlanService.findAll()
                .stream()
                .map(TrainingPlanDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingPlanDto> getTrainingPlanById(@PathVariable Long id) {
        TrainingPlan plan = trainingPlanService.findById(id);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new TrainingPlanDto(plan));
    }

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<List<TrainingPlanDto>> getTrainingPlansByCompetition(@PathVariable Long competitionId) {
        List<TrainingPlanDto> plans = trainingPlanService.findByCompetitionId(competitionId)
                .stream()
                .map(TrainingPlanDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(plans);
    }

    @PostMapping
    public ResponseEntity<TrainingPlanDto> createTrainingPlan(@Valid @RequestBody TrainingPlan trainingPlan) {
        TrainingPlan saved = trainingPlanService.save(trainingPlan);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TrainingPlanDto(saved));
    }

    @PostMapping("/upload")
    public ResponseEntity<TrainingPlanDto> uploadTrainingPlan(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("competitionId") Long competitionId) {
        try {
            TrainingPlan plan = trainingPlanService.uploadTrainingPlan(file, name, description, competitionId);
            return ResponseEntity.status(HttpStatus.CREATED).body(new TrainingPlanDto(plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingPlanDto> updateTrainingPlan(
            @PathVariable Long id,
            @Valid @RequestBody TrainingPlan trainingPlan) {
        if (trainingPlanService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        trainingPlan.setId(id);
        TrainingPlan updated = trainingPlanService.save(trainingPlan);
        return ResponseEntity.ok(new TrainingPlanDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrainingPlan(@PathVariable Long id) {
        if (trainingPlanService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        trainingPlanService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Template endpoints
    // -------------------------------------------------------------------------

    /**
     * Upload a plan JSON as a reusable template. No competition required.
     * No Training records are created at this point.
     */
    @PostMapping("/upload-template")
    public ResponseEntity<TrainingPlanDto> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "description", defaultValue = "") String description) {
        try {
            TrainingPlanDto dto = trainingPlanService.uploadAsTemplate(file, name, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Returns all plans stored as templates.
     */
    @GetMapping("/templates")
    public ResponseEntity<List<TrainingPlanDto>> getTemplates() {
        return ResponseEntity.ok(trainingPlanService.findAllTemplates());
    }

    /**
     * Assigns an existing plan (template or regular) to a competition.
     * Creates a new TrainingPlan with all Training records date-shifted to
     * align the last training with the competition date.
     */
    @PostMapping("/assign")
    public ResponseEntity<TrainingPlanDto> assignPlanToCompetition(
            @RequestParam Long planId,
            @RequestParam Long competitionId) {
        try {
            TrainingPlanDto dto = trainingPlanService.assignPlanToCompetition(planId, competitionId);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
