package com.trainingsplan.service;

import com.trainingsplan.dto.AthleteState;
import com.trainingsplan.dto.Workout;
import org.springframework.stereotype.Service;
import pacr.training.simulation.dto.WorkoutImpactDTO;
import pacr.training.simulation.dto.WorkoutImpactDTO.InjuryRisk;

@Service
public class TrainingImpactEngine {

    private final TrainingLoadCalculator trainingLoadCalculator;
    private final FatigueModel fatigueModel;
    private final RecoveryModel recoveryModel;
    private final InjuryRiskModel injuryRiskModel;

    public TrainingImpactEngine(
            TrainingLoadCalculator trainingLoadCalculator,
            FatigueModel fatigueModel,
            RecoveryModel recoveryModel,
            InjuryRiskModel injuryRiskModel
    ) {
        this.trainingLoadCalculator = trainingLoadCalculator;
        this.fatigueModel = fatigueModel;
        this.recoveryModel = recoveryModel;
        this.injuryRiskModel = injuryRiskModel;
    }

    public WorkoutImpactDTO predictImpact(Workout workout, AthleteState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }

        double predictedTRIMP = trainingLoadCalculator.calculateTRIMP(workout);
        double fatigueIncrease = fatigueModel.predictFatigueIncrease(predictedTRIMP);
        double predictedFatigue = fatigueModel.predictNextFatigue(state.fatigue(), fatigueIncrease);
        int recoveryHours = recoveryModel.calculateRecoveryHours(predictedTRIMP);
        InjuryRisk injuryRisk = injuryRiskModel.predictRisk(predictedFatigue);

        return WorkoutImpactDTO.builder()
                .predictedTRIMP(predictedTRIMP)
                .fatigueIncrease(fatigueIncrease)
                .predictedFatigue(predictedFatigue)
                .recoveryHours(recoveryHours)
                .injuryRisk(injuryRisk)
                .build();
    }
}
