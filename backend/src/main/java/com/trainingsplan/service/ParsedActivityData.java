package com.trainingsplan.service;

import com.trainingsplan.entity.CompletedTraining;

import java.util.List;

public class ParsedActivityData {
    public CompletedTraining training;
    public List<Integer> timeSeconds;
    public List<Integer> heartRates;
    public List<double[]> latLngPoints;
    /** Per-trackpoint elevation in metres; entries may be null when the GPX has no {@code <ele>}. */
    public List<Double> elevations;
}
