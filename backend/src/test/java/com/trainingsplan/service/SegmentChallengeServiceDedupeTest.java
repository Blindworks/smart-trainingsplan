package com.trainingsplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.SegmentEffortResultDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.SegmentChallengeRepository;
import com.trainingsplan.repository.SegmentEffortRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SegmentChallengeServiceDedupeTest {

    @Mock private SegmentChallengeRepository challengeRepository;
    @Mock private SegmentEffortRepository effortRepository;
    @Mock private GpxParsingService gpxParsingService;
    @Mock private SegmentMatchingService matchingService;

    private SegmentChallengeService service;

    /** Minimal polyline JSON: one point — enough for matchTrack's objectMapper.readValue. */
    private static final String POLYLINE_JSON = "[[50.178,8.7354,112.0],[50.168,8.73,166.0]]";

    private static final List<double[]> CROPPED_TRACK = List.of(
            new double[]{50.178, 8.7354, 112.0, 0.0},
            new double[]{50.168, 8.7300, 166.0, 298.0}
    );

    private SegmentChallenge activeChallenge() {
        SegmentChallenge c = new SegmentChallenge();
        c.setId(1L);
        c.setSlug("heartbreak-hill-2026");
        c.setName("Heartbreak Hill 2026");
        c.setActive(true);
        c.setPolylineJson(POLYLINE_JSON);
        return c;
    }

    private SegmentMatchResult successfulMatch() {
        return SegmentMatchResult.matched(298, 1.09, 13.1, 275, CROPPED_TRACK);
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new SegmentChallengeService(challengeRepository, effortRepository,
                gpxParsingService, matchingService, new ObjectMapper());

        // Default: no rate-limit hits
        when(effortRepository.countByChallengeIdAndIpHashAndCreatedAtAfter(anyLong(), anyString(), any()))
                .thenReturn(0L);
    }

    // ---- helpers ----

    private void stubChallengeAndMatch() throws Exception {
        when(challengeRepository.findBySlug("heartbreak-hill-2026"))
                .thenReturn(Optional.of(activeChallenge()));
        ParsedActivityData data = new ParsedActivityData();
        data.latLngPoints = List.of(new double[]{50.178, 8.7354}, new double[]{50.168, 8.73});
        data.timeSeconds = List.of(0, 298);
        data.elevations = List.of(112.0, 166.0);
        when(gpxParsingService.parse(any())).thenReturn(data);
        when(matchingService.match(any(), any(), any(), any())).thenReturn(successfulMatch());
    }

    // ---- tests ----

    @Test
    void submitPublicEffort_firstUpload_savesEffortWithDedupeKey() throws Exception {
        stubChallengeAndMatch();
        when(effortRepository.findFirstByChallengeIdAndStatusAndDedupeKey(anyLong(), any(), anyString()))
                .thenReturn(Optional.empty());

        SegmentEffort saved = new SegmentEffort();
        saved.setId(42L);
        saved.setElapsedSeconds(298);
        saved.setStatus(EffortStatus.VALID);
        saved.setActivityType(ActivityType.RUN);
        saved.setEditToken("tok");
        when(effortRepository.save(any())).thenReturn(saved);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(saved));

        SegmentEffortResultDto result = service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "Alice",
                "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        // save must have been called once
        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(1)).save(captor.capture());

        SegmentEffort persisted = captor.getValue();
        assertNotNull(persisted.getDedupeKey(), "dedupeKey must be set before save");
        assertEquals(32, persisted.getDedupeKey().length(), "MD5 hex = 32 chars");
        assertNotNull(result);
    }

    @Test
    void submitPublicEffort_duplicate_returnsExistingWithoutSaving() throws Exception {
        stubChallengeAndMatch();

        SegmentEffort existing = new SegmentEffort();
        existing.setId(7L);
        existing.setElapsedSeconds(298);
        existing.setStatus(EffortStatus.VALID);
        existing.setActivityType(ActivityType.RUN);
        existing.setEditToken("old-tok");
        existing.setDedupeKey("somehash");

        when(effortRepository.findFirstByChallengeIdAndStatusAndDedupeKey(anyLong(), any(), anyString()))
                .thenReturn(Optional.of(existing));
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(existing));

        SegmentEffortResultDto result = service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "Alice",
                "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        // save must NOT be called for a duplicate
        verify(effortRepository, never()).save(any());
        assertNotNull(result);
        assertEquals(7L, result.effortId());
    }

    @Test
    void submitPublicEffort_twoDifferentRunners_bothSaved() throws Exception {
        stubChallengeAndMatch();

        // First call — no duplicate
        when(effortRepository.findFirstByChallengeIdAndStatusAndDedupeKey(anyLong(), any(), anyString()))
                .thenReturn(Optional.empty());

        SegmentEffort saved1 = new SegmentEffort();
        saved1.setId(10L);
        saved1.setElapsedSeconds(298);
        saved1.setStatus(EffortStatus.VALID);
        saved1.setActivityType(ActivityType.RUN);
        when(effortRepository.save(any())).thenReturn(saved1);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(saved1));

        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Alice",
                "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");
        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Bob",
                "<gpx/>".getBytes(), "run.gpx", "5.6.7.8");

        // save called twice — once per upload (both resolve as "new" in this test because the
        // mock always returns empty)
        verify(effortRepository, times(2)).save(any());
    }
}
