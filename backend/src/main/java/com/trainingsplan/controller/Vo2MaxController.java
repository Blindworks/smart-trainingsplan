package com.trainingsplan.controller;

import com.trainingsplan.dto.StravaActivityDto;
import com.trainingsplan.service.Vo2MaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vo2max")
@CrossOrigin(origins = "http://localhost:4200")
public class Vo2MaxController {

    private final Vo2MaxService vo2MaxService;

    public Vo2MaxController(Vo2MaxService vo2MaxService) {
        this.vo2MaxService = vo2MaxService;
    }

    @PostMapping("/estimate")
    public ResponseEntity<?> estimateFromActivity(@RequestBody StravaActivityDto activity) {
        Optional<Double> vo2Max = vo2MaxService.calculateFromActivity(activity);
        if (vo2Max.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "VO2max konnte nicht berechnet werden. Erwartet wird eine Laufaktivitaet mit gueltiger Distanz und Zeit."
            ));
        }

        return ResponseEntity.ok(Map.of("vo2max", vo2Max.get()));
    }
}
