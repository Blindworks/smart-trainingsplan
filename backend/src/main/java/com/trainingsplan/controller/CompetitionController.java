package com.trainingsplan.controller;

import com.trainingsplan.entity.Competition;
import com.trainingsplan.service.CompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/competitions")
@CrossOrigin(origins = "http://localhost:4200")
public class CompetitionController {

    @Autowired
    private CompetitionService competitionService;

    @GetMapping
    public ResponseEntity<List<Competition>> getAllCompetitions() {
        return ResponseEntity.ok(competitionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Competition> getCompetitionById(@PathVariable Long id) {
        Competition competition = competitionService.findById(id);
        if (competition != null) {
            return ResponseEntity.ok(competition);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Competition> createCompetition(@Valid @RequestBody Competition competition) {
        Competition savedCompetition = competitionService.save(competition);
        return ResponseEntity.ok(savedCompetition);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Competition> updateCompetition(@PathVariable Long id, 
                                                        @Valid @RequestBody Competition competition) {
        Competition existingCompetition = competitionService.findById(id);
        if (existingCompetition != null) {
            competition.setId(id);
            Competition updatedCompetition = competitionService.save(competition);
            return ResponseEntity.ok(updatedCompetition);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        if (competitionService.findById(id) != null) {
            competitionService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{competitionId}/generate-weeks")
    public ResponseEntity<Competition> generateTrainingWeeks(@PathVariable Long competitionId) {
        Competition competition = competitionService.generateTrainingWeeks(competitionId);
        if (competition != null) {
            return ResponseEntity.ok(competition);
        }
        return ResponseEntity.notFound().build();
    }
}