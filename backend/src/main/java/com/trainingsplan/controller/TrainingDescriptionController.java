package com.trainingsplan.controller;

import com.trainingsplan.entity.TrainingDescription;
import com.trainingsplan.service.TrainingDescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/training-descriptions")
@CrossOrigin(origins = "http://localhost:3000")
public class TrainingDescriptionController {

    @Autowired
    private TrainingDescriptionService trainingDescriptionService;

    @GetMapping
    public ResponseEntity<List<TrainingDescription>> getAllTrainingDescriptions() {
        return ResponseEntity.ok(trainingDescriptionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingDescription> getTrainingDescriptionById(@PathVariable Long id) {
        TrainingDescription trainingDescription = trainingDescriptionService.findById(id);
        if (trainingDescription != null) {
            return ResponseEntity.ok(trainingDescription);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<TrainingDescription> getTrainingDescriptionByName(@PathVariable String name) {
        return trainingDescriptionService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TrainingDescription> createTrainingDescription(@Valid @RequestBody TrainingDescription trainingDescription) {
        TrainingDescription savedDescription = trainingDescriptionService.save(trainingDescription);
        return ResponseEntity.ok(savedDescription);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingDescription> updateTrainingDescription(@PathVariable Long id, 
                                                                        @Valid @RequestBody TrainingDescription trainingDescription) {
        TrainingDescription existingDescription = trainingDescriptionService.findById(id);
        if (existingDescription != null) {
            trainingDescription.setId(id);
            TrainingDescription updatedDescription = trainingDescriptionService.createOrUpdate(trainingDescription);
            return ResponseEntity.ok(updatedDescription);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrainingDescription(@PathVariable Long id) {
        if (trainingDescriptionService.findById(id) != null) {
            trainingDescriptionService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}