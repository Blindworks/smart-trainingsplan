package com.trainingsplan.controller;

import com.trainingsplan.entity.TrainingWeek;
import com.trainingsplan.repository.TrainingWeekRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-weeks")
@CrossOrigin(origins = "http://localhost:3000")
public class TrainingWeekController {

    @Autowired
    private TrainingWeekRepository trainingWeekRepository;

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<List<TrainingWeek>> getTrainingWeeksByCompetition(@PathVariable Long competitionId) {
        List<TrainingWeek> weeks = trainingWeekRepository.findByCompetitionIdOrderByWeekNumber(competitionId);
        return ResponseEntity.ok(weeks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingWeek> getTrainingWeekById(@PathVariable Long id) {
        return trainingWeekRepository.findById(id)
                .map(week -> ResponseEntity.ok(week))
                .orElse(ResponseEntity.notFound().build());
    }
}