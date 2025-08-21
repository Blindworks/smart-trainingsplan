package com.trainingsplan.controller;

import com.trainingsplan.dto.DailyTrainingCompletionDto;
import com.trainingsplan.service.TrainingCompletionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/training-completion")
@CrossOrigin(origins = "http://localhost:3000")
public class TrainingCompletionController {
    
    @Autowired
    private TrainingCompletionService trainingCompletionService;
    
    @GetMapping("/today")
    public ResponseEntity<DailyTrainingCompletionDto> getTodayCompletion() {
        DailyTrainingCompletionDto completion = trainingCompletionService.getTodayTrainingCompletion();
        return ResponseEntity.ok(completion);
    }
    
    @GetMapping("/date/{date}")
    public ResponseEntity<DailyTrainingCompletionDto> getCompletionByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyTrainingCompletionDto completion = trainingCompletionService.getDailyTrainingCompletion(date);
        return ResponseEntity.ok(completion);
    }
    
    @GetMapping("/week")
    public ResponseEntity<List<DailyTrainingCompletionDto>> getWeeklyCompletion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DailyTrainingCompletionDto> completions = trainingCompletionService.getWeeklyTrainingCompletion(startDate, endDate);
        return ResponseEntity.ok(completions);
    }
    
    @GetMapping("/current-week")
    public ResponseEntity<List<DailyTrainingCompletionDto>> getCurrentWeekCompletion() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        List<DailyTrainingCompletionDto> completions = trainingCompletionService.getWeeklyTrainingCompletion(startOfWeek, endOfWeek);
        return ResponseEntity.ok(completions);
    }
}