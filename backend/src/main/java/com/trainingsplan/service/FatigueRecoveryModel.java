package com.trainingsplan.service;

import org.springframework.stereotype.Service;

@Service
public class FatigueRecoveryModel {

    private static final double DAILY_RECOVERY = 0.05;
    private static final double MIN_FATIGUE = 0.30;

    public double applyDailyRecovery(double fatigue) {
        return Math.max(MIN_FATIGUE, fatigue - DAILY_RECOVERY);
    }
}
