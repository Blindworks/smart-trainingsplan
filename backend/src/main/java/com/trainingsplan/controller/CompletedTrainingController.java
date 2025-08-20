package com.trainingsplan.controller;

import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.service.CompletedTrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/completed-trainings")
@CrossOrigin(origins = "http://localhost:3000")
public class CompletedTrainingController {

    @Autowired
    private CompletedTrainingService completedTrainingService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFitFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate trainingDate) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Datei ist leer");
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".fit")) {
                return ResponseEntity.badRequest().body("Nur .FIT-Dateien sind erlaubt");
            }
            
            CompletedTraining training = completedTrainingService.uploadAndParseFitFile(file, trainingDate);
            return ResponseEntity.ok(training);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Verarbeiten der FIT-Datei: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unerwarteter Fehler: " + e.getMessage());
        }
    }

    @GetMapping("/by-date")
    public ResponseEntity<List<CompletedTraining>> getCompletedTrainingsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<CompletedTraining> trainings = completedTrainingService.getCompletedTrainingsByDate(date);
        return ResponseEntity.ok(trainings);
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<CompletedTraining>> getCompletedTrainingsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<CompletedTraining> trainings = completedTrainingService.getCompletedTrainingsBetweenDates(startDate, endDate);
        return ResponseEntity.ok(trainings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompletedTraining> getCompletedTrainingById(@PathVariable Long id) {
        // Diese Methode kann später implementiert werden
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompletedTraining(@PathVariable Long id) {
        // Diese Methode kann später implementiert werden
        return ResponseEntity.notFound().build();
    }
}