package com.trainingsplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.SegmentEffortResultDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.SegmentChallengeRepository;
import com.trainingsplan.repository.SegmentEffortRepository;
import com.trainingsplan.util.SegmentEffortDedup;
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

/**
 * Tests keep-best-by-name semantics: one leaderboard row per (challenge, activityType, displayName),
 * holding the smallest elapsedSeconds seen for that identity.
 */
@ExtendWith(MockitoExtension.class)
class SegmentChallengeServiceDedupeTest {

    @Mock private SegmentChallengeRepository challengeRepository;
    @Mock private SegmentEffortRepository effortRepository;
    @Mock private GpxParsingService gpxParsingService;
    @Mock private SegmentMatchingService matchingService;

    private SegmentChallengeService service;

    private static final String POLYLINE_JSON = "[[50.178,8.7354,112.0],[50.168,8.73,166.0]]";

    private static final List<double[]> CROPPED_TRACK = List.of(
            new double[]{50.17800, 8.73546, 112.0, 0.0},
            new double[]{50.16885, 8.73000, 166.0, 298.0}
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

        // lenient: implausibleBirthYear_isRejected throws before these are called
        lenient().when(effortRepository.countByChallengeIdAndIpHashAndCreatedAtAfter(anyLong(), anyString(), any()))
                .thenReturn(0L);
        lenient().when(challengeRepository.findBySlug("heartbreak-hill-2026"))
                .thenReturn(Optional.of(activeChallenge()));
    }

    private ParsedActivityData minimalParsedData() {
        ParsedActivityData data = new ParsedActivityData();
        data.latLngPoints = List.of(new double[]{50.178, 8.7354}, new double[]{50.168, 8.73});
        data.timeSeconds = List.of(0, 298);
        data.elevations = List.of(112.0, 166.0);
        return data;
    }

    // ---- scenario 1: first upload for this name → save new row ----

