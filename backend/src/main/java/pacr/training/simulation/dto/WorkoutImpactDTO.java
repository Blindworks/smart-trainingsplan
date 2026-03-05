package pacr.training.simulation.dto;

public class WorkoutImpactDTO {

    private double predictedTRIMP;
    private double fatigueIncrease;
    private double predictedFatigue;
    private int recoveryHours;
    private InjuryRisk injuryRisk;

    public WorkoutImpactDTO() {
    }

    public WorkoutImpactDTO(double predictedTRIMP, double fatigueIncrease, double predictedFatigue,
                            int recoveryHours, InjuryRisk injuryRisk) {
        this.predictedTRIMP = predictedTRIMP;
        this.fatigueIncrease = fatigueIncrease;
        this.predictedFatigue = predictedFatigue;
        this.recoveryHours = recoveryHours;
        this.injuryRisk = injuryRisk;
    }

    private WorkoutImpactDTO(Builder builder) {
        this.predictedTRIMP = builder.predictedTRIMP;
        this.fatigueIncrease = builder.fatigueIncrease;
        this.predictedFatigue = builder.predictedFatigue;
        this.recoveryHours = builder.recoveryHours;
        this.injuryRisk = builder.injuryRisk;
    }

    public static Builder builder() {
        return new Builder();
    }

    public double getPredictedTRIMP() {
        return predictedTRIMP;
    }

    public void setPredictedTRIMP(double predictedTRIMP) {
        this.predictedTRIMP = predictedTRIMP;
    }

    public double getFatigueIncrease() {
        return fatigueIncrease;
    }

    public void setFatigueIncrease(double fatigueIncrease) {
        this.fatigueIncrease = fatigueIncrease;
    }

    public double getPredictedFatigue() {
        return predictedFatigue;
    }

    public void setPredictedFatigue(double predictedFatigue) {
        this.predictedFatigue = predictedFatigue;
    }

    public int getRecoveryHours() {
        return recoveryHours;
    }

    public void setRecoveryHours(int recoveryHours) {
        this.recoveryHours = recoveryHours;
    }

    public InjuryRisk getInjuryRisk() {
        return injuryRisk;
    }

    public void setInjuryRisk(InjuryRisk injuryRisk) {
        this.injuryRisk = injuryRisk;
    }

    public enum InjuryRisk {
        LOW,
        MEDIUM,
        HIGH
    }

    public static class Builder {

        private double predictedTRIMP;
        private double fatigueIncrease;
        private double predictedFatigue;
        private int recoveryHours;
        private InjuryRisk injuryRisk;

        private Builder() {
        }

        public Builder predictedTRIMP(double predictedTRIMP) {
            this.predictedTRIMP = predictedTRIMP;
            return this;
        }

        public Builder fatigueIncrease(double fatigueIncrease) {
            this.fatigueIncrease = fatigueIncrease;
            return this;
        }

        public Builder predictedFatigue(double predictedFatigue) {
            this.predictedFatigue = predictedFatigue;
            return this;
        }

        public Builder recoveryHours(int recoveryHours) {
            this.recoveryHours = recoveryHours;
            return this;
        }

        public Builder injuryRisk(InjuryRisk injuryRisk) {
            this.injuryRisk = injuryRisk;
            return this;
        }

        public WorkoutImpactDTO build() {
            return new WorkoutImpactDTO(this);
        }
    }
}
