package com.trainingsplan.repository;

import com.trainingsplan.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    List<Competition> findByDateAfter(LocalDate date);
    
    List<Competition> findByDateBefore(LocalDate date);
    
    @Query("SELECT c FROM Competition c WHERE c.date BETWEEN :startDate AND :endDate")
    List<Competition> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Competition> findByNameContainingIgnoreCase(String name);

    List<Competition> findByUserId(Long userId);

    Optional<Competition> findByIdAndUserId(Long id, Long userId);
}