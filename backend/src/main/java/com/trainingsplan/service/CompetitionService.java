package com.trainingsplan.service;

import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.TrainingWeek;
import com.trainingsplan.repository.CompetitionRepository;
import com.trainingsplan.repository.TrainingWeekRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompetitionService {

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private TrainingWeekRepository trainingWeekRepository;

    public List<Competition> findAll() {
        return competitionRepository.findAll();
    }

    public Competition findById(Long id) {
        return competitionRepository.findById(id).orElse(null);
    }

    public Competition save(Competition competition) {
        return competitionRepository.save(competition);
    }

    public void deleteById(Long id) {
        competitionRepository.deleteById(id);
    }

    public Competition generateTrainingWeeks(Long competitionId) {
        Competition competition = findById(competitionId);
        if (competition == null) {
            return null;
        }

        LocalDate competitionDate = competition.getDate();
        LocalDate today = LocalDate.now();
        
        // Finde den Sonntag der Wettkampfwoche (meist ist der Wettkampf schon ein Sonntag)
        LocalDate competitionSunday = competitionDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        // Finde den Montag der aktuellen Woche
        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        // Berechne Anzahl der Wochen vom aktuellen Montag bis zum Wettkampf-Sonntag
        long weeksBetween = ChronoUnit.WEEKS.between(currentMonday, competitionSunday);
        
        List<TrainingWeek> weeks = new ArrayList<>();
        
        // Standardmäßig 12 Wochen Training planen (kann später durch Trainingspläne überschrieben werden)
        int maxTrainingWeeks = 12;
        
        // Erstelle Wochen rückwärts vom Wettkampf
        for (int i = 0; i < Math.min(weeksBetween, maxTrainingWeeks); i++) {
            // Woche i: 0 = Wettkampfwoche, 1 = eine Woche vorher, etc.
            LocalDate weekSunday = competitionSunday.minusWeeks(i);
            LocalDate weekMonday = weekSunday.minusDays(6); // Montag = Sonntag - 6 Tage
            
            // Nur Wochen erstellen, die nicht in der Vergangenheit liegen
            if (!weekSunday.isBefore(today)) {
                // Wochennummer: Wettkampfwoche = maxTrainingWeeks, rückwärts zählen
                int weekNumber = maxTrainingWeeks - i;
                TrainingWeek week = new TrainingWeek(weekNumber, weekMonday, weekSunday, competition);
                weeks.add(week);
            }
        }
        
        // Sortiere Wochen chronologisch (früheste zuerst)
        weeks.sort((w1, w2) -> w1.getStartDate().compareTo(w2.getStartDate()));
        
        trainingWeekRepository.saveAll(weeks);
        competition.setTrainingWeeks(weeks);
        
        return competitionRepository.save(competition);
    }
}