package com.trainingsplan.service;

import com.trainingsplan.dto.CompetitionDto;
import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.entity.TrainingWeek;
import com.trainingsplan.repository.CompetitionRegistrationRepository;
import com.trainingsplan.repository.CompetitionRepository;
import com.trainingsplan.repository.TrainingWeekRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CompetitionService {

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionRegistrationRepository registrationRepository;

    @Autowired
    private TrainingWeekRepository trainingWeekRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public List<CompetitionDto> findAll() {
        Long userId = securityUtils.getCurrentUserId();
        List<Competition> all = competitionRepository.findAll();
        return all.stream()
                .map(c -> {
                    CompetitionRegistration reg = userId != null
                            ? registrationRepository.findByCompetitionIdAndUserId(c.getId(), userId).orElse(null)
                            : null;
                    return new CompetitionDto(c, reg);
                })
                .collect(Collectors.toList());
    }

    public CompetitionDto findById(Long id) {
        Competition competition = competitionRepository.findById(id).orElse(null);
        if (competition == null) return null;
        Long userId = securityUtils.getCurrentUserId();
        CompetitionRegistration reg = userId != null
                ? registrationRepository.findByCompetitionIdAndUserId(id, userId).orElse(null)
                : null;
        return new CompetitionDto(competition, reg);
    }

    /** For internal use where the raw entity is needed (e.g. TrainingPlanService). */
    public Competition findEntityById(Long id) {
        return competitionRepository.findById(id).orElse(null);
    }

    public CompetitionDto save(Competition competition) {
        Competition saved = competitionRepository.save(competition);
        Long userId = securityUtils.getCurrentUserId();
        CompetitionRegistration reg = userId != null
                ? registrationRepository.findByCompetitionIdAndUserId(saved.getId(), userId).orElse(null)
                : null;
        return new CompetitionDto(saved, reg);
    }

    public void deleteById(Long id) {
        competitionRepository.deleteById(id);
    }

    public CompetitionRegistration updateRegistration(Long competitionId, String ranking,
                                                      String targetTime, Boolean registeredWithOrganizer) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new RuntimeException("Not authenticated");
        CompetitionRegistration reg = registrationRepository
                .findByCompetitionIdAndUserId(competitionId, userId)
                .orElseThrow(() -> new RuntimeException("Not registered for competition: " + competitionId));
        if (ranking != null) reg.setRanking(ranking);
        if (targetTime != null) reg.setTargetTime(targetTime);
        if (registeredWithOrganizer != null) reg.setRegisteredWithOrganizer(registeredWithOrganizer);
        return registrationRepository.save(reg);
    }

    public CompetitionRegistration register(Long competitionId, String targetTime, Boolean registeredWithOrganizer, String ranking) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("Competition not found: " + competitionId));
        var user = securityUtils.getCurrentUser();
        if (user == null) throw new RuntimeException("Not authenticated");
        Optional<CompetitionRegistration> existing = registrationRepository
                .findByCompetitionIdAndUserId(competitionId, user.getId());
        if (existing.isPresent()) return existing.get();
        CompetitionRegistration reg = new CompetitionRegistration(competition, user);
        if (targetTime != null) reg.setTargetTime(targetTime);
        if (registeredWithOrganizer != null) reg.setRegisteredWithOrganizer(registeredWithOrganizer);
        if (ranking != null) reg.setRanking(ranking);
        return registrationRepository.save(reg);
    }

    public void unregister(Long competitionId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new RuntimeException("Not authenticated");
        registrationRepository.findByCompetitionIdAndUserId(competitionId, userId)
                .ifPresent(registrationRepository::delete);
    }

    public CompetitionDto generateTrainingWeeks(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId).orElse(null);
        if (competition == null) return null;

        LocalDate competitionDate = competition.getDate();
        LocalDate today = LocalDate.now();

        LocalDate competitionSunday = competitionDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long weeksBetween = ChronoUnit.WEEKS.between(currentMonday, competitionSunday);

        List<TrainingWeek> weeks = new ArrayList<>();
        int maxTrainingWeeks = 12;

        for (int i = 0; i < Math.min(weeksBetween, maxTrainingWeeks); i++) {
            LocalDate weekSunday = competitionSunday.minusWeeks(i);
            LocalDate weekMonday = weekSunday.minusDays(6);
            if (!weekSunday.isBefore(today)) {
                int weekNumber = maxTrainingWeeks - i;
                TrainingWeek week = new TrainingWeek(weekNumber, weekMonday, weekSunday, competition);
                weeks.add(week);
            }
        }

        weeks.sort((w1, w2) -> w1.getStartDate().compareTo(w2.getStartDate()));
        trainingWeekRepository.saveAll(weeks);
        competition.setTrainingWeeks(weeks);
        competitionRepository.save(competition);

        Long userId = securityUtils.getCurrentUserId();
        CompetitionRegistration reg = userId != null
                ? registrationRepository.findByCompetitionIdAndUserId(competitionId, userId).orElse(null)
                : null;
        return new CompetitionDto(competition, reg);
    }
}
