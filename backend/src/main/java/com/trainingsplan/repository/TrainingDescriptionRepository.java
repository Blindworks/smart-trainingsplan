package com.trainingsplan.repository;

import com.trainingsplan.entity.TrainingDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingDescriptionRepository extends JpaRepository<TrainingDescription, Long> {
    Optional<TrainingDescription> findByName(String name);
}