    @Test
    void firstUpload_nameUnseen_savesNewEffortWithDedupeKey() throws Exception {
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(300, 1.09, 13.1, 275, CROPPED_TRACK));
        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), any(), any(), anyString()))
                .thenReturn(Optional.empty());

        SegmentEffort saved = new SegmentEffort();
        saved.setId(1L);
        saved.setElapsedSeconds(300);
        saved.setStatus(EffortStatus.VALID);
        saved.setActivityType(ActivityType.RUN);
        saved.setEditToken("tok");
        when(effortRepository.save(any())).thenReturn(saved);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(saved));

        SegmentEffortResultDto result = service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "Alice",
                null, null, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(1)).save(captor.capture());

        SegmentEffort persisted = captor.getValue();
        assertNotNull(persisted.getDedupeKey(), "dedupeKey must be set on new effort");
        assertEquals(32, persisted.getDedupeKey().length(), "MD5 hex is 32 chars");
        assertNotNull(result);
    }

    // ---- scenario 2: slower re-upload (same name) → bump attempt count, keep best time ----

    @Test
    void slowerReupload_sameNameVariant_bumpsAttemptCount_keepsBestTime() throws Exception {
        // existing best: 300 s, 1 attempt; new upload: 360 s (slower) -> save with attempt 2, time stays 300
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(360, 1.09, 13.1, 275, CROPPED_TRACK));

        SegmentEffort existing = new SegmentEffort();
        existing.setId(7L);
        existing.setElapsedSeconds(300);
        existing.setStatus(EffortStatus.VALID);
        existing.setActivityType(ActivityType.RUN);
        existing.setEditToken("old-tok");
        existing.setDedupeKey("somehash");
        existing.setAttemptCount(1);

        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), eq(EffortKind.PUBLIC), eq(EffortStatus.VALID), anyString()))
                .thenReturn(Optional.of(existing));
        when(effortRepository.save(any())).thenReturn(existing);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(existing));

        SegmentEffortResultDto result = service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "  Alice  ",
                null, null, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(1)).save(captor.capture());
        SegmentEffort saved = captor.getValue();
        assertEquals(7L, saved.getId(), "same row");
        assertEquals(300, saved.getElapsedSeconds(), "best time kept, not the slower upload");
        assertEquals(2, saved.getAttemptCount(), "attempt counter bumped on every upload");
        assertEquals(300, result.elapsedSeconds());
    }

    // ---- scenario 3: faster re-upload (same name) → update same row in place ----

    @Test
    void fasterReupload_sameNameCaseVariant_updatesSameRowWithNewBestTime() throws Exception {
        // existing best: 300 s; new upload: 250 s (faster) → update same entity
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(250, 1.09, 13.1, 275, CROPPED_TRACK));

        SegmentEffort existing = new SegmentEffort();
        existing.setId(7L);
        existing.setElapsedSeconds(300);
        existing.setStatus(EffortStatus.VALID);
        existing.setActivityType(ActivityType.RUN);
        existing.setEditToken("old-tok");
        existing.setDedupeKey("somehash");

        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), eq(EffortKind.PUBLIC), eq(EffortStatus.VALID), anyString()))
                .thenReturn(Optional.of(existing));
        when(effortRepository.save(any())).thenReturn(existing);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(existing));

        // "ALICE" normalises to the same identity as "Alice"
        service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "ALICE",
                null, null, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(1)).save(captor.capture());

        SegmentEffort updated = captor.getValue();
        assertEquals(7L, updated.getId(), "must update the same entity (same id)");
        assertEquals(250, updated.getElapsedSeconds(), "elapsedSeconds must be updated to new best");
    }

    // ---- scenario 4: different display name → separate new row ----

    @Test
    void differentDisplayName_savesNewRow() throws Exception {
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(290, 1.09, 13.1, 275, CROPPED_TRACK))
                .thenReturn(SegmentMatchResult.matched(305, 1.09, 12.7, 285, CROPPED_TRACK));

        // finder always returns empty (Bob is unknown)
        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), any(), any(), anyString()))
                .thenReturn(Optional.empty());

        SegmentEffort stub = new SegmentEffort();
        stub.setId(10L);
        stub.setElapsedSeconds(290);
        stub.setStatus(EffortStatus.VALID);
        stub.setActivityType(ActivityType.RUN);
        when(effortRepository.save(any())).thenReturn(stub);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(stub));

        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Alice",
                null, null, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");
        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Bob",
                null, null, "<gpx/>".getBytes(), "run.gpx", "5.6.7.8");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(2)).save(captor.capture());

        List<SegmentEffort> captured = captor.getAllValues();
        String keyAlice = captured.get(0).getDedupeKey();
        String keyBob   = captured.get(1).getDedupeKey();

        assertNotNull(keyAlice, "Alice's dedupeKey must be set");
        assertNotNull(keyBob,   "Bob's dedupeKey must be set");
        assertNotEquals(keyAlice, keyBob, "different display names must produce different identity keys");
    }

    // ---- scenario 5: result carries speed/pace from the winning effort ----

    @Test
    void result_carriesSpeedAndPaceFromSavedEffort() throws Exception {
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(300, 1.09, 13.1, 275, CROPPED_TRACK));
        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), any(), any(), anyString()))
                .thenReturn(Optional.empty());

        SegmentEffort saved = new SegmentEffort();
        saved.setId(1L);
        saved.setElapsedSeconds(300);
        saved.setStatus(EffortStatus.VALID);
        saved.setActivityType(ActivityType.RUN);
        saved.setEditToken("tok");
        saved.setAvgSpeedKmh(13.1);
        saved.setAvgPaceSecondsPerKm(275);
        when(effortRepository.save(any())).thenReturn(saved);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(saved));

        SegmentEffortResultDto result = service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "Alice",
                null, null, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        assertEquals(13.1, result.avgSpeedKmh(), "ride/run speed must flow through to the DTO");
        assertEquals(275, result.avgPaceSecondsPerKm(), "pace must flow through to the DTO");
    }

    @Test
    void fasterReupload_bumpsAttemptCount_andUpdatesBestTime() throws Exception {
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(250, 1.09, 13.1, 275, CROPPED_TRACK));

        SegmentEffort existing = new SegmentEffort();
        existing.setId(7L);
        existing.setElapsedSeconds(300);
        existing.setStatus(EffortStatus.VALID);
        existing.setActivityType(ActivityType.RUN);
        existing.setEditToken("old-tok");
        existing.setDedupeKey("somehash");
        existing.setAttemptCount(3);

        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), eq(EffortKind.PUBLIC), eq(EffortStatus.VALID), anyString()))
                .thenReturn(Optional.of(existing));
        when(effortRepository.save(any())).thenReturn(existing);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(existing));

        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Alice",
                Gender.FEMALE, 1990, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(1)).save(captor.capture());
        SegmentEffort saved = captor.getValue();
        assertEquals(250, saved.getElapsedSeconds(), "best time updated");
        assertEquals(4, saved.getAttemptCount(), "attempt counter bumped");
        assertEquals(Gender.FEMALE, saved.getGender(), "latest demographics applied");
        assertEquals(1990, saved.getBirthYear());
    }

    @Test
    void firstUpload_persistsGenderAndBirthYear_andAttemptCountOne() throws Exception {
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(300, 1.09, 13.1, 275, CROPPED_TRACK));
        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), any(), any(), anyString())).thenReturn(Optional.empty());

        SegmentEffort saved = new SegmentEffort();
        saved.setId(1L);
        saved.setElapsedSeconds(300);
        saved.setStatus(EffortStatus.VALID);
        saved.setActivityType(ActivityType.RUN);
        saved.setEditToken("tok");
        when(effortRepository.save(any())).thenReturn(saved);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(saved));

        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Mara",
                Gender.FEMALE, 1986, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository, times(1)).save(captor.capture());
        SegmentEffort persisted = captor.getValue();
        assertEquals(Gender.FEMALE, persisted.getGender());
        assertEquals(1986, persisted.getBirthYear());
        assertEquals(1, persisted.getAttemptCount());
    }

    @Test
    void implausibleBirthYear_isRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "X",
                        Gender.MALE, 3000, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4"));
        assertEquals("invalid_birth_year", ex.getMessage());
    }

    // ---- file-content dedup: the exact same .gpx may not be uploaded twice ----

    @Test
    void duplicateFile_sameContentHash_isRejected_andNotSaved() {
        // An effort with this file's content hash already exists in the challenge.
        when(effortRepository.existsByChallengeIdAndKindAndStatusAndFileHash(
                anyLong(), eq(EffortKind.PUBLIC), eq(EffortStatus.VALID), anyString()))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Anyone",
                        null, null, "<gpx>same-bytes</gpx>".getBytes(), "run.gpx", "1.2.3.4"));

        assertEquals("duplicate_file", ex.getMessage());
        verify(effortRepository, never()).save(any());
    }

    @Test
    void firstUpload_persistsFileHash() throws Exception {
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(300, 1.09, 13.1, 275, CROPPED_TRACK));
        when(effortRepository.existsByChallengeIdAndKindAndStatusAndFileHash(
                anyLong(), any(), any(), anyString())).thenReturn(false);
        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), any(), any(), anyString())).thenReturn(Optional.empty());

        SegmentEffort saved = new SegmentEffort();
        saved.setId(1L);
        saved.setElapsedSeconds(300);
        saved.setStatus(EffortStatus.VALID);
        saved.setActivityType(ActivityType.RUN);
        saved.setEditToken("tok");
        when(effortRepository.save(any())).thenReturn(saved);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(saved));

        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Alice",
                null, null, "<gpx>alice</gpx>".getBytes(), "run.gpx", "1.2.3.4");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository).save(captor.capture());
        SegmentEffort persisted = captor.getValue();
        assertNotNull(persisted.getFileHash(), "fileHash must be set on a new effort");
        assertEquals(SegmentEffortDedup.fileHash("<gpx>alice</gpx>".getBytes()), persisted.getFileHash(),
                "fileHash must be the SHA-256 of the uploaded bytes");
    }

    @Test
    void fasterReupload_updatesFileHashToNewRecording() throws Exception {
        // existing best 300 s with an old recording; new, different, faster file (250 s)
        when(gpxParsingService.parse(any())).thenReturn(minimalParsedData());
        when(matchingService.match(any(), any(), any(), any()))
                .thenReturn(SegmentMatchResult.matched(250, 1.09, 13.1, 275, CROPPED_TRACK));
        when(effortRepository.existsByChallengeIdAndKindAndStatusAndFileHash(
                anyLong(), any(), any(), anyString())).thenReturn(false);

        SegmentEffort existing = new SegmentEffort();
        existing.setId(7L);
        existing.setElapsedSeconds(300);
        existing.setStatus(EffortStatus.VALID);
        existing.setActivityType(ActivityType.RUN);
        existing.setEditToken("old-tok");
        existing.setDedupeKey("somehash");
        existing.setFileHash("old-file-hash");

        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), eq(EffortKind.PUBLIC), eq(EffortStatus.VALID), anyString()))
                .thenReturn(Optional.of(existing));
        when(effortRepository.save(any())).thenReturn(existing);
        when(effortRepository.findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                anyLong(), any(), any())).thenReturn(List.of(existing));

        service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "Alice",
                null, null, "<gpx>new-best</gpx>".getBytes(), "run.gpx", "1.2.3.4");

        ArgumentCaptor<SegmentEffort> captor = ArgumentCaptor.forClass(SegmentEffort.class);
        verify(effortRepository).save(captor.capture());
        SegmentEffort updated = captor.getValue();
        assertEquals(250, updated.getElapsedSeconds(), "new best time kept");
        assertEquals(SegmentEffortDedup.fileHash("<gpx>new-best</gpx>".getBytes()), updated.getFileHash(),
                "fileHash follows the new best recording");
    }
}
