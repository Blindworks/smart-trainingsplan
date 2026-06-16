# Heartbreak Hill Challenge — Backend (Plan 1/3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the backend for a public, anonymous segment challenge: GPX upload → segment matching → time → a PACR-owned leaderboard that mixes an admin-curated reference roster ("the greats") with public uploads.

**Architecture:** A new, self-contained module in the existing layer-based packages (`entity` / `repository` / `service` / `controller` / `dto`), fully decoupled from the PRO-gated `CommunityRoute`/`RouteAttempt` world. Public read + submit endpoints live under `/api/public/**` (permitAll, no user assumptions); roster management under `/api/admin/**` (ADMIN role). Existing `GpxParsingService` is reused (and minimally extended). Entities are generic (`SegmentChallenge`) so future segment campaigns are possible.

**Tech Stack:** Java 21, Spring Boot 3.2, JPA/Hibernate, MariaDB, Liquibase, JUnit Jupiter, Jackson.

---

## File Structure

**New files**
- `backend/src/main/java/com/trainingsplan/util/GeoUtils.java` — static Haversine distance.
- `backend/src/main/java/com/trainingsplan/entity/ActivityType.java` — enum `RIDE | RUN`.
- `backend/src/main/java/com/trainingsplan/entity/EffortKind.java` — enum `REFERENCE | PUBLIC`.
- `backend/src/main/java/com/trainingsplan/entity/EffortStatus.java` — enum `VALID | PENDING | REJECTED`.
- `backend/src/main/java/com/trainingsplan/entity/EffortCategory.java` — enum `PRO_MEN | PRO_WOMEN | KONA_QUALIFIER | AGE_GROUP | COMMUNITY`.
- `backend/src/main/java/com/trainingsplan/entity/SegmentChallenge.java` — challenge/segment entity.
- `backend/src/main/java/com/trainingsplan/entity/SegmentEffort.java` — leaderboard entry entity.
- `backend/src/main/java/com/trainingsplan/repository/SegmentChallengeRepository.java`
- `backend/src/main/java/com/trainingsplan/repository/SegmentEffortRepository.java`
- `backend/src/main/java/com/trainingsplan/service/SegmentMatchResult.java` — value object from matcher.
- `backend/src/main/java/com/trainingsplan/service/SegmentMatchingService.java` — core matching logic.
- `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java` — orchestration + leaderboard.
- `backend/src/main/java/com/trainingsplan/dto/SegmentChallengeDto.java`
- `backend/src/main/java/com/trainingsplan/dto/SegmentLeaderboardEntryDto.java`
- `backend/src/main/java/com/trainingsplan/dto/SegmentEffortResultDto.java`
- `backend/src/main/java/com/trainingsplan/dto/SegmentTrackDto.java`
- `backend/src/main/java/com/trainingsplan/controller/PublicSegmentChallengeController.java`
- `backend/src/main/java/com/trainingsplan/controller/AdminSegmentChallengeController.java`
- `backend/src/main/java/com/trainingsplan/controller/SegmentEffortController.java` — authenticated claim.
- `backend/src/main/resources/db/changelog/changes/140-create-segment-challenge.xml`
- Tests: `GeoUtilsTest`, `SegmentMatchingServiceTest`, `SegmentChallengeServiceLeaderboardTest`, `GpxParsingServiceElevationTest`, `PublicSegmentChallengeControllerTest`.

**Modified files**
- `backend/src/main/java/com/trainingsplan/service/ParsedActivityData.java` — add `elevations`.
- `backend/src/main/java/com/trainingsplan/service/GpxParsingService.java` — populate `elevations`.
- `backend/src/main/java/com/trainingsplan/config/SecurityConfig.java` — permitAll `/api/public/**`.
- `backend/src/main/resources/db/changelog/db.changelog-master.xml` — include migration 140.
- `CHANGELOG.md`, `backend/pom.xml` + `frontend/package.json` (version bump) — at the final commit.

---

