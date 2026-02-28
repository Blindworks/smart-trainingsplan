package com.trainingsplan.controller;

import com.trainingsplan.dto.CompetitionDto;
import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.service.CompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionController {

    @Autowired
    private CompetitionService competitionService;

    @GetMapping
    public ResponseEntity<List<CompetitionDto>> getAllCompetitions() {
        return ResponseEntity.ok(competitionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionDto> getCompetitionById(@PathVariable Long id) {
        CompetitionDto dto = competitionService.findById(id);
        if (dto != null) return ResponseEntity.ok(dto);
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<CompetitionDto> createCompetition(@Valid @RequestBody Competition competition) {
        return ResponseEntity.ok(competitionService.save(competition));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionDto> updateCompetition(@PathVariable Long id,
                                                            @Valid @RequestBody Competition competition) {
        if (competitionService.findEntityById(id) == null) return ResponseEntity.notFound().build();
        competition.setId(id);
        return ResponseEntity.ok(competitionService.save(competition));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        if (competitionService.findEntityById(id) == null) return ResponseEntity.notFound().build();
        competitionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<CompetitionRegistration> registerForCompetition(@PathVariable Long id) {
        try {
            CompetitionRegistration reg = competitionService.register(id);
            return ResponseEntity.ok(reg);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/register")
    public ResponseEntity<CompetitionRegistration> updateRegistration(@PathVariable Long id,
                                                                      @RequestBody Map<String, String> body) {
        try {
            CompetitionRegistration reg = competitionService.updateRegistration(id, body.get("ranking"));
            return ResponseEntity.ok(reg);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<Void> unregisterFromCompetition(@PathVariable Long id) {
        competitionService.unregister(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{competitionId}/generate-weeks")
    public ResponseEntity<CompetitionDto> generateTrainingWeeks(@PathVariable Long competitionId) {
        CompetitionDto dto = competitionService.generateTrainingWeeks(competitionId);
        if (dto != null) return ResponseEntity.ok(dto);
        return ResponseEntity.notFound().build();
    }
}
