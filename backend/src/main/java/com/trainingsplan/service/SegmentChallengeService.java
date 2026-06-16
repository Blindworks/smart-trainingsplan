package com.trainingsplan.service;

import com.trainingsplan.dto.SegmentLeaderboardEntryDto;
import com.trainingsplan.entity.EffortKind;
import com.trainingsplan.entity.SegmentEffort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SegmentChallengeService {

    /** Builds a ranked leaderboard (rank 1 = fastest) with gap-to-leader, from any effort list. */
    public static List<SegmentLeaderboardEntryDto> buildLeaderboard(List<SegmentEffort> efforts) {
        List<SegmentEffort> sorted = new ArrayList<>(efforts);
        sorted.sort(Comparator.comparingInt(SegmentEffort::getElapsedSeconds));

        List<SegmentLeaderboardEntryDto> out = new ArrayList<>(sorted.size());
        Integer leaderTime = sorted.isEmpty() ? null : sorted.get(0).getElapsedSeconds();
        int rank = 0;
        for (SegmentEffort e : sorted) {
            rank++;
            int gap = leaderTime == null ? 0 : e.getElapsedSeconds() - leaderTime;
            out.add(new SegmentLeaderboardEntryDto(
                    e.getId(),
                    rank,
                    e.getDisplayName(),
                    e.getKind() != null ? e.getKind().name() : null,
                    e.getCategory() != null ? e.getCategory().name() : null,
                    e.getElapsedSeconds(),
                    formatElapsed(e.getElapsedSeconds()),
                    gap,
                    e.getAvgSpeedKmh(),
                    e.getAvgPaceSecondsPerKm(),
                    e.getKind() == EffortKind.REFERENCE
            ));
        }
        return out;
    }

    /** Formats seconds as m:ss (e.g. 252 → "4:12"). */
    public static String formatElapsed(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return m + ":" + (s < 10 ? "0" + s : Integer.toString(s));
    }
}
