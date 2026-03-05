package com.trainingsplan.service;

import com.trainingsplan.dto.RecentWorkoutDto;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecentWorkoutService {

    private static final int DEFAULT_LIMIT = 5;

    private final CompletedTrainingRepository completedTrainingRepository;
    private final ActivityMetricsRepository activityMetricsRepository;

    public RecentWorkoutService(CompletedTrainingRepository completedTrainingRepository,
                                ActivityMetricsRepository activityMetricsRepository) {
        this.completedTrainingRepository = completedTrainingRepository;
        this.activityMetricsRepository = activityMetricsRepository;
    }

    public List<RecentWorkoutDto> getRecentWorkouts(Long userId) {
        return getRecentWorkouts(userId, DEFAULT_LIMIT);
    }

    public List<RecentWorkoutDto> getRecentWorkouts(Long userId, int limit) {
        List<CompletedTraining> trainings = completedTrainingRepository
                .findByUserIdOrderByTrainingDateDescUploadDateDesc(userId, PageRequest.of(0, limit));

        List<Long> ids = trainings.stream().map(CompletedTraining::getId).toList();

        Map<Long, Double> trimpById = activityMetricsRepository.findByCompletedTrainingIdIn(ids).stream()
                .filter(am -> am.getTrimp() != null)
                .collect(Collectors.toMap(
                        am -> am.getCompletedTraining().getId(),
                        ActivityMetrics::getTrimp
                ));

        return trainings.stream()
                .map(ct -> new RecentWorkoutDto(
                        ct.getTrainingDate(),
                        resolveType(ct),
                        ct.getDistanceKm(),
                        trimpById.get(ct.getId())
                ))
                .toList();
    }

    private String resolveType(CompletedTraining ct) {
        if (ct.getTrainingType() != null) return ct.getTrainingType();
        return ct.getSport();
    }
}
