package com.trainingsplan.repository;

import com.trainingsplan.entity.CompletedTraining;
import org.springframework.data.jpa.repository.JpaRepository;
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
}