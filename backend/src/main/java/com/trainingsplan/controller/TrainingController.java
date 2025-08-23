package com.trainingsplan.controller;

import com.trainingsplan.entity.Training;
import com.trainingsplan.service.TrainingService;
import com.trainingsplan.dto.TrainingFeedbackDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainings")
@CrossOrigin(origins = "http://localhost:4200")
public class TrainingController {

    @Autowired
    private TrainingService trainingService;

    @GetMapping
    public ResponseEntity<List<Training>> getAllTrainings() {
        return ResponseEntity.ok(trainingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Training> getTrainingById(@PathVariable Long id) {
        Training training = trainingService.findById(id);
        if (training != null) {
            return ResponseEntity.ok(training);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/week/{weekId}")
    public ResponseEntity<List<Training>> getTrainingsByWeek(@PathVariable Long weekId) {
        return ResponseEntity.ok(trainingService.findByTrainingWeekId(weekId));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Training>> getTrainingsByDate(@PathVariable String date) {
        LocalDate trainingDate = LocalDate.parse(date);
        return ResponseEntity.ok(trainingService.findByDate(trainingDate));
    }

    @GetMapping("/competition/{competitionId}/date/{date}")
    public ResponseEntity<List<Training>> getTrainingsByCompetitionAndDate(@PathVariable Long competitionId, 
                                                                          @PathVariable String date) {
        LocalDate trainingDate = LocalDate.parse(date);
        return ResponseEntity.ok(trainingService.findByCompetitionIdAndDate(competitionId, trainingDate));
    }

    @GetMapping("/competition/{competitionId}/mixed")
    public ResponseEntity<List<Training>> getMixedTrainings(@PathVariable Long competitionId,
                                                           @RequestParam List<Long> planIds,
                                                           @RequestParam String date) {
        LocalDate trainingDate = LocalDate.parse(date);
        List<Training> mixedTrainings = trainingService.generateMixedTraining(competitionId, planIds, trainingDate);
        return ResponseEntity.ok(mixedTrainings);
    }

    @PostMapping
    public ResponseEntity<Training> createTraining(@Valid @RequestBody Training training) {
        Training savedTraining = trainingService.save(training);
        return ResponseEntity.ok(savedTraining);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Training> updateTraining(@PathVariable Long id, 
                                                  @Valid @RequestBody Training training) {
        Training existingTraining = trainingService.findById(id);
        if (existingTraining != null) {
            training.setId(id);
            Training updatedTraining = trainingService.save(training);
            return ResponseEntity.ok(updatedTraining);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/feedback")
    public ResponseEntity<Training> updateTrainingFeedback(@PathVariable Long id, 
                                                          @Valid @RequestBody TrainingFeedbackDto feedback) {
        Training training = trainingService.updateTrainingFeedback(id, feedback.getIsCompleted(), feedback.getCompletionStatus());
        if (training != null) {
            return ResponseEntity.ok(training);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTraining(@PathVariable Long id) {
        if (trainingService.findById(id) != null) {
            trainingService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}