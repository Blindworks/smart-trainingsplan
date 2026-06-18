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

    private SegmentEffort demo(String name, int elapsed, Gender g, Integer birthYear, int attempts) {
        SegmentEffort e = effort(EffortKind.PUBLIC, EffortCategory.COMMUNITY, name, elapsed);
        e.setGender(g);
        e.setBirthYear(birthYear);
        e.setAttemptCount(attempts);
        return e;
    }

    private static final int REF_YEAR = 2026;

    @Test
    void buildLeaderboard_ranksAscendingAndComputesGapToLeader() {
        List<SegmentEffort> efforts = List.of(
                effort(EffortKind.PUBLIC, EffortCategory.COMMUNITY, "Lukas", 298),
                effort(EffortKind.REFERENCE, EffortCategory.PRO_MEN, "Pro M", 252),
                effort(EffortKind.PUBLIC, EffortCategory.COMMUNITY, "Sarah", 301)
        ); // intentionally unsorted

        List<SegmentLeaderboardEntryDto> board = SegmentChallengeService.buildLeaderboard(efforts, REF_YEAR);

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

    @Test
    void buildLeaderboard_derivesGenderForReferenceFromCategory_andAgeGroupFromBirthYear() {
        SegmentEffort proWoman = effort(EffortKind.REFERENCE, EffortCategory.PRO_WOMEN, "Pro W", 270);
        SegmentEffort runner = demo("Mara", 300, Gender.FEMALE, 1986, 1); // age 40 @2026

        List<SegmentLeaderboardEntryDto> board =
                SegmentChallengeService.buildLeaderboard(List.of(proWoman, runner), REF_YEAR);

        SegmentLeaderboardEntryDto pro = board.stream().filter(b -> b.displayName().equals("Pro W")).findFirst().orElseThrow();
        SegmentLeaderboardEntryDto mara = board.stream().filter(b -> b.displayName().equals("Mara")).findFirst().orElseThrow();
        assertEquals("FEMALE", pro.gender(), "reference gender derived from PRO_WOMEN");
        assertNull(pro.ageGroup(), "reference without birth year has no age group");
        assertEquals("FEMALE", mara.gender());
        assertEquals("40-44", mara.ageGroup());
        assertEquals(1, mara.attemptCount());
    }

    @Test
    void filterForScope_menWomenUseEffectiveGender_diversExcluded() {
        SegmentEffort man = demo("M", 300, Gender.MALE, 1990, 1);
        SegmentEffort woman = demo("W", 310, Gender.FEMALE, 1990, 1);
        SegmentEffort diverse = demo("D", 320, Gender.DIVERS, 1990, 1);
        SegmentEffort proMan = effort(EffortKind.REFERENCE, EffortCategory.PRO_MEN, "ProM", 250);
        List<SegmentEffort> all = List.of(man, woman, diverse, proMan);

        List<SegmentEffort> men = SegmentChallengeService.filterForScope(all, LeaderboardScope.MEN, null, REF_YEAR);
        assertEquals(2, men.size());
        assertTrue(men.contains(man), "stored MALE is in MEN");
        assertTrue(men.contains(proMan), "PRO_MEN counts as MALE via effective gender");
        assertFalse(men.contains(diverse), "DIVERS never in MEN");

        List<SegmentEffort> women = SegmentChallengeService.filterForScope(all, LeaderboardScope.WOMEN, null, REF_YEAR);
        assertEquals(1, women.size());
        assertEquals("W", women.get(0).getDisplayName());
    }

    @Test
    void filterForScope_ageGroupFiltersWithinGender() {
        SegmentEffort young = demo("Young", 300, Gender.MALE, 2000, 1); // age 26 -> 25-29
        SegmentEffort mid = demo("Mid", 305, Gender.MALE, 1986, 1);     // age 40 -> 40-44
        SegmentEffort noYear = demo("NoYear", 310, Gender.MALE, null, 1);

        List<SegmentEffort> men40 = SegmentChallengeService.filterForScope(
                List.of(young, mid, noYear), LeaderboardScope.MEN, "40-44", REF_YEAR);

        assertEquals(1, men40.size());
        assertEquals("Mid", men40.get(0).getDisplayName());
    }

    @Test
    void filterForScope_mostAttemptsIsPublicOnly() {
        SegmentEffort pub = demo("Pub", 300, null, null, 5);
        SegmentEffort ref = effort(EffortKind.REFERENCE, EffortCategory.PRO_MEN, "Ref", 250);

        List<SegmentEffort> only = SegmentChallengeService.filterForScope(
                List.of(pub, ref), LeaderboardScope.MOST_ATTEMPTS, null, REF_YEAR);

        assertEquals(1, only.size());
        assertEquals("Pub", only.get(0).getDisplayName());
    }

    @Test
    void buildAttemptsLeaderboard_sortsByAttemptsDescTieBreakByTime() {
        SegmentEffort a = demo("A", 300, null, null, 2);
        SegmentEffort b = demo("B", 280, null, null, 5);
        SegmentEffort c = demo("C", 290, null, null, 5);

        List<SegmentLeaderboardEntryDto> board =
                SegmentChallengeService.buildAttemptsLeaderboard(List.of(a, b, c), REF_YEAR);

        assertEquals("B", board.get(0).displayName()); // 5 attempts, 280 s
        assertEquals(1, board.get(0).rank());
        assertEquals("C", board.get(1).displayName()); // 5 attempts, 290 s (tie-break slower)
        assertEquals(1, board.get(1).rank());          // same attempt count -> same rank
        assertEquals("A", board.get(2).displayName()); // 2 attempts
        assertEquals(3, board.get(2).rank());
        assertEquals(5, board.get(0).attemptCount());
    }
}
