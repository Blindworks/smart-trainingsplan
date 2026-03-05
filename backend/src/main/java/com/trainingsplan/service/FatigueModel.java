package com.trainingsplan.service;

import org.springframework.stereotype.Service;

@Service
public class FatigueModel {

    public double predictFatigueIncrease(double trimp) {
        return trimp / 800.0;
    }

    public double predictNextFatigue(double currentFatigue, double fatigueIncrease) {
        double nextFatigue = currentFatigue + fatigueIncrease;
        return Math.max(0.0, Math.min(1.0, nextFatigue));
    }
}