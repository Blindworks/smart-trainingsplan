package com.trainingsplan.dto;

import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionRegistration;

import java.time.LocalDate;

public class CompetitionDto {
    private Long id;
    private String name;
    private LocalDate date;
    private String description;
    private String type;
    private String location;
    private String ranking;
    private boolean registered;
    private Long registrationId;
    private Long trainingPlanId;
    private String trainingPlanName;

    public CompetitionDto(Competition competition, CompetitionRegistration registration) {
        this.id = competition.getId();
        this.name = competition.getName();
        this.date = competition.getDate();
        this.description = competition.getDescription();
        this.type = competition.getType();
        this.location = competition.getLocation();
        if (registration != null) {
            this.registered = true;
            this.registrationId = registration.getId();
            this.ranking = registration.getRanking();
            this.trainingPlanId = registration.getTrainingPlanId();
            this.trainingPlanName = registration.getTrainingPlanName();
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getLocation() { return location; }
    public String getRanking() { return ranking; }
    public boolean isRegistered() { return registered; }
    public Long getRegistrationId() { return registrationId; }
    public Long getTrainingPlanId() { return trainingPlanId; }
    public String getTrainingPlanName() { return trainingPlanName; }
}
