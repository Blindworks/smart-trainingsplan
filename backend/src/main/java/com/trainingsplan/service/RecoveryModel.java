package com.trainingsplan.service;

import org.springframework.stereotype.Service;

@Service
public class RecoveryModel {

    public int calculateRecoveryHours(double trimp) {
        if (trimp < 40.0) {
            return 12;
        }
        if (trimp < 80.0) {
            return 24;
        }
        if (trimp < 120.0) {
            return 36;
        }
        return 48;
    }
}
