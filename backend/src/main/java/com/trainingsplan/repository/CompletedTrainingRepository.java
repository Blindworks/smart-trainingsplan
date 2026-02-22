package com.trainingsplan.repository;

import com.trainingsplan.entity.CompletedTraining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompletedTrainingRepository extends JpaRepository<CompletedTraining, Long> {
    
    List<CompletedTraining> findByTrainingDateOrderByUploadDateDesc(LocalDate trainingDate);
    
    List<CompletedTraining> findByTrainingDateBetweenOrderByTrainingDate(LocalDate startDate, LocalDate endDate);
    
    Optional<CompletedTraining> findTopByTrainingDateOrderByUploadDateDesc(LocalDate trainingDate);
    
    List<CompletedTraining> findBySportOrderByTrainingDateDesc(String sport);

    long countByTrainingDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByStravaActivityId(Long stravaActivityId);

    Optional<CompletedTraining> findByStravaActivityId(Long stravaActivityId);

    List<CompletedTraining> findByUserId(Long userId);

    @Query("SELECT c FROM CompletedTraining c WHERE c.user.id = :userId OR c.user IS NULL")
    List<CompletedTraining> findByUserIdOrUserIsNull(@Param("userId") Long userId);

    List<CompletedTraining> findByUserIdAndTrainingDateBetweenOrderByTrainingDate(Long userId, LocalDate startDate, LocalDate endDate);

    List<CompletedTraining> findByUserIdAndTrainingDateOrderByUploadDateDesc(Long userId, LocalDate trainingDate);

    Optional<CompletedTraining> findTopByUserIdAndTrainingDateOrderByUploadDateDesc(Long userId, LocalDate trainingDate);

    Optional<CompletedTraining> findTopByUserIdAndSportContainingIgnoreCaseOrderByTrainingDateDescUploadDateDesc(Long userId, String sport);
}
