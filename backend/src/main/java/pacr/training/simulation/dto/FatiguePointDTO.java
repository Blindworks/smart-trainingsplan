package pacr.training.simulation.dto;

import java.time.LocalDate;

public class FatiguePointDTO {

    private LocalDate date;
    private double fatigue;

    public FatiguePointDTO() {
    }

    public FatiguePointDTO(LocalDate date, double fatigue) {
        this.date = date;
        this.fatigue = fatigue;
    }

    private FatiguePointDTO(Builder builder) {
        this.date = builder.date;
        this.fatigue = builder.fatigue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getFatigue() {
        return fatigue;
    }

    public void setFatigue(double fatigue) {
        this.fatigue = fatigue;
    }

    public static class Builder {

        private LocalDate date;
        private double fatigue;

        private Builder() {
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder fatigue(double fatigue) {
            this.fatigue = fatigue;
            return this;
        }

        public FatiguePointDTO build() {
            return new FatiguePointDTO(this);
        }
    }
}
