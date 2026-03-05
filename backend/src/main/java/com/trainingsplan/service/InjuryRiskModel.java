package com.trainingsplan.service;

import org.springframework.stereotype.Service;
import pacr.training.simulation.dto.WorkoutImpactDTO.InjuryRisk;

@Service
public class InjuryRiskModel {

    public InjuryRisk predictRisk(double predictedFatigue) {
        if (predictedFatigue > 0.85) {
            return InjuryRisk.HIGH;
        }
        if (predictedFatigue > 0.75) {
            return InjuryRisk.MEDIUM;
        }
        return InjuryRisk.LOW;
    }
}
