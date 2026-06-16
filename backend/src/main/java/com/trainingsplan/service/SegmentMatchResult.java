package com.trainingsplan.service;

import java.util.List;

/** Outcome of matching an uploaded track against a segment's start/end gates. */
public class SegmentMatchResult {

    private final boolean matched;
    private final String rejectionReason;
    private final int elapsedSeconds;
    private final double distanceKm;
    private final double avgSpeedKmh;
    private final int avgPaceSecondsPerKm;
    /** Cropped points, each {lat, lng, ele (0 if absent), relativeSeconds}. */
    private final List<double[]> croppedTrack;

    private SegmentMatchResult(boolean matched, String rejectionReason, int elapsedSeconds,
                               double distanceKm, double avgSpeedKmh, int avgPaceSecondsPerKm,
                               List<double[]> croppedTrack) {
        this.matched = matched;
        this.rejectionReason = rejectionReason;
        this.elapsedSeconds = elapsedSeconds;
        this.distanceKm = distanceKm;
        this.avgSpeedKmh = avgSpeedKmh;
        this.avgPaceSecondsPerKm = avgPaceSecondsPerKm;
        this.croppedTrack = croppedTrack;
    }

    public static SegmentMatchResult rejected(String reason) {
        return new SegmentMatchResult(false, reason, 0, 0, 0, 0, List.of());
    }

    public static SegmentMatchResult matched(int elapsedSeconds, double distanceKm, double avgSpeedKmh,
                                             int avgPaceSecondsPerKm, List<double[]> croppedTrack) {
        return new SegmentMatchResult(true, null, elapsedSeconds, distanceKm, avgSpeedKmh,
                avgPaceSecondsPerKm, croppedTrack);
    }

    public boolean isMatched() { return matched; }
    public String getRejectionReason() { return rejectionReason; }
    public int getElapsedSeconds() { return elapsedSeconds; }
    public double getDistanceKm() { return distanceKm; }
    public double getAvgSpeedKmh() { return avgSpeedKmh; }
    public int getAvgPaceSecondsPerKm() { return avgPaceSecondsPerKm; }
    public List<double[]> getCroppedTrack() { return croppedTrack; }
}
