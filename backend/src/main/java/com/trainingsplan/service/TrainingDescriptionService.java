package com.trainingsplan.service;

import com.trainingsplan.entity.TrainingDescription;
import com.trainingsplan.repository.TrainingDescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TrainingDescriptionService {

    @Autowired
    private TrainingDescriptionRepository trainingDescriptionRepository;

    public List<TrainingDescription> findAll() {
        return trainingDescriptionRepository.findAll();
    }

    public TrainingDescription findById(Long id) {
        return trainingDescriptionRepository.findById(id).orElse(null);
    }

    public Optional<TrainingDescription> findByName(String name) {
        return trainingDescriptionRepository.findByName(name);
    }

    public TrainingDescription save(TrainingDescription trainingDescription) {
        return trainingDescriptionRepository.save(trainingDescription);
    }

    public void deleteById(Long id) {
        trainingDescriptionRepository.deleteById(id);
    }

    public TrainingDescription createOrUpdate(TrainingDescription trainingDescription) {
        if (trainingDescription.getId() != null) {
            TrainingDescription existing = findById(trainingDescription.getId());
            if (existing != null) {
                existing.setName(trainingDescription.getName());
                existing.setDetailedInstructions(trainingDescription.getDetailedInstructions());
                existing.setWarmupInstructions(trainingDescription.getWarmupInstructions());
                existing.setCooldownInstructions(trainingDescription.getCooldownInstructions());
                existing.setEquipment(trainingDescription.getEquipment());
                existing.setTips(trainingDescription.getTips());
                existing.setEstimatedDurationMinutes(trainingDescription.getEstimatedDurationMinutes());
                existing.setDifficultyLevel(trainingDescription.getDifficultyLevel());
                return save(existing);
            }
        }
        return save(trainingDescription);
    }
}