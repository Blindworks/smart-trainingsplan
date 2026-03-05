package pacr.training.simulation.dto;

import java.util.List;

public class WeekSimulationResultDTO {

    private List<FatiguePointDTO> fatigueTimeline;
    private double peakFatigue;
    private List<String> riskFlags;

    public WeekSimulationResultDTO() {
    }

    public WeekSimulationResultDTO(List<FatiguePointDTO> fatigueTimeline, double peakFatigue, List<String> riskFlags) {
        this.fatigueTimeline = fatigueTimeline;
        this.peakFatigue = peakFatigue;
        this.riskFlags = riskFlags;
    }

    private WeekSimulationResultDTO(Builder builder) {
        this.fatigueTimeline = builder.fatigueTimeline;
        this.peakFatigue = builder.peakFatigue;
        this.riskFlags = builder.riskFlags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<FatiguePointDTO> getFatigueTimeline() {
        return fatigueTimeline;
    }

    public void setFatigueTimeline(List<FatiguePointDTO> fatigueTimeline) {
        this.fatigueTimeline = fatigueTimeline;
    }

    public double getPeakFatigue() {
        return peakFatigue;
    }

    public void setPeakFatigue(double peakFatigue) {
        this.peakFatigue = peakFatigue;
    }

    public List<String> getRiskFlags() {
        return riskFlags;
    }

    public void setRiskFlags(List<String> riskFlags) {
        this.riskFlags = riskFlags;
    }

    public static class Builder {

        private List<FatiguePointDTO> fatigueTimeline;
        private double peakFatigue;
        private List<String> riskFlags;

        private Builder() {
        }

        public Builder fatigueTimeline(List<FatiguePointDTO> fatigueTimeline) {
            this.fatigueTimeline = fatigueTimeline;
            return this;
        }

        public Builder peakFatigue(double peakFatigue) {
            this.peakFatigue = peakFatigue;
            return this;
        }

        public Builder riskFlags(List<String> riskFlags) {
            this.riskFlags = riskFlags;
            return this;
        }

        public WeekSimulationResultDTO build() {
            return new WeekSimulationResultDTO(this);
        }
    }
}
