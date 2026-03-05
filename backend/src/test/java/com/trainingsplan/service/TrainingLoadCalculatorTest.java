package com.trainingsplan.service;

import com.trainingsplan.dto.Workout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainingLoadCalculatorTest {

    private TrainingLoadCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new TrainingLoadCalculator();
    }

    @Test
    void calculateTRIMP_z1() {
        Workout workout = new Workout(LocalDate.now(), "Easy Z1 run", null, 40, null, null);
        assertEquals(40.0, calculator.calculateTRIMP(workout), 1e-9);
    }

    @Test
    void calculateTRIMP_z2() {
        Workout workout = new Workout(LocalDate.now(), "Endurance Z2", null, 45, null, null);
        assertEquals(90.0, calculator.calculateTRIMP(workout), 1e-9);
    }

    @Test
    void calculateTRIMP_z3() {
        Workout workout = new Workout(LocalDate.now(), "Tempo Z3", null, 30, null, null);
        assertEquals(90.0, calculator.calculateTRIMP(workout), 1e-9);
    }

    @Test
    void calculateTRIMP_z4() {
        Workout workout = new Workout(LocalDate.now(), "Threshold Z4", null, 25, null, null);
        assertEquals(100.0, calculator.calculateTRIMP(workout), 1e-9);
    }

    @Test
    void calculateTRIMP_z5() {
        Workout workout = new Workout(LocalDate.now(), "Intervals Z5", null, 20, null, null);
        assertEquals(100.0, calculator.calculateTRIMP(workout), 1e-9);
    }

    @Test
    void calculateTRIMP_throwsWhenZoneMissing() {
        Workout workout = new Workout(LocalDate.now(), "Easy run", null, 30, null, null);
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateTRIMP(workout));
    }
}
