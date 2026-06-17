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

    private static final String POLYLINE_JSON = "[[50.178,8.7354,112.0],[50.168,8.73,166.0]]";

    // Two distinct cropped tracks — different start coords AND different elapsed seconds,
    // so they always produce different dedupe keys.
    private static final List<double[]> TRACK_ALICE = List.of(
            new double[]{50.17800, 8.73546, 112.0, 0.0},
            new double[]{50.16885, 8.73000, 166.0, 298.0}
    );
    private static final List<double[]> TRACK_BOB = List.of(
            new double[]{50.17800, 8.73546, 112.0, 0.0},
            new double[]{50.16885, 8.73000, 166.0, 310.0}
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

    @BeforeEach
    void setUp() {
        service = new SegmentChallengeService(challengeRepository, effortRepository,
                gpxParsingService, matchingService, new ObjectMapper());

        when(effortRepository.countByChallengeIdAndIpHashAndCreatedAtAfter(anyLong(), anyString(), any()))
                .thenReturn(0L);
        when(challengeRepository.findBySlug("heartbreak-hill-2026"))
                .thenReturn(Optional.of(activeChallenge()));
    }

    private ParsedActivityData minimalParsedData() {
        ParsedActivityData data = new ParsedActivityData();
        data.latLngPoints = List.of(new double[]{50.178, 8.7354}, new double[]{50.168, 8.73});
        data.timeSeconds = List.of(0, 298);
        data.elevations = List.of(112.0, 166.0);
        return data;
    }

    // ---- tests ----

    @Test
    void submitPublicEffort_firstUpload_savesEffortWithDedupeKey() throws Exception {
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(298, 1.09, 13.1, 275, TRACK_ALICE));
        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), any(), any(), anyString()))
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

        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Alice",
                "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(1)).save(captor.capture());

        SegmentEffort persisted = captor.getValue();
        assertNotNull(persisted.getDedupeKey(), "dedupeKey must be set before save");
        assertEquals(32, persisted.getDedupeKey().length(), "MD5 hex = 32 chars");
    }

    @Test
    void submitPublicEffort_duplicate_returnsExistingWithoutSaving() throws Exception {
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(298, 1.09, 13.1, 275, TRACK_ALICE));

        SegmentEffort existing = new SegmentEffort();
        existing.setId(7L);
        existing.setElapsedSeconds(298);
        existing.setStatus(EffortStatus.VALID);
        existing.setActivityType(ActivityType.RUN);
        existing.setEditToken("old-tok");
        existing.setDedupeKey("somehash");

        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), eq(EffortKind.PUBLIC), eq(EffortStatus.VALID), anyString()))
                .thenReturn(Optional.of(existing));
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(existing));

        SegmentEffortResultDto result = service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "Alice",
                "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        verify(effortRepository, never()).save(any());
        assertNotNull(result);
        assertEquals(7L, result.effortId());
    }

    @Test
    void submitPublicEffort_twoDifferentRunners_produceDifferentKeysAndBothSaved() throws Exception {
        // Alice: elapsed=298, Bob: elapsed=310 → genuinely different dedupe keys
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(298, 1.09, 13.1, 275, TRACK_ALICE))
                .thenReturn(SegmentMatchResult.matched(310, 1.09, 12.7, 285, TRACK_BOB));

        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), any(), any(), anyString()))
                .thenReturn(Optional.empty());

        SegmentEffort stub = new SegmentEffort();
        stub.setId(10L);
        stub.setElapsedSeconds(298);
        stub.setStatus(EffortStatus.VALID);
        stub.setActivityType(ActivityType.RUN);
        when(effortRepository.save(any())).thenReturn(stub);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(stub));

        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Alice",
                "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");
        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Bob",
                "<gpx/>".getBytes(), "run.gpx", "5.6.7.8");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(2)).save(captor.capture());

        List<SegmentEffort> captured = captor.getAllValues();
        String keyAlice = captured.get(0).getDedupeKey();
        String keyBob   = captured.get(1).getDedupeKey();

        assertNotNull(keyAlice, "Alice's dedupeKey must be set");
        assertNotNull(keyBob,   "Bob's dedupeKey must be set");
        assertNotEquals(keyAlice, keyBob, "different runs must produce different dedupe keys");
    }
}
