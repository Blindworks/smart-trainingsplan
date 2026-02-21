package com.trainingsplan.service;

import com.trainingsplan.dto.StravaActivityDto;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Vo2MaxServiceTest {

    private final Vo2MaxService vo2MaxService = new Vo2MaxService();

    @Test
    void calculateFromActivity_returnsValueForRunningActivity() {
        StravaActivityDto activity = new StravaActivityDto();
        activity.setType("Run");
        activity.setDistanceMeters(10_000.0);
        activity.setMovingTimeSeconds(2_700);

        Optional<Double> result = vo2MaxService.calculateFromActivity(activity);

        assertTrue(result.isPresent());
        assertEquals(45.26, result.get(), 0.01);
    }

    @Test
    void calculateFromActivity_returnsEmptyForNonRunningActivity() {
        StravaActivityDto activity = new StravaActivityDto();
        activity.setType("Ride");
        activity.setDistanceMeters(10_000.0);
        activity.setMovingTimeSeconds(2_700);

        Optional<Double> result = vo2MaxService.calculateFromActivity(activity);

        assertTrue(result.isEmpty());
    }

    @Test
    void calculate_returnsEmptyForInvalidValues() {
        assertTrue(vo2MaxService.calculate(null, 1800).isEmpty());
        assertTrue(vo2MaxService.calculate(5000.0, null).isEmpty());
        assertTrue(vo2MaxService.calculate(0.0, 1800).isEmpty());
        assertTrue(vo2MaxService.calculate(5000.0, 0).isEmpty());
    }
}
