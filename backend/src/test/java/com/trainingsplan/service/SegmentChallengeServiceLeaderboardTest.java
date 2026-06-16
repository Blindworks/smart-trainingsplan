package com.trainingsplan.service;

import com.trainingsplan.dto.SegmentLeaderboardEntryDto;
import com.trainingsplan.entity.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmentChallengeServiceLeaderboardTest {

    private SegmentEffort effort(EffortKind kind, EffortCategory cat, String name, int elapsed) {
        SegmentEffort e = new SegmentEffort();
        e.setId((long) (elapsed));
        e.setKind(kind);
        e.setCategory(cat);
        e.setDisplayName(name);
        e.setElapsedSeconds(elapsed);
        e.setStatus(EffortStatus.VALID);
        e.setActivityType(ActivityType.RIDE);
        return e;
    }

    @Test
    void buildLeaderboard_ranksAscendingAndComputesGapToLeader() {
        List<SegmentEffort> efforts = List.of(
                effort(EffortKind.PUBLIC, EffortCategory.COMMUNITY, "Lukas", 298),
                effort(EffortKind.REFERENCE, EffortCategory.PRO_MEN, "Pro M", 252),
                effort(EffortKind.PUBLIC, EffortCategory.COMMUNITY, "Sarah", 301)
        ); // intentionally unsorted

        List<SegmentLeaderboardEntryDto> board = SegmentChallengeService.buildLeaderboard(efforts);

        assertEquals(3, board.size());
        assertEquals("Pro M", board.get(0).displayName());
        assertEquals(1, board.get(0).rank());
        assertEquals(0, board.get(0).gapToLeaderSeconds());
        assertTrue(board.get(0).reference());
        assertEquals(2, board.get(1).rank());
        assertEquals(46, board.get(1).gapToLeaderSeconds()); // 298 - 252
        assertEquals("4:12", board.get(0).elapsedFormatted()); // 252 s
    }

    @Test
    void formatElapsed_padsSeconds() {
        assertEquals("4:12", SegmentChallengeService.formatElapsed(252));
        assertEquals("0:05", SegmentChallengeService.formatElapsed(5));
        assertEquals("12:00", SegmentChallengeService.formatElapsed(720));
    }
}
