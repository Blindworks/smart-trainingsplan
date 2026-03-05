package com.trainingsplan.controller;

import com.trainingsplan.dto.TrainingImpactRequest;
import com.trainingsplan.dto.TrainingWeekSimulationRequest;
import com.trainingsplan.service.TrainingImpactService;
import com.trainingsplan.service.WeekSimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pacr.training.simulation.dto.WeekSimulationResultDTO;
import pacr.training.simulation.dto.WorkoutImpactDTO;

@RestController
@RequestMapping("/api/training")
public class TrainingImpactController {

    private final TrainingImpactService trainingImpactService;
    private final WeekSimulationService weekSimulationService;

    public TrainingImpactController(
            TrainingImpactService trainingImpactService,
            WeekSimulationService weekSimulationService
    ) {
        this.trainingImpactService = trainingImpactService;
        this.weekSimulationService = weekSimulationService;
    }

    @PostMapping("/impact")
    public ResponseEntity<WorkoutImpactDTO> predictImpact(@Valid @RequestBody TrainingImpactRequest request) {
        WorkoutImpactDTO impact = trainingImpactService.predictImpact(request.getUserId(), request.getWorkout());
        return ResponseEntity.ok(impact);
    }

    @PostMapping("/week/simulate")
    public ResponseEntity<WeekSimulationResultDTO> simulateTrainingWeek(
            @Valid @RequestBody TrainingWeekSimulationRequest request
    ) {
        WeekSimulationResultDTO simulation = weekSimulationService
                .simulateTrainingWeek(request.getUserId(), request.getWorkouts());
        return ResponseEntity.ok(simulation);
    }
}
