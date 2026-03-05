package com.trainingsplan.controller;

import com.trainingsplan.dto.TrainingImpactRequest;
import com.trainingsplan.service.TrainingImpactService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pacr.training.simulation.dto.WorkoutImpactDTO;

@RestController
@RequestMapping("/api/training")
public class TrainingImpactController {

    private final TrainingImpactService trainingImpactService;

    public TrainingImpactController(TrainingImpactService trainingImpactService) {
        this.trainingImpactService = trainingImpactService;
    }

    @PostMapping("/impact")
    public ResponseEntity<WorkoutImpactDTO> predictImpact(@Valid @RequestBody TrainingImpactRequest request) {
        WorkoutImpactDTO impact = trainingImpactService.predictImpact(request.getUserId(), request.getWorkout());
        return ResponseEntity.ok(impact);
    }
}