## Task 1: GeoUtils (Haversine)

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/util/GeoUtils.java`
- Test: `backend/src/test/java/com/trainingsplan/util/GeoUtilsTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.trainingsplan.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void haversine_samepoint_isZero() {
        assertEquals(0.0, GeoUtils.haversineMeters(50.178, 8.74, 50.178, 8.74), 1e-6);
    }

    @Test
    void haversine_oneDegreeLat_isAboutOneEleventhKm() {
        // ~0.0003 deg latitude ≈ 33.3 m
        double d = GeoUtils.haversineMeters(50.0000, 8.0, 50.0003, 8.0);
        assertEquals(33.3, d, 1.5);
    }

    @Test
    void haversine_knownDistance_frankfurtToOffenbach() {
        // Frankfurt Hbf (50.1070, 8.6638) → Offenbach (50.0997, 8.7765): ~8.1 km
        double d = GeoUtils.haversineMeters(50.1070, 8.6638, 50.0997, 8.7765);
        assertEquals(8100, d, 400);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -q test -Dtest=GeoUtilsTest`
Expected: FAIL — `GeoUtils` does not exist (compilation error).

- [ ] **Step 3: Write minimal implementation**

```java
package com.trainingsplan.util;

/** WGS-84 geodesic helpers. */
public final class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoUtils() {}

    /** Great-circle distance in metres between two WGS-84 coordinates. */
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -q test -Dtest=GeoUtilsTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/util/GeoUtils.java backend/src/test/java/com/trainingsplan/util/GeoUtilsTest.java
git commit -m "Add GeoUtils Haversine helper for segment matching"
```

---

## Task 2: Enums

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/entity/ActivityType.java`
- Create: `backend/src/main/java/com/trainingsplan/entity/EffortKind.java`
- Create: `backend/src/main/java/com/trainingsplan/entity/EffortStatus.java`
- Create: `backend/src/main/java/com/trainingsplan/entity/EffortCategory.java`

- [ ] **Step 1: Create the four enums**

`ActivityType.java`:
```java
package com.trainingsplan.entity;

public enum ActivityType { RIDE, RUN }
```

`EffortKind.java`:
```java
package com.trainingsplan.entity;

/** REFERENCE = admin-curated "the greats" roster; PUBLIC = anonymous public upload. */
public enum EffortKind { REFERENCE, PUBLIC }
```

`EffortStatus.java`:
```java
package com.trainingsplan.entity;

public enum EffortStatus { VALID, PENDING, REJECTED }
```

`EffortCategory.java`:
```java
package com.trainingsplan.entity;

/** Category label, mainly for REFERENCE efforts. */
public enum EffortCategory { PRO_MEN, PRO_WOMEN, KONA_QUALIFIER, AGE_GROUP, COMMUNITY }
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/entity/ActivityType.java backend/src/main/java/com/trainingsplan/entity/EffortKind.java backend/src/main/java/com/trainingsplan/entity/EffortStatus.java backend/src/main/java/com/trainingsplan/entity/EffortCategory.java
git commit -m "Add enums for segment challenge module"
```

---

## Task 3: SegmentChallenge entity

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/entity/SegmentChallenge.java`

- [ ] **Step 1: Create the entity**

```java
package com.trainingsplan.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "segment_challenges")
public class SegmentChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String subtitle;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "start_lat", nullable = false)
    private Double startLat;

    @Column(name = "start_lng", nullable = false)
    private Double startLng;

    @Column(name = "end_lat", nullable = false)
    private Double endLat;

    @Column(name = "end_lng", nullable = false)
    private Double endLng;

    @Column(name = "distance_m")
    private Double distanceM;

    @Column(name = "elevation_gain_m")
    private Integer elevationGainM;

    @Column(name = "avg_grade_pct")
    private Double avgGradePct;

    @Column(name = "max_grade_pct")
    private Double maxGradePct;

    @Column(name = "polyline_json", columnDefinition = "LONGTEXT")
    private String polylineJson;

    @Column(name = "terrain_asset_ref", length = 255)
    private String terrainAssetRef;

    @Column(name = "bounding_box_json", length = 500)
    private String boundingBoxJson;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SegmentChallenge() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public Double getStartLat() { return startLat; }
    public void setStartLat(Double startLat) { this.startLat = startLat; }
    public Double getStartLng() { return startLng; }
    public void setStartLng(Double startLng) { this.startLng = startLng; }
    public Double getEndLat() { return endLat; }
    public void setEndLat(Double endLat) { this.endLat = endLat; }
    public Double getEndLng() { return endLng; }
    public void setEndLng(Double endLng) { this.endLng = endLng; }
    public Double getDistanceM() { return distanceM; }
    public void setDistanceM(Double distanceM) { this.distanceM = distanceM; }
    public Integer getElevationGainM() { return elevationGainM; }
    public void setElevationGainM(Integer elevationGainM) { this.elevationGainM = elevationGainM; }
    public Double getAvgGradePct() { return avgGradePct; }
    public void setAvgGradePct(Double avgGradePct) { this.avgGradePct = avgGradePct; }
    public Double getMaxGradePct() { return maxGradePct; }
    public void setMaxGradePct(Double maxGradePct) { this.maxGradePct = maxGradePct; }
    public String getPolylineJson() { return polylineJson; }
    public void setPolylineJson(String polylineJson) { this.polylineJson = polylineJson; }
    public String getTerrainAssetRef() { return terrainAssetRef; }
    public void setTerrainAssetRef(String terrainAssetRef) { this.terrainAssetRef = terrainAssetRef; }
    public String getBoundingBoxJson() { return boundingBoxJson; }
    public void setBoundingBoxJson(String boundingBoxJson) { this.boundingBoxJson = boundingBoxJson; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/entity/SegmentChallenge.java
git commit -m "Add SegmentChallenge entity"
```

---

## Task 4: SegmentEffort entity

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/entity/SegmentEffort.java`

- [ ] **Step 1: Create the entity**

```java
package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "segment_efforts")
public class SegmentEffort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    @JsonIgnore
    private SegmentChallenge challenge;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EffortKind kind;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EffortCategory category;

    @Column(name = "elapsed_seconds", nullable = false)
    private Integer elapsedSeconds;

    @Column(name = "avg_speed_kmh")
    private Double avgSpeedKmh;

    @Column(name = "avg_pace_seconds_per_km")
    private Integer avgPaceSecondsPerKm;

    @Column(name = "avg_power_w")
    private Integer avgPowerW;

    @Column(name = "avg_hr")
    private Integer avgHr;

    @Column(name = "track_json", columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String trackJson;

    @Column(name = "source_format", length = 20)
    private String sourceFormat;

    @Column(name = "claimed_by_user_id")
    private Long claimedByUserId;

    @Column(name = "edit_token", length = 64)
    @JsonIgnore
    private String editToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EffortStatus status = EffortStatus.VALID;

    @Column(name = "ip_hash", length = 64)
    @JsonIgnore
    private String ipHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SegmentEffort() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SegmentChallenge getChallenge() { return challenge; }
    public void setChallenge(SegmentChallenge challenge) { this.challenge = challenge; }
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }
    public EffortKind getKind() { return kind; }
    public void setKind(EffortKind kind) { this.kind = kind; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public EffortCategory getCategory() { return category; }
    public void setCategory(EffortCategory category) { this.category = category; }
    public Integer getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(Integer elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
    public Double getAvgSpeedKmh() { return avgSpeedKmh; }
    public void setAvgSpeedKmh(Double avgSpeedKmh) { this.avgSpeedKmh = avgSpeedKmh; }
    public Integer getAvgPaceSecondsPerKm() { return avgPaceSecondsPerKm; }
    public void setAvgPaceSecondsPerKm(Integer avgPaceSecondsPerKm) { this.avgPaceSecondsPerKm = avgPaceSecondsPerKm; }
    public Integer getAvgPowerW() { return avgPowerW; }
    public void setAvgPowerW(Integer avgPowerW) { this.avgPowerW = avgPowerW; }
    public Integer getAvgHr() { return avgHr; }
    public void setAvgHr(Integer avgHr) { this.avgHr = avgHr; }
    public String getTrackJson() { return trackJson; }
    public void setTrackJson(String trackJson) { this.trackJson = trackJson; }
    public String getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(String sourceFormat) { this.sourceFormat = sourceFormat; }
    public Long getClaimedByUserId() { return claimedByUserId; }
    public void setClaimedByUserId(Long claimedByUserId) { this.claimedByUserId = claimedByUserId; }
    public String getEditToken() { return editToken; }
    public void setEditToken(String editToken) { this.editToken = editToken; }
    public EffortStatus getStatus() { return status; }
    public void setStatus(EffortStatus status) { this.status = status; }
    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/entity/SegmentEffort.java
git commit -m "Add SegmentEffort entity"
```

---

## Task 5: Liquibase migration 140 + master include

**Files:**
- Create: `backend/src/main/resources/db/changelog/changes/140-create-segment-challenge.xml`
- Modify: `backend/src/main/resources/db/changelog/db.changelog-master.xml` (append include after line 144)

> **Important (project rule):** `<preConditions onFail="MARK_RAN">` MUST come **before** `<comment>`, or Spring Boot crashes on startup.

- [ ] **Step 1: Create the migration file**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="140-1" author="benedikt">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="segment_challenges"/>
            </not>
        </preConditions>
        <comment>Create segment_challenges table for public segment campaigns (Heartbreak Hill)</comment>
        <createTable tableName="segment_challenges">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="slug" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="subtitle" type="VARCHAR(500)"/>
            <column name="event_date" type="DATE"/>
            <column name="start_lat" type="DOUBLE">
                <constraints nullable="false"/>
            </column>
            <column name="start_lng" type="DOUBLE">
                <constraints nullable="false"/>
            </column>
            <column name="end_lat" type="DOUBLE">
                <constraints nullable="false"/>
            </column>
            <column name="end_lng" type="DOUBLE">
                <constraints nullable="false"/>
            </column>
            <column name="distance_m" type="DOUBLE"/>
            <column name="elevation_gain_m" type="INT"/>
            <column name="avg_grade_pct" type="DOUBLE"/>
            <column name="max_grade_pct" type="DOUBLE"/>
            <column name="polyline_json" type="LONGTEXT"/>
            <column name="terrain_asset_ref" type="VARCHAR(255)"/>
            <column name="bounding_box_json" type="VARCHAR(500)"/>
            <column name="active" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP"/>
        </createTable>
    </changeSet>

    <changeSet id="140-2" author="benedikt">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="segment_efforts"/>
            </not>
        </preConditions>
        <comment>Create segment_efforts table for leaderboard entries (reference roster + public uploads)</comment>
        <createTable tableName="segment_efforts">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="challenge_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="activity_type" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="kind" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="display_name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="category" type="VARCHAR(30)"/>
            <column name="elapsed_seconds" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="avg_speed_kmh" type="DOUBLE"/>
            <column name="avg_pace_seconds_per_km" type="INT"/>
            <column name="avg_power_w" type="INT"/>
            <column name="avg_hr" type="INT"/>
            <column name="track_json" type="LONGTEXT"/>
            <column name="source_format" type="VARCHAR(20)"/>
            <column name="claimed_by_user_id" type="BIGINT"/>
            <column name="edit_token" type="VARCHAR(64)"/>
            <column name="status" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="ip_hash" type="VARCHAR(64)"/>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>
        <addForeignKeyConstraint baseTableName="segment_efforts" baseColumnNames="challenge_id"
                                 constraintName="fk_segment_efforts_challenge"
                                 referencedTableName="segment_challenges" referencedColumnNames="id"/>
        <createIndex tableName="segment_efforts" indexName="idx_segment_efforts_leaderboard">
            <column name="challenge_id"/>
            <column name="activity_type"/>
            <column name="status"/>
            <column name="elapsed_seconds"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Register the include in the master changelog**

In `backend/src/main/resources/db/changelog/db.changelog-master.xml`, add this line immediately after the `139-...` include (currently the last include, around line 144):

```xml
    <include file="db/changelog/changes/140-create-segment-challenge.xml"/>
```

- [ ] **Step 3: Verify the app boots and migration applies**

Run: `cd backend && mvn -q spring-boot:run` (let it start, then stop with Ctrl+C)
Expected: startup log shows changeSets `140-1` and `140-2` ran with no Liquibase error; no `preConditions`/startup crash.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/changelog/changes/140-create-segment-challenge.xml backend/src/main/resources/db/changelog/db.changelog-master.xml
git commit -m "Add Liquibase migration 140 for segment challenge tables"
```

---

## Task 6: Repositories

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/repository/SegmentChallengeRepository.java`
- Create: `backend/src/main/java/com/trainingsplan/repository/SegmentEffortRepository.java`

- [ ] **Step 1: Create SegmentChallengeRepository**

```java
package com.trainingsplan.repository;

import com.trainingsplan.entity.SegmentChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SegmentChallengeRepository extends JpaRepository<SegmentChallenge, Long> {
    Optional<SegmentChallenge> findBySlug(String slug);
}
```

- [ ] **Step 2: Create SegmentEffortRepository**

```java
package com.trainingsplan.repository;

import com.trainingsplan.entity.ActivityType;
import com.trainingsplan.entity.EffortStatus;
import com.trainingsplan.entity.SegmentEffort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SegmentEffortRepository extends JpaRepository<SegmentEffort, Long> {

    List<SegmentEffort> findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
            Long challengeId, ActivityType activityType, EffortStatus status);

    long countByChallengeIdAndActivityTypeAndStatus(
            Long challengeId, ActivityType activityType, EffortStatus status);

    long countByChallengeIdAndIpHashAndCreatedAtAfter(
            Long challengeId, String ipHash, LocalDateTime after);
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/repository/SegmentChallengeRepository.java backend/src/main/java/com/trainingsplan/repository/SegmentEffortRepository.java
git commit -m "Add segment challenge repositories"
```

---

## Task 7: Extend GpxParsingService with per-point elevation (TDD)

The matcher and the 3D ghost need elevation per trackpoint. `ParsedActivityData` currently lacks it.

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/service/ParsedActivityData.java`
- Modify: `backend/src/main/java/com/trainingsplan/service/GpxParsingService.java`
- Test: `backend/src/test/java/com/trainingsplan/service/GpxParsingServiceElevationTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.trainingsplan.service;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class GpxParsingServiceElevationTest {

    private static final String GPX = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
          <trk><name>t</name><type>cycling</type><trkseg>
            <trkpt lat="50.1780" lon="8.7400"><ele>100.0</ele><time>2026-06-01T08:00:00Z</time></trkpt>
            <trkpt lat="50.1783" lon="8.7400"><ele>110.0</ele><time>2026-06-01T08:00:05Z</time></trkpt>
            <trkpt lat="50.1786" lon="8.7400"><ele>125.5</ele><time>2026-06-01T08:00:10Z</time></trkpt>
          </trkseg></trk>
        </gpx>
        """;

    @Test
    void parse_populatesPerPointElevation() throws Exception {
        GpxParsingService svc = new GpxParsingService();
        ParsedActivityData data = svc.parse(GPX.getBytes(StandardCharsets.UTF_8));

        assertNotNull(data.elevations);
        assertEquals(3, data.elevations.size());
        assertEquals(100.0, data.elevations.get(0), 1e-9);
        assertEquals(110.0, data.elevations.get(1), 1e-9);
        assertEquals(125.5, data.elevations.get(2), 1e-9);
        // sanity: existing streams still aligned
        assertEquals(3, data.latLngPoints.size());
        assertEquals(3, data.timeSeconds.size());
        assertEquals(0, data.timeSeconds.get(0));
        assertEquals(10, data.timeSeconds.get(2));
    }

    @Test
    void parse_missingElevation_yieldsNullEntry() throws Exception {
        String noEle = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
              <trk><trkseg>
                <trkpt lat="50.1780" lon="8.7400"><time>2026-06-01T08:00:00Z</time></trkpt>
              </trkseg></trk>
            </gpx>
            """;
        ParsedActivityData data = new GpxParsingService().parse(noEle.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, data.elevations.size());
        assertNull(data.elevations.get(0));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -q test -Dtest=GpxParsingServiceElevationTest`
Expected: FAIL — `data.elevations` field does not exist (compilation error).

- [ ] **Step 3: Add the field to ParsedActivityData**

Replace the body of `backend/src/main/java/com/trainingsplan/service/ParsedActivityData.java` with:

```java
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
```

- [ ] **Step 4: Populate elevations in GpxParsingService**

In `GpxParsingService.parse(...)`, add a new list next to the existing ones (near the `latLngPointsList` declaration, ~line 45):

```java
        List<Double> elevationsList = new ArrayList<>();
```

Inside the trackpoint loop, immediately after the `ele` value is parsed (after the block that ends ~line 102, before the timestamp block), add:

```java
            elevationsList.add(ele); // may be null
```

Then, where the result is assembled (~line 220, after `result.latLngPoints = latLngPointsList;`), add:

```java
        result.elevations = elevationsList;
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn -q test -Dtest=GpxParsingServiceElevationTest`
Expected: PASS (2 tests).

- [ ] **Step 6: Run the full GPX-related suite to confirm no regressions**

Run: `cd backend && mvn -q test -Dtest=GpxParsingService*`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/ParsedActivityData.java backend/src/main/java/com/trainingsplan/service/GpxParsingService.java backend/src/test/java/com/trainingsplan/service/GpxParsingServiceElevationTest.java
git commit -m "Extend GPX parser with per-point elevation"
```

---

## Task 8: SegmentMatchResult + SegmentMatchingService (TDD)

The core: crop an uploaded track to the segment between the start/end gates and compute elapsed time.

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/service/SegmentMatchResult.java`
- Create: `backend/src/main/java/com/trainingsplan/service/SegmentMatchingService.java`
- Test: `backend/src/test/java/com/trainingsplan/service/SegmentMatchingServiceTest.java`

- [ ] **Step 1: Create the result value object**

```java
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
```

- [ ] **Step 2: Write the failing test**

```java
package com.trainingsplan.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmentMatchingServiceTest {

    private final SegmentMatchingService svc = new SegmentMatchingService();

    // Straight northbound track: 11 points, ~33 m apart, 5 s apart.
    private static final double BASE_LAT = 50.1780;
    private static final double LNG = 8.7400;

    private List<double[]> latLng(int n) {
        List<double[]> pts = new ArrayList<>();
        for (int i = 0; i < n; i++) pts.add(new double[]{BASE_LAT + i * 0.0003, LNG});
        return pts;
    }

    private List<Integer> times(int n) {
        List<Integer> t = new ArrayList<>();
        for (int i = 0; i < n; i++) t.add(i * 5);
        return t;
    }

    private List<Double> eles(int n) {
        List<Double> e = new ArrayList<>();
        for (int i = 0; i < n; i++) e.add(100.0 + i);
        return e;
    }

    @Test
    void match_trackThroughBothGatesInOrder_isMatched() {
        // start gate = point index 2, end gate = point index 8
        double startLat = BASE_LAT + 2 * 0.0003;
        double endLat = BASE_LAT + 8 * 0.0003;

        SegmentMatchResult r = svc.match(latLng(11), times(11), eles(11),
                startLat, LNG, endLat, LNG);

        assertTrue(r.isMatched(), () -> "expected match, got: " + r.getRejectionReason());
        assertEquals(30, r.getElapsedSeconds());   // (8-2)*5
        assertEquals(7, r.getCroppedTrack().size()); // indices 2..8 inclusive
        assertTrue(r.getAvgSpeedKmh() > 0);
        // cropped track relative seconds start at 0
        assertEquals(0.0, r.getCroppedTrack().get(0)[3], 1e-9);
        assertEquals(30.0, r.getCroppedTrack().get(6)[3], 1e-9);
    }

    @Test
    void match_startGateNeverApproached_isRejected() {
        double farLat = BASE_LAT + 1.0; // ~111 km away
        double endLat = BASE_LAT + 8 * 0.0003;

        SegmentMatchResult r = svc.match(latLng(11), times(11), eles(11),
                farLat, LNG, endLat, LNG);

        assertFalse(r.isMatched());
        assertNotNull(r.getRejectionReason());
    }

    @Test
    void match_endGateBeforeStartGate_wrongDirection_isRejected() {
        // start gate = point 8, end gate = point 2 → no end point AFTER the start index
        double startLat = BASE_LAT + 8 * 0.0003;
        double endLat = BASE_LAT + 2 * 0.0003;

        SegmentMatchResult r = svc.match(latLng(11), times(11), eles(11),
                startLat, LNG, endLat, LNG);

        assertFalse(r.isMatched());
        assertNotNull(r.getRejectionReason());
    }

    @Test
    void match_emptyTrack_isRejected() {
        SegmentMatchResult r = svc.match(List.of(), List.of(), List.of(),
                BASE_LAT, LNG, BASE_LAT, LNG);
        assertFalse(r.isMatched());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd backend && mvn -q test -Dtest=SegmentMatchingServiceTest`
Expected: FAIL — `SegmentMatchingService` does not exist (compilation error).

- [ ] **Step 4: Implement the service**

```java
package com.trainingsplan.service;

import com.trainingsplan.util.GeoUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches an uploaded GPS track against a segment defined by a start gate and an end gate.
 * The segment effort is the sub-track between the closest approach to the start gate and the
 * subsequent closest approach to the end gate.
 */
@Service
public class SegmentMatchingService {

    /** A track point must come within this distance (m) of a gate to count as crossing it. */
    private static final double GATE_RADIUS_M = 35.0;

    public SegmentMatchResult match(List<double[]> latLngPoints,
                                    List<Integer> timeSeconds,
                                    List<Double> elevations,
                                    double startLat, double startLng,
                                    double endLat, double endLng) {

        if (latLngPoints == null || latLngPoints.size() < 2
                || timeSeconds == null || timeSeconds.size() != latLngPoints.size()) {
            return SegmentMatchResult.rejected("track_too_short_or_misaligned");
        }

        int entryIdx = closestIndexWithinRadius(latLngPoints, 0, startLat, startLng);
        if (entryIdx < 0) {
            return SegmentMatchResult.rejected("start_gate_not_reached");
        }

        int exitIdx = closestIndexWithinRadius(latLngPoints, entryIdx + 1, endLat, endLng);
        if (exitIdx < 0) {
            return SegmentMatchResult.rejected("end_gate_not_reached_after_start");
        }

        int elapsed = timeSeconds.get(exitIdx) - timeSeconds.get(entryIdx);
        if (elapsed <= 0) {
            return SegmentMatchResult.rejected("non_positive_elapsed_time");
        }

        List<double[]> cropped = new ArrayList<>();
        double distanceM = 0.0;
        int baseTime = timeSeconds.get(entryIdx);
        double[] prev = null;
        for (int i = entryIdx; i <= exitIdx; i++) {
            double lat = latLngPoints.get(i)[0];
            double lng = latLngPoints.get(i)[1];
            double ele = (elevations != null && i < elevations.size() && elevations.get(i) != null)
                    ? elevations.get(i) : 0.0;
            int relSec = timeSeconds.get(i) - baseTime;
            cropped.add(new double[]{lat, lng, ele, relSec});
            if (prev != null) {
                distanceM += GeoUtils.haversineMeters(prev[0], prev[1], lat, lng);
            }
            prev = new double[]{lat, lng};
        }

        double distanceKm = distanceM / 1000.0;
        double avgSpeedKmh = distanceKm / (elapsed / 3600.0);
        int avgPace = distanceKm > 0 ? (int) Math.round(elapsed / distanceKm) : 0;

        return SegmentMatchResult.matched(elapsed, distanceKm, avgSpeedKmh, avgPace, cropped);
    }

    /**
     * Returns the index (>= fromIdx) of the point closest to the gate, or -1 if no point
     * comes within {@link #GATE_RADIUS_M}.
     */
    private int closestIndexWithinRadius(List<double[]> points, int fromIdx, double gateLat, double gateLng) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = fromIdx; i < points.size(); i++) {
            double d = GeoUtils.haversineMeters(points.get(i)[0], points.get(i)[1], gateLat, gateLng);
            if (d <= GATE_RADIUS_M && d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn -q test -Dtest=SegmentMatchingServiceTest`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/SegmentMatchResult.java backend/src/main/java/com/trainingsplan/service/SegmentMatchingService.java backend/src/test/java/com/trainingsplan/service/SegmentMatchingServiceTest.java
git commit -m "Add segment matching service with gate-based cropping"
```

---

## Task 9: DTOs

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/dto/SegmentChallengeDto.java`
- Create: `backend/src/main/java/com/trainingsplan/dto/SegmentLeaderboardEntryDto.java`
- Create: `backend/src/main/java/com/trainingsplan/dto/SegmentEffortResultDto.java`
- Create: `backend/src/main/java/com/trainingsplan/dto/SegmentTrackDto.java`

> Named `Segment*` to avoid colliding with the existing `LeaderboardEntryDto` used by the community-routes module.

- [ ] **Step 1: Create the four DTO records**

`SegmentChallengeDto.java`:
```java
package com.trainingsplan.dto;

import java.time.LocalDate;

public record SegmentChallengeDto(
        Long id,
        String slug,
        String name,
        String subtitle,
        LocalDate eventDate,
        Double distanceM,
        Integer elevationGainM,
        Double avgGradePct,
        Double maxGradePct,
        String polylineJson,
        String terrainAssetRef,
        long rideCount,
        long runCount
) {}
```

`SegmentLeaderboardEntryDto.java`:
```java
package com.trainingsplan.dto;

public record SegmentLeaderboardEntryDto(
        Long effortId,
        int rank,
        String displayName,
        String kind,
        String category,
        int elapsedSeconds,
        String elapsedFormatted,
        Integer gapToLeaderSeconds,
        Double avgSpeedKmh,
        Integer avgPaceSecondsPerKm,
        boolean reference
) {}
```

`SegmentEffortResultDto.java`:
```java
package com.trainingsplan.dto;

public record SegmentEffortResultDto(
        Long effortId,
        String editToken,
        int rank,
        long totalCount,
        int elapsedSeconds,
        String elapsedFormatted,
        Integer gapToLeaderSeconds,
        double percentileBeaten,
        String status
) {}
```

`SegmentTrackDto.java`:
```java
package com.trainingsplan.dto;

public record SegmentTrackDto(
        Long effortId,
        String activityType,
        String trackJson
) {}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/dto/SegmentChallengeDto.java backend/src/main/java/com/trainingsplan/dto/SegmentLeaderboardEntryDto.java backend/src/main/java/com/trainingsplan/dto/SegmentEffortResultDto.java backend/src/main/java/com/trainingsplan/dto/SegmentTrackDto.java
git commit -m "Add segment challenge DTOs"
```

---

## Task 10: SegmentChallengeService — leaderboard ranking (TDD)

Build the service incrementally. First the pure leaderboard/ranking logic (testable without a DB), then the orchestration methods.

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java`
- Test: `backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceLeaderboardTest.java`

- [ ] **Step 1: Write the failing test (ranking + formatting helpers)**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -q test -Dtest=SegmentChallengeServiceLeaderboardTest`
Expected: FAIL — `SegmentChallengeService` does not exist.

- [ ] **Step 3: Create the service with the static helpers (minimal to pass)**

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -q test -Dtest=SegmentChallengeServiceLeaderboardTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceLeaderboardTest.java
git commit -m "Add segment leaderboard ranking logic"
```

---

## Task 11: SegmentChallengeService — orchestration methods

Add the Spring-managed orchestration (DB + parsing + matching) to the service created in Task 10.

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java`

- [ ] **Step 1: Convert to a Spring `@Service` with dependencies and add orchestration methods**

Replace the class declaration and add the dependency-injected instance methods. The final file is:

```java
package com.trainingsplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.SegmentChallengeDto;
import com.trainingsplan.dto.SegmentEffortResultDto;
import com.trainingsplan.dto.SegmentLeaderboardEntryDto;
import com.trainingsplan.dto.SegmentTrackDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.SegmentChallengeRepository;
import com.trainingsplan.repository.SegmentEffortRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SegmentChallengeService {

    private static final int MAX_PUBLIC_SUBMITS_PER_IP_PER_HOUR = 20;
    // Plausibility ceilings for the segment effort average speed.
    private static final double MAX_RIDE_KMH = 90.0;
    private static final double MAX_RUN_KMH = 32.0;

    private final SegmentChallengeRepository challengeRepository;
    private final SegmentEffortRepository effortRepository;
    private final GpxParsingService gpxParsingService;
    private final SegmentMatchingService matchingService;
    private final ObjectMapper objectMapper;

    public SegmentChallengeService(SegmentChallengeRepository challengeRepository,
                                   SegmentEffortRepository effortRepository,
                                   GpxParsingService gpxParsingService,
                                   SegmentMatchingService matchingService,
                                   ObjectMapper objectMapper) {
        this.challengeRepository = challengeRepository;
        this.effortRepository = effortRepository;
        this.gpxParsingService = gpxParsingService;
        this.matchingService = matchingService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SegmentChallengeDto getChallenge(String slug) {
        SegmentChallenge c = requireChallenge(slug);
        long rideCount = effortRepository.countByChallengeIdAndActivityTypeAndStatus(
                c.getId(), ActivityType.RIDE, EffortStatus.VALID);
        long runCount = effortRepository.countByChallengeIdAndActivityTypeAndStatus(
                c.getId(), ActivityType.RUN, EffortStatus.VALID);
        return new SegmentChallengeDto(c.getId(), c.getSlug(), c.getName(), c.getSubtitle(),
                c.getEventDate(), c.getDistanceM(), c.getElevationGainM(), c.getAvgGradePct(),
                c.getMaxGradePct(), c.getPolylineJson(), c.getTerrainAssetRef(), rideCount, runCount);
    }

    @Transactional(readOnly = true)
    public List<SegmentLeaderboardEntryDto> getLeaderboard(String slug, ActivityType type) {
        SegmentChallenge c = requireChallenge(slug);
        List<SegmentEffort> efforts = effortRepository
                .findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                        c.getId(), type, EffortStatus.VALID);
        return buildLeaderboard(efforts);
    }

    @Transactional(readOnly = true)
    public SegmentTrackDto getEffortTrack(Long effortId) {
        SegmentEffort e = effortRepository.findById(effortId)
                .orElseThrow(() -> new IllegalArgumentException("effort_not_found"));
        return new SegmentTrackDto(e.getId(), e.getActivityType().name(), e.getTrackJson());
    }

    @Transactional
    public SegmentEffortResultDto submitPublicEffort(String slug, ActivityType type, String displayName,
                                                     byte[] fileBytes, String originalFilename, String clientIp) {
        SegmentChallenge c = requireChallenge(slug);

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("display_name_required");
        }
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".gpx")) {
            throw new IllegalArgumentException("only_gpx_supported");
        }
        String ipHash = hashIp(clientIp);
        if (effortRepository.countByChallengeIdAndIpHashAndCreatedAtAfter(
                c.getId(), ipHash, LocalDateTime.now().minusHours(1)) >= MAX_PUBLIC_SUBMITS_PER_IP_PER_HOUR) {
            throw new IllegalStateException("rate_limit_exceeded");
        }

        SegmentMatchResult match = matchTrack(c, fileBytes);
        if (!match.isMatched()) {
            throw new IllegalArgumentException(match.getRejectionReason());
        }
        double maxKmh = type == ActivityType.RIDE ? MAX_RIDE_KMH : MAX_RUN_KMH;
        if (match.getAvgSpeedKmh() > maxKmh) {
            throw new IllegalArgumentException("implausible_speed");
        }

        SegmentEffort e = new SegmentEffort();
        e.setChallenge(c);
        e.setActivityType(type);
        e.setKind(EffortKind.PUBLIC);
        e.setCategory(EffortCategory.COMMUNITY);
        e.setDisplayName(displayName.trim());
        e.setElapsedSeconds(match.getElapsedSeconds());
        e.setAvgSpeedKmh(match.getAvgSpeedKmh());
        e.setAvgPaceSecondsPerKm(match.getAvgPaceSecondsPerKm());
        e.setTrackJson(serializeTrack(match.getCroppedTrack()));
        e.setSourceFormat("GPX");
        e.setStatus(EffortStatus.VALID);
        e.setEditToken(UUID.randomUUID().toString());
        e.setIpHash(ipHash);
        e.setCreatedAt(LocalDateTime.now());
        effortRepository.save(e);

        return buildResult(c, type, e);
    }

    @Transactional
    public Long addReferenceEffort(String slug, ActivityType type, String displayName,
                                   EffortCategory category, byte[] fileBytes, String originalFilename,
                                   Integer knownTimeSecondsOverride) {
        SegmentChallenge c = requireChallenge(slug);
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".gpx")) {
            throw new IllegalArgumentException("only_gpx_supported");
        }
        SegmentMatchResult match = matchTrack(c, fileBytes);
        if (!match.isMatched()) {
            throw new IllegalArgumentException(match.getRejectionReason());
        }

        SegmentEffort e = new SegmentEffort();
        e.setChallenge(c);
        e.setActivityType(type);
        e.setKind(EffortKind.REFERENCE);
        e.setCategory(category);
        e.setDisplayName(displayName.trim());
        e.setElapsedSeconds(knownTimeSecondsOverride != null ? knownTimeSecondsOverride : match.getElapsedSeconds());
        e.setAvgSpeedKmh(match.getAvgSpeedKmh());
        e.setAvgPaceSecondsPerKm(match.getAvgPaceSecondsPerKm());
        e.setTrackJson(serializeTrack(match.getCroppedTrack()));
        e.setSourceFormat("GPX");
        e.setStatus(EffortStatus.VALID);
        e.setCreatedAt(LocalDateTime.now());
        effortRepository.save(e);
        return e.getId();
    }

    @Transactional
    public void claimEffort(Long effortId, String editToken, Long userId) {
        SegmentEffort e = effortRepository.findById(effortId)
                .orElseThrow(() -> new IllegalArgumentException("effort_not_found"));
        if (e.getEditToken() == null || !e.getEditToken().equals(editToken)) {
            throw new IllegalArgumentException("invalid_edit_token");
        }
        e.setClaimedByUserId(userId);
        effortRepository.save(e);
    }

    @Transactional
    public void setEffortStatus(Long effortId, EffortStatus status) {
        SegmentEffort e = effortRepository.findById(effortId)
                .orElseThrow(() -> new IllegalArgumentException("effort_not_found"));
        e.setStatus(status);
        effortRepository.save(e);
    }

    // ---- internal helpers -------------------------------------------------

    private SegmentChallenge requireChallenge(String slug) {
        return challengeRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("challenge_not_found"));
    }

    private SegmentMatchResult matchTrack(SegmentChallenge c, byte[] fileBytes) {
        try {
            ParsedActivityData data = gpxParsingService.parse(fileBytes);
            return matchingService.match(data.latLngPoints, data.timeSeconds, data.elevations,
                    c.getStartLat(), c.getStartLng(), c.getEndLat(), c.getEndLng());
        } catch (Exception ex) {
            return SegmentMatchResult.rejected("unparseable_file");
        }
    }

    private SegmentEffortResultDto buildResult(SegmentChallenge c, ActivityType type, SegmentEffort saved) {
        List<SegmentEffort> efforts = effortRepository
                .findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                        c.getId(), type, EffortStatus.VALID);
        efforts.sort(Comparator.comparingInt(SegmentEffort::getElapsedSeconds));
        long total = efforts.size();
        int rank = 1;
        int slower = 0;
        for (SegmentEffort e : efforts) {
            if (e.getElapsedSeconds() < saved.getElapsedSeconds()) rank++;
            if (e.getElapsedSeconds() > saved.getElapsedSeconds()) slower++;
        }
        Integer leaderTime = efforts.isEmpty() ? null : efforts.get(0).getElapsedSeconds();
        int gap = leaderTime == null ? 0 : saved.getElapsedSeconds() - leaderTime;
        double percentileBeaten = total > 1 ? (slower * 100.0) / (total - 1) : 100.0;

        return new SegmentEffortResultDto(saved.getId(), saved.getEditToken(), rank, total,
                saved.getElapsedSeconds(), formatElapsed(saved.getElapsedSeconds()), gap,
                Math.round(percentileBeaten * 10) / 10.0, saved.getStatus().name());
    }

    private String serializeTrack(List<double[]> track) {
        try {
            return objectMapper.writeValueAsString(track);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String hashIp(String ip) {
        String raw = (ip == null ? "unknown" : ip) + "|heartbreak-salt";
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    // ---- pure leaderboard helpers (kept from Task 10) ---------------------

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

    public static String formatElapsed(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return m + ":" + (s < 10 ? "0" + s : Integer.toString(s));
    }
}
```

- [ ] **Step 2: Run the leaderboard test again to confirm no regression**

Run: `cd backend && mvn -q test -Dtest=SegmentChallengeServiceLeaderboardTest`
Expected: PASS (2 tests) — the static helpers still behave identically.

- [ ] **Step 3: Verify compilation of the whole module**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java
git commit -m "Add segment challenge orchestration (submit, roster, claim, moderation)"
```

---

## Task 12: SecurityConfig — open /api/public/**

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/config/SecurityConfig.java`

- [ ] **Step 1: Add the permitAll matcher**

In `securityFilterChain(...)`, inside the `.authorizeHttpRequests(...)` block, add this line immediately **before** `.requestMatchers("/api/admin/**").hasRole("ADMIN")` (currently line 70):

```java
                .requestMatchers("/api/public/**").permitAll()
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/config/SecurityConfig.java
git commit -m "Permit public access to /api/public/** endpoints"
```

---

## Task 13: PublicSegmentChallengeController (TDD via MockMvc)

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/controller/PublicSegmentChallengeController.java`
- Test: `backend/src/test/java/com/trainingsplan/controller/PublicSegmentChallengeControllerTest.java`

- [ ] **Step 1: Write the failing test (slice test with mocked service)**

```java
package com.trainingsplan.controller;

import com.trainingsplan.dto.SegmentEffortResultDto;
import com.trainingsplan.entity.ActivityType;
import com.trainingsplan.service.SegmentChallengeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicSegmentChallengeController.class)
@WithMockUser
class PublicSegmentChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SegmentChallengeService service;

    @Test
    void submitEffort_returnsResultJson() throws Exception {
        when(service.submitPublicEffort(eq("heartbreak-hill-2026"), eq(ActivityType.RIDE),
                eq("Lukas"), any(), eq("ride.gpx"), any()))
                .thenReturn(new SegmentEffortResultDto(7L, "tok", 47, 312, 298, "4:58", 46, 85.0, "VALID"));

        MockMultipartFile file = new MockMultipartFile("file", "ride.gpx",
                "application/gpx+xml", "<gpx/>".getBytes());

        mockMvc.perform(multipart("/api/public/challenges/heartbreak-hill-2026/efforts")
                        .file(file)
                        .param("displayName", "Lukas")
                        .param("type", "RIDE")
                        .with(req -> { req.setRemoteAddr("1.2.3.4"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(47))
                .andExpect(jsonPath("$.elapsedFormatted").value("4:58"));
    }

    @Test
    void submitEffort_rejectedMatch_returns422() throws Exception {
        when(service.submitPublicEffort(any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("start_gate_not_reached"));

        MockMultipartFile file = new MockMultipartFile("file", "ride.gpx",
                "application/gpx+xml", "<gpx/>".getBytes());

        mockMvc.perform(multipart("/api/public/challenges/heartbreak-hill-2026/efforts")
                        .file(file)
                        .param("displayName", "Lukas")
                        .param("type", "RIDE"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reason").value("start_gate_not_reached"));
    }
}
```

> Note: `@WebMvcTest` pulls in the security filter chain. `@WithMockUser` satisfies it without needing the real JWT filter; this test verifies controller behaviour and JSON shape, not the permitAll wiring (which is verified by manual smoke test in Task 16).

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -q test -Dtest=PublicSegmentChallengeControllerTest`
Expected: FAIL — controller does not exist.

- [ ] **Step 3: Implement the controller**

```java
package com.trainingsplan.controller;

import com.trainingsplan.dto.SegmentChallengeDto;
import com.trainingsplan.dto.SegmentEffortResultDto;
import com.trainingsplan.dto.SegmentLeaderboardEntryDto;
import com.trainingsplan.dto.SegmentTrackDto;
import com.trainingsplan.entity.ActivityType;
import com.trainingsplan.service.SegmentChallengeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/challenges")
public class PublicSegmentChallengeController {

    private final SegmentChallengeService service;

    public PublicSegmentChallengeController(SegmentChallengeService service) {
        this.service = service;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getChallenge(@PathVariable String slug) {
        try {
            SegmentChallengeDto dto = service.getChallenge(slug);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{slug}/leaderboard")
    public ResponseEntity<?> getLeaderboard(@PathVariable String slug,
                                            @RequestParam(defaultValue = "RIDE") ActivityType type) {
        try {
            List<SegmentLeaderboardEntryDto> board = service.getLeaderboard(slug, type);
            return ResponseEntity.ok(board);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{slug}/efforts/{id}/track")
    public ResponseEntity<?> getTrack(@PathVariable String slug, @PathVariable Long id) {
        try {
            SegmentTrackDto dto = service.getEffortTrack(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{slug}/efforts")
    public ResponseEntity<?> submitEffort(@PathVariable String slug,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam("displayName") String displayName,
                                          @RequestParam("type") ActivityType type,
                                          HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "empty_file"));
        }
        try {
            SegmentEffortResultDto result = service.submitPublicEffort(
                    slug, type, displayName, file.getBytes(), file.getOriginalFilename(),
                    request.getRemoteAddr());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("reason", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("reason", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("reason", "io_error"));
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -q test -Dtest=PublicSegmentChallengeControllerTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/controller/PublicSegmentChallengeController.java backend/src/test/java/com/trainingsplan/controller/PublicSegmentChallengeControllerTest.java
git commit -m "Add public segment challenge controller"
```

---

## Task 14: AdminSegmentChallengeController

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/controller/AdminSegmentChallengeController.java`

Provides challenge creation/update and reference-roster upload. Lives under `/api/admin/**`, which `SecurityConfig` already restricts to `hasRole("ADMIN")`.

- [ ] **Step 1: Implement the controller**

```java
package com.trainingsplan.controller;

import com.trainingsplan.entity.ActivityType;
import com.trainingsplan.entity.EffortCategory;
import com.trainingsplan.entity.EffortStatus;
import com.trainingsplan.entity.SegmentChallenge;
import com.trainingsplan.repository.SegmentChallengeRepository;
import com.trainingsplan.service.SegmentChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/challenges")
public class AdminSegmentChallengeController {

    private final SegmentChallengeRepository challengeRepository;
    private final SegmentChallengeService service;

    public AdminSegmentChallengeController(SegmentChallengeRepository challengeRepository,
                                           SegmentChallengeService service) {
        this.challengeRepository = challengeRepository;
        this.service = service;
    }

    /** Create or update a challenge by slug (idempotent on slug). */
    @PostMapping
    public ResponseEntity<?> upsert(@RequestBody SegmentChallenge body) {
        SegmentChallenge c = challengeRepository.findBySlug(body.getSlug()).orElseGet(SegmentChallenge::new);
        c.setSlug(body.getSlug());
        c.setName(body.getName());
        c.setSubtitle(body.getSubtitle());
        c.setEventDate(body.getEventDate());
        c.setStartLat(body.getStartLat());
        c.setStartLng(body.getStartLng());
        c.setEndLat(body.getEndLat());
        c.setEndLng(body.getEndLng());
        c.setDistanceM(body.getDistanceM());
        c.setElevationGainM(body.getElevationGainM());
        c.setAvgGradePct(body.getAvgGradePct());
        c.setMaxGradePct(body.getMaxGradePct());
        c.setPolylineJson(body.getPolylineJson());
        c.setTerrainAssetRef(body.getTerrainAssetRef());
        c.setBoundingBoxJson(body.getBoundingBoxJson());
        c.setActive(body.isActive());
        if (c.getId() == null) {
            c.setCreatedAt(LocalDateTime.now());
        }
        c.setUpdatedAt(LocalDateTime.now());
        challengeRepository.save(c);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", c.getId(), "slug", c.getSlug()));
    }

    /** Upload a named reference effort ("one of the greats"). */
    @PostMapping("/{slug}/reference-efforts")
    public ResponseEntity<?> addReferenceEffort(@PathVariable String slug,
                                                @RequestParam("file") MultipartFile file,
                                                @RequestParam("displayName") String displayName,
                                                @RequestParam("type") ActivityType type,
                                                @RequestParam("category") EffortCategory category,
                                                @RequestParam(value = "knownTimeSeconds", required = false) Integer knownTimeSeconds) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "empty_file"));
        }
        try {
            Long id = service.addReferenceEffort(slug, type, displayName, category,
                    file.getBytes(), file.getOriginalFilename(), knownTimeSeconds);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("effortId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("reason", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("reason", "io_error"));
        }
    }

    /** Hide/reject (moderate) an effort. */
    @PutMapping("/efforts/{id}/status")
    public ResponseEntity<?> setStatus(@PathVariable Long id, @RequestParam EffortStatus status) {
        try {
            service.setEffortStatus(id, status);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/controller/AdminSegmentChallengeController.java
git commit -m "Add admin controller for challenge upsert and reference roster"
```

---

## Task 15: SegmentEffortController (authenticated claim)

The Hybrid-Funnel "claim" step: after a user signs up / logs in, link their anonymous effort to their account. This endpoint is authenticated (it falls under `anyRequest().authenticated()` since it is not under `/api/public/**`).

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/controller/SegmentEffortController.java`

- [ ] **Step 1: Implement the controller**

```java
package com.trainingsplan.controller;

import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.SegmentChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/segment-efforts")
public class SegmentEffortController {

    private final SegmentChallengeService service;
    private final SecurityUtils securityUtils;

    public SegmentEffortController(SegmentChallengeService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    /** Claim an anonymous effort for the authenticated user, using its edit token. */
    @PostMapping("/{id}/claim")
    public ResponseEntity<?> claim(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            service.claimEffort(id, body.get("editToken"), user.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/controller/SegmentEffortController.java
git commit -m "Add authenticated claim endpoint for segment efforts"
```

---

## Task 16: End-to-end smoke test + seed the Heartbreak Hill challenge

Verify the public flow works against a running app, and create the real challenge row. (Run with the local MariaDB per CLAUDE.md, or H2.)

- [ ] **Step 1: Boot the app**

Run: `cd backend && mvn -q spring-boot:run` (leave running in a second terminal)
Expected: starts on :8080, migration 140 applied.

- [ ] **Step 2: Create the challenge via the admin endpoint**

> Requires an ADMIN JWT in `$TOKEN`. Coordinates/polyline below are placeholders — replace with the real Strava-segment values before launch (tracked in the spec's "Annahmen / Offene Punkte").

```bash
curl -s -X POST http://localhost:8080/api/admin/challenges \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"slug":"heartbreak-hill-2026","name":"Heartbreak Hill","subtitle":"Ironman Frankfurt 2026","eventDate":"2026-06-28","startLat":50.1748,"startLng":8.7360,"endLat":50.1792,"endLng":8.7405,"active":true}'
```
Expected: `201` with `{"id":...,"slug":"heartbreak-hill-2026"}`.

- [ ] **Step 3: Verify the public challenge endpoint is reachable WITHOUT auth**

```bash
curl -s http://localhost:8080/api/public/challenges/heartbreak-hill-2026
```
Expected: `200` JSON with `slug`, `rideCount: 0`, `runCount: 0`. (No `401` — confirms permitAll wiring from Task 12.)

- [ ] **Step 4: Verify a public GPX upload ranks (using a real Heartbreak Hill GPX)**

```bash
curl -s -X POST http://localhost:8080/api/public/challenges/heartbreak-hill-2026/efforts \
  -F "file=@/path/to/heartbreak.gpx" -F "displayName=Tester" -F "type=RIDE"
```
Expected: `200` JSON with `rank`, `elapsedFormatted`, `editToken`. If the GPX does not cover the segment, expect `422` with `{"reason":"start_gate_not_reached"}` (adjust gate coords in Step 2 accordingly).

- [ ] **Step 5: Stop the app, then bump version + changelog**

- Bump version: `./version-bump.sh minor` (new public endpoints + feature).
- Add to `CHANGELOG.md` under `[Unreleased]`:

```markdown
### Added
- Public Heartbreak Hill Challenge backend: anonymous GPX upload, segment matching, and a leaderboard mixing an admin-curated reference roster with public uploads (`/api/public/challenges/**`).
```

- [ ] **Step 6: Run the full backend test suite**

Run: `cd backend && mvn -q test`
Expected: BUILD SUCCESS (all tests pass).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Bump version and changelog for Heartbreak Hill backend"
```

---

## Self-Review

**Spec coverage** (against `2026-06-16-heartbreak-hill-challenge-design.md`):
- §4 Data model — `SegmentChallenge` (Task 3), `SegmentEffort` (Task 4), enums (Task 2), migration 140 (Task 5), leaderboard index (Task 5). ✓
- §5 `SegmentMatchingService` — Task 8 (gate crop + elapsed + plausibility split into service in Task 11). ✓
- §5 `SegmentChallengeService` (getBySlug, leaderboard, submit, getEffortTrack, addReferenceEffort, claim) — Tasks 10–11. ✓
- §5 GPX parser per-point elevation extension — Task 7. ✓
- §5 Endpoints (public GET/leaderboard/track/POST submit; admin upsert/reference; claim) — Tasks 13–15. ✓
- §5 Security `/api/public/**` permitAll + anti-abuse (ipHash, rate limit, plausibility, moderation status) — Tasks 11–12. ✓
- §9 Phase 1 backend — fully covered; the 2D page and 3D are Plans 2 and 3.

**Out of scope here (correct):** FIT/TCX public upload (GPX only per spec phasing — `submitPublicEffort` rejects non-`.gpx`); Strava segment-efforts auto-import (Phase 3); the frontend page (Plan 2); the 3D terrain (Plan 3).

**Type consistency:** `ActivityType`/`EffortKind`/`EffortStatus`/`EffortCategory` used identically across entity, repository method names, service, and controllers. `buildLeaderboard`/`formatElapsed` defined in Task 10 and preserved verbatim in Task 11's final file. `SegmentMatchResult` factory methods (`matched`/`rejected`) match their usage in `SegmentMatchingService` and `SegmentChallengeService`. DTO names prefixed `Segment*` to avoid the existing `LeaderboardEntryDto` collision.

**Placeholder scan:** segment coordinates/polyline in Task 16 are explicitly flagged as placeholders to replace with real Strava-segment data before launch (tracked in the spec). No `TODO`/`TBD` in code.
