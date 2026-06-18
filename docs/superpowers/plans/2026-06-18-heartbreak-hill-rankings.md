# Heartbreak Hill — Erweiterte Rankings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die Heartbreak-Hill-Bestenliste um geschlechtergetrennte Wertung, Altersklassen (Ironman-5-Jahres, geschlechtergetrennt) und eine Versuche-Wertung (Badge + eigenes Leaderboard) erweitern; Demografie optional beim Upload, AK live aus dem Geburtsjahr berechnet.

**Architecture:** Drei neue Spalten auf `segment_efforts` (`gender`, `birth_year`, `attempt_count`); Ranglisten werden zur Laufzeit aus diesen Feldern gefiltert/sortiert (kein Historie-Schema, keine materialisierten Ränge). Die Altersklasse wird nie persistiert, sondern aus dem Geburtsjahr + Referenzjahr berechnet.

**Tech Stack:** Spring Boot 3.2 / Java 21 / Liquibase / MariaDB (Backend), Angular 19 standalone / ngx-translate / Vitest (Frontend). Tests: JUnit5 + Mockito (Backend, kein Spring-Context — `standaloneSetup`/pure Unit), Vitest + HttpTestingController (Frontend).

**Spec:** `docs/superpowers/specs/2026-06-18-heartbreak-hill-rankings-design.md`

**Konventionen aus dem Projektgedächtnis (beachten):**
- Lokale Arbeit committet **direkt auf `main`**, kein Feature-Branch, kein PR.
- Liquibase: `<preConditions onFail="MARK_RAN">` **vor** `<comment>`. Master-Changelog `db.changelog-master.xml` immer um den neuen Include erweitern. Höchste Nummer aktuell `143` → neue Migration `144`.
- Maven braucht JDK 21 inline (System-`JAVA_HOME` zeigt fälschlich auf JDK 17). Auf Windows z.B.:
  `JAVA_HOME="C:\Program Files\Java\jdk-21" mvn -q -f backend/pom.xml test` (Pfad an die lokale JDK-21-Installation anpassen).
- Frontend nutzt **Vitest**, nicht Karma. Test-Run: `cd frontend && npm test`.
- Jeder neue i18n-Key muss in **`de.json` und `en.json`** stehen.

---

## File Structure

**Backend — neu:**
- `backend/src/main/java/com/trainingsplan/entity/Gender.java` — Enum MALE/FEMALE/DIVERS
- `backend/src/main/java/com/trainingsplan/entity/AgeGroup.java` — Ironman-5-Jahres-Buckets + Live-Berechnung aus Geburtsjahr
- `backend/src/main/java/com/trainingsplan/entity/LeaderboardScope.java` — Enum OVERALL/MEN/WOMEN/MOST_ATTEMPTS
- `backend/src/main/resources/db/changelog/changes/144-add-segment-effort-demographics.xml` — Migration
- `backend/src/test/java/com/trainingsplan/entity/AgeGroupTest.java` — Unit-Test AK-Logik

**Backend — geändert:**
- `entity/SegmentEffort.java` — Felder `gender`, `birthYear`, `attemptCount`
- `dto/SegmentLeaderboardEntryDto.java` — Felder `gender`, `ageGroup`, `attemptCount`
- `service/SegmentChallengeService.java` — Filter/Scope-Logik, Versuchszähler, Upload-Signatur, AK-DTO-Mapping
- `controller/PublicSegmentChallengeController.java` — Leaderboard `scope`/`ageGroup`, Upload `gender`/`birthYear`
- `controller/AdminSegmentChallengeController.java` — Referenz-Effort `gender`/`birthYear`
- `resources/db/changelog/db.changelog-master.xml` — Include 144
- Tests: `SegmentChallengeServiceLeaderboardTest`, `SegmentChallengeServiceDedupeTest`, `PublicSegmentChallengeControllerTest`

**Frontend — geändert:**
- `models/heartbreak-hill.model.ts` — Typen + `LeaderboardEntry`-Felder + `AGE_GROUPS`
- `services/heartbreak-hill.service.ts` — `getLeaderboard(type, scope, ageGroup)`, `submitEffort(..., gender, birthYear)`
- `components/heartbreak-hill/heartbreak-hill.ts` — Signals + Lade-/Submit-Logik
- `components/heartbreak-hill/heartbreak-hill.html` — Filter-Leiste, AK-Dropdown, Demografie-Felder, Versuche-Badge
- `components/heartbreak-hill/heartbreak-hill.scss` — Styles
- `assets/i18n/de.json`, `assets/i18n/en.json` — neue Keys
- Tests: `services/heartbreak-hill.service.spec.ts`

**Abschluss:**
- `version-bump.sh minor`, `CHANGELOG.md`

---

## Task 1: Enums `Gender`, `AgeGroup`, `LeaderboardScope` (+ AK-Logik mit Tests)

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/entity/Gender.java`
- Create: `backend/src/main/java/com/trainingsplan/entity/AgeGroup.java`
- Create: `backend/src/main/java/com/trainingsplan/entity/LeaderboardScope.java`
- Test: `backend/src/test/java/com/trainingsplan/entity/AgeGroupTest.java`

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/trainingsplan/entity/AgeGroupTest.java`:
```java
package com.trainingsplan.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgeGroupTest {

    @Test
    void forAge_mapsBoundariesToCorrectBucket() {
        assertEquals(AgeGroup.U18, AgeGroup.forAge(17));
        assertEquals(AgeGroup.AG_18_24, AgeGroup.forAge(18));
        assertEquals(AgeGroup.AG_18_24, AgeGroup.forAge(24));
        assertEquals(AgeGroup.AG_25_29, AgeGroup.forAge(25));
        assertEquals(AgeGroup.AG_65_69, AgeGroup.forAge(69));
        assertEquals(AgeGroup.AG_70_PLUS, AgeGroup.forAge(70));
        assertEquals(AgeGroup.AG_70_PLUS, AgeGroup.forAge(95));
    }

    @Test
    void fromBirthYear_usesReferenceYear() {
        // 1986 at reference year 2026 → age 40 → 40-44
        assertEquals(AgeGroup.AG_40_44, AgeGroup.fromBirthYear(1986, 2026));
        // 2009 at 2026 → age 17 → U18
        assertEquals(AgeGroup.U18, AgeGroup.fromBirthYear(2009, 2026));
    }

    @Test
    void fromKey_resolvesStableApiKeys() {
        assertEquals(AgeGroup.AG_30_34, AgeGroup.fromKey("30-34"));
        assertEquals(AgeGroup.AG_70_PLUS, AgeGroup.fromKey("70+"));
        assertEquals(AgeGroup.U18, AgeGroup.fromKey("U18"));
        assertNull(AgeGroup.fromKey("nonsense"));
        assertNull(AgeGroup.fromKey(null));
    }

    @Test
    void getKey_returnsStableApiKey() {
        assertEquals("30-34", AgeGroup.AG_30_34.getKey());
        assertEquals("70+", AgeGroup.AG_70_PLUS.getKey());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test -Dtest=AgeGroupTest`
Expected: FAIL — compilation error, `AgeGroup` does not exist.

- [ ] **Step 3: Write the three enums**

`backend/src/main/java/com/trainingsplan/entity/Gender.java`:
```java
package com.trainingsplan.entity;

public enum Gender { MALE, FEMALE, DIVERS }
```

`backend/src/main/java/com/trainingsplan/entity/LeaderboardScope.java`:
```java
package com.trainingsplan.entity;

/** Which ranking slice the public leaderboard endpoint returns. */
public enum LeaderboardScope { OVERALL, MEN, WOMEN, MOST_ATTEMPTS }
```

`backend/src/main/java/com/trainingsplan/entity/AgeGroup.java`:
```java
package com.trainingsplan.entity;

/** Ironman-style 5-year age groups. Stable {@code key} is the public API/UI value. */
public enum AgeGroup {
    U18("U18", 0, 17),
    AG_18_24("18-24", 18, 24),
    AG_25_29("25-29", 25, 29),
    AG_30_34("30-34", 30, 34),
    AG_35_39("35-39", 35, 39),
    AG_40_44("40-44", 40, 44),
    AG_45_49("45-49", 45, 49),
    AG_50_54("50-54", 50, 54),
    AG_55_59("55-59", 55, 59),
    AG_60_64("60-64", 60, 64),
    AG_65_69("65-69", 65, 69),
    AG_70_PLUS("70+", 70, Integer.MAX_VALUE);

    private final String key;
    private final int minAge;
    private final int maxAge;

    AgeGroup(String key, int minAge, int maxAge) {
        this.key = key;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public String getKey() {
        return key;
    }

    /** Bucket for an age, or null if the age is negative/implausible. */
    public static AgeGroup forAge(int age) {
        if (age < 0) {
            return null;
        }
        for (AgeGroup g : values()) {
            if (age >= g.minAge && age <= g.maxAge) {
                return g;
            }
        }
        return null;
    }

    public static AgeGroup fromBirthYear(int birthYear, int referenceYear) {
        return forAge(referenceYear - birthYear);
    }

    /** Resolve a stable API key (e.g. "30-34") to a bucket; null/unknown → null. */
    public static AgeGroup fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (AgeGroup g : values()) {
            if (g.key.equals(key)) {
                return g;
            }
        }
        return null;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test -Dtest=AgeGroupTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/entity/Gender.java \
        backend/src/main/java/com/trainingsplan/entity/AgeGroup.java \
        backend/src/main/java/com/trainingsplan/entity/LeaderboardScope.java \
        backend/src/test/java/com/trainingsplan/entity/AgeGroupTest.java
git commit -m "Add Gender, AgeGroup (Ironman 5-year) and LeaderboardScope enums"
```

---

## Task 2: Migration 144 + Entity- und DTO-Felder

**Files:**
- Create: `backend/src/main/resources/db/changelog/changes/144-add-segment-effort-demographics.xml`
- Modify: `backend/src/main/resources/db/changelog/db.changelog-master.xml`
- Modify: `backend/src/main/java/com/trainingsplan/entity/SegmentEffort.java`
- Modify: `backend/src/main/java/com/trainingsplan/dto/SegmentLeaderboardEntryDto.java`

- [ ] **Step 1: Write the migration**

`backend/src/main/resources/db/changelog/changes/144-add-segment-effort-demographics.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="144-1" author="benedikt">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="segment_efforts" columnName="gender"/>
            </not>
        </preConditions>
        <comment>Add optional demographics (gender, birth_year) and attempt counter to segment_efforts</comment>
        <addColumn tableName="segment_efforts">
            <column name="gender" type="VARCHAR(10)"/>
            <column name="birth_year" type="INT"/>
            <column name="attempt_count" type="INT" defaultValueNumeric="1">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Register the migration in the master changelog**

In `backend/src/main/resources/db/changelog/db.changelog-master.xml`, find the last include (`143-add-segment-effort-dedupe-key.xml`) and add directly after it:
```xml
    <include file="db/changelog/changes/144-add-segment-effort-demographics.xml"/>
```

- [ ] **Step 3: Add entity fields**

In `backend/src/main/java/com/trainingsplan/entity/SegmentEffort.java`, add the imports/fields. Add after the `category` field block (around line 34):
```java
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 1;
```

And add the accessors next to the other getters/setters (e.g. after `setCategory`):
```java
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
```
(`Gender` is in the same package `com.trainingsplan.entity`, so no import needed.)

- [ ] **Step 4: Extend the leaderboard DTO**

Replace the body of `backend/src/main/java/com/trainingsplan/dto/SegmentLeaderboardEntryDto.java`:
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
        boolean reference,
        String gender,
        String ageGroup,
        int attemptCount
) {}
```

- [ ] **Step 5: Compile (does not pass yet — known callers break)**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test-compile`
Expected: FAIL — `SegmentChallengeService.buildLeaderboard` still calls the old 11-arg DTO constructor. This is fixed in Task 3. Proceed without committing yet.

> Note: Tasks 2 and 3 form one compiling unit. Commit happens at the end of Task 3.

---

## Task 3: Leaderboard-Filterung (scope + ageGroup), AK-Mapping, Versuche-Sortierung

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java`
- Test: `backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceLeaderboardTest.java`

- [ ] **Step 1: Write the failing tests**

Replace `backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceLeaderboardTest.java` with:
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
        SegmentEffort young = demo("Young", 300, Gender.MALE, 2000, 1); // age 26 → 25-29
        SegmentEffort mid = demo("Mid", 305, Gender.MALE, 1986, 1);     // age 40 → 40-44
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
        SegmentEffort c = demo("C", 290, null, null, 5); // same attempts as B, faster than... no, slower than B

        List<SegmentLeaderboardEntryDto> board =
                SegmentChallengeService.buildAttemptsLeaderboard(List.of(a, b, c), REF_YEAR);

        assertEquals("B", board.get(0).displayName()); // 5 attempts, 280 s
        assertEquals(1, board.get(0).rank());
        assertEquals("C", board.get(1).displayName()); // 5 attempts, 290 s (tie-break slower)
        assertEquals(1, board.get(1).rank());          // same attempt count → same rank
        assertEquals("A", board.get(2).displayName()); // 2 attempts
        assertEquals(3, board.get(2).rank());
        assertEquals(5, board.get(0).attemptCount());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test -Dtest=SegmentChallengeServiceLeaderboardTest`
Expected: FAIL — `buildLeaderboard(List, int)`, `filterForScope`, `buildAttemptsLeaderboard` do not exist.

- [ ] **Step 3: Rewrite the leaderboard helpers in the service**

In `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java`, replace the existing `buildLeaderboard(...)` method (the `public static List<SegmentLeaderboardEntryDto> buildLeaderboard(List<SegmentEffort> efforts) {...}` block) with the following set of helpers:
```java
    // ---- pure leaderboard helpers -----------------------------------------

    /** Stored gender, or for reference efforts derived from the PRO_MEN/PRO_WOMEN category. */
    public static Gender effectiveGender(SegmentEffort e) {
        if (e.getGender() != null) {
            return e.getGender();
        }
        if (e.getCategory() == EffortCategory.PRO_MEN) {
            return Gender.MALE;
        }
        if (e.getCategory() == EffortCategory.PRO_WOMEN) {
            return Gender.FEMALE;
        }
        return null;
    }

    /** Filter efforts for a ranking scope. MEN/WOMEN use effective gender and may add an age-group filter. */
    public static List<SegmentEffort> filterForScope(List<SegmentEffort> efforts, LeaderboardScope scope,
                                                     String ageGroupKey, int referenceYear) {
        AgeGroup wantAg = AgeGroup.fromKey(ageGroupKey);
        List<SegmentEffort> out = new ArrayList<>();
        for (SegmentEffort e : efforts) {
            switch (scope) {
                case OVERALL -> out.add(e);
                case MOST_ATTEMPTS -> {
                    if (e.getKind() == EffortKind.PUBLIC) {
                        out.add(e);
                    }
                }
                case MEN, WOMEN -> {
                    Gender want = scope == LeaderboardScope.MEN ? Gender.MALE : Gender.FEMALE;
                    boolean ok = effectiveGender(e) == want;
                    if (ok && wantAg != null) {
                        Integer by = e.getBirthYear();
                        ok = by != null && AgeGroup.fromBirthYear(by, referenceYear) == wantAg;
                    }
                    if (ok) {
                        out.add(e);
                    }
                }
            }
        }
        return out;
    }

    /** Time-sorted leaderboard (OVERALL / MEN / WOMEN). */
    public static List<SegmentLeaderboardEntryDto> buildLeaderboard(List<SegmentEffort> efforts, int referenceYear) {
        List<SegmentEffort> sorted = new ArrayList<>(efforts);
        sorted.sort(Comparator.comparingInt(SegmentEffort::getElapsedSeconds));
        Integer leaderTime = sorted.isEmpty() ? null : sorted.get(0).getElapsedSeconds();

        List<SegmentLeaderboardEntryDto> out = new ArrayList<>(sorted.size());
        int position = 0;
        int rank = 0;
        Integer prevElapsed = null;
        for (SegmentEffort e : sorted) {
            position++;
            if (prevElapsed == null || e.getElapsedSeconds() != prevElapsed) {
                rank = position;
                prevElapsed = e.getElapsedSeconds();
            }
            out.add(toDto(e, rank, leaderTime, referenceYear));
        }
        return out;
    }

    /** Attempts-sorted leaderboard (MOST_ATTEMPTS): attempt_count desc, tie-break elapsed asc. */
    public static List<SegmentLeaderboardEntryDto> buildAttemptsLeaderboard(List<SegmentEffort> efforts, int referenceYear) {
        List<SegmentEffort> sorted = new ArrayList<>(efforts);
        sorted.sort(Comparator.comparingInt(SegmentEffort::getAttemptCount).reversed()
                .thenComparingInt(SegmentEffort::getElapsedSeconds));
        Integer leaderTime = sorted.stream()
                .map(SegmentEffort::getElapsedSeconds).min(Integer::compareTo).orElse(null);

        List<SegmentLeaderboardEntryDto> out = new ArrayList<>(sorted.size());
        int position = 0;
        int rank = 0;
        Integer prevAttempts = null;
        for (SegmentEffort e : sorted) {
            position++;
            if (prevAttempts == null || e.getAttemptCount() != prevAttempts) {
                rank = position;
                prevAttempts = e.getAttemptCount();
            }
            out.add(toDto(e, rank, leaderTime, referenceYear));
        }
        return out;
    }

    private static SegmentLeaderboardEntryDto toDto(SegmentEffort e, int rank, Integer leaderTime, int referenceYear) {
        int gap = leaderTime == null ? 0 : e.getElapsedSeconds() - leaderTime;
        Gender g = effectiveGender(e);
        AgeGroup ag = (g != null && e.getBirthYear() != null)
                ? AgeGroup.fromBirthYear(e.getBirthYear(), referenceYear) : null;
        return new SegmentLeaderboardEntryDto(
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
                e.getKind() == EffortKind.REFERENCE,
                g != null ? g.name() : null,
                ag != null ? ag.getKey() : null,
                e.getAttemptCount());
    }

    public static String formatElapsed(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return m + ":" + (s < 10 ? "0" + s : Integer.toString(s));
    }
```

- [ ] **Step 4: Update the `getLeaderboard` service method to use scope + ageGroup**

In the same file, replace the existing `getLeaderboard` method:
```java
    @Transactional(readOnly = true)
    public List<SegmentLeaderboardEntryDto> getLeaderboard(String slug, ActivityType type) {
        SegmentChallenge c = requireActiveChallenge(slug);
        List<SegmentEffort> efforts = effortRepository
                .findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                        c.getId(), type, EffortStatus.VALID);
        return buildLeaderboard(efforts);
    }
```
with:
```java
    @Transactional(readOnly = true)
    public List<SegmentLeaderboardEntryDto> getLeaderboard(String slug, ActivityType type,
                                                           LeaderboardScope scope, String ageGroupKey) {
        SegmentChallenge c = requireActiveChallenge(slug);
        List<SegmentEffort> efforts = effortRepository
                .findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
                        c.getId(), type, EffortStatus.VALID);
        int referenceYear = c.getEventDate() != null
                ? c.getEventDate().getYear() : java.time.LocalDate.now().getYear();
        List<SegmentEffort> filtered = filterForScope(efforts, scope, ageGroupKey, referenceYear);
        return scope == LeaderboardScope.MOST_ATTEMPTS
                ? buildAttemptsLeaderboard(filtered, referenceYear)
                : buildLeaderboard(filtered, referenceYear);
    }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test -Dtest=AgeGroupTest,SegmentChallengeServiceLeaderboardTest`
Expected: PASS. (If `test-compile` still fails on `PublicSegmentChallengeController` calling the old 2-arg `getLeaderboard`, that is fixed in Task 5 — but the service + leaderboard tests compile and pass on their own. If the controller breaks compilation of the whole module, temporarily proceed to Task 5 before running the full suite; the per-test compile of these classes still succeeds because Maven compiles the module — see note.)

> If `mvn test` cannot compile the module because the controller still calls `getLeaderboard(slug, type)`, do Step 6 of **Task 5** (controller update) before running the full suite. To keep this task self-contained, you may instead run only the two unit test classes above which the Maven Surefire `-Dtest` filter still compiles within the module. Recommended: implement Task 5 immediately after, then run the full backend suite once.

- [ ] **Step 6: Commit (Tasks 2 + 3 together)**

```bash
git add backend/src/main/resources/db/changelog/changes/144-add-segment-effort-demographics.xml \
        backend/src/main/resources/db/changelog/db.changelog-master.xml \
        backend/src/main/java/com/trainingsplan/entity/SegmentEffort.java \
        backend/src/main/java/com/trainingsplan/dto/SegmentLeaderboardEntryDto.java \
        backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java \
        backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceLeaderboardTest.java
git commit -m "Add demographics columns + gender/age-group/attempts leaderboard filtering"
```

---

## Task 4: Upload mit `gender`/`birthYear` + Versuchszähler

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java`
- Test: `backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceDedupeTest.java`

- [ ] **Step 1: Update existing dedupe tests + add attempt/birth-year tests**

In `backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceDedupeTest.java`:

(a) Every call to `service.submitPublicEffort(...)` currently passes 6 args. Update the signature by inserting `null, null` (gender, birthYear) before `fileBytes`. For example replace:
```java
        SegmentEffortResultDto result = service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "Alice",
                "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");
```
with:
```java
        SegmentEffortResultDto result = service.submitPublicEffort(
                "heartbreak-hill-2026", ActivityType.RUN, "Alice",
                null, null, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");
```
Apply the same `null, null` insertion to the calls in `slowerReupload_*`, `fasterReupload_*`, `differentDisplayName_*` and `result_carriesSpeedAndPaceFromSavedEffort` (the two-`Alice`/`Bob` calls too).

(b) The slower-reupload test must change: a slower re-upload now **saves** (to bump the attempt counter) but keeps the best time. Replace the whole `slowerReupload_sameNameVariant_doesNotSave_returnsExistingBest` test with:
```java
    @Test
    void slowerReupload_sameNameVariant_bumpsAttemptCount_keepsBestTime() throws Exception {
        // existing best: 300 s, 1 attempt; new upload: 360 s (slower) → save with attempt 2, time stays 300
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
```

(c) Add two new tests at the end of the class (before the closing brace):
```java
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
        when(effortRepository.findFirstByChallengeIdAndKindAndStatusAndDedupeKey(
                anyLong(), any(), any(), anyString())).thenReturn(Optional.empty());

        // birthYear 3000 is impossible → 422 before any match attempt
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.submitPublicEffort("heartbreak-hill-2026", ActivityType.RUN, "X",
                        Gender.MALE, 3000, "<gpx/>".getBytes(), "run.gpx", "1.2.3.4"));
        assertEquals("invalid_birth_year", ex.getMessage());
    }
```
(`Gender` import: add `import com.trainingsplan.entity.*;` already present via `entity.*`.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test -Dtest=SegmentChallengeServiceDedupeTest`
Expected: FAIL — `submitPublicEffort` does not yet accept `Gender, Integer`.

- [ ] **Step 3: Update `submitPublicEffort` in the service**

In `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java`, replace the whole `submitPublicEffort` method with:
```java
    @Transactional
    public SegmentEffortResultDto submitPublicEffort(String slug, ActivityType type, String displayName,
                                                     Gender gender, Integer birthYear,
                                                     byte[] fileBytes, String originalFilename, String clientIp) {
        SegmentChallenge c = requireActiveChallenge(slug);

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("display_name_required");
        }
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".gpx")) {
            throw new IllegalArgumentException("only_gpx_supported");
        }
        if (birthYear != null) {
            int refYear = c.getEventDate() != null
                    ? c.getEventDate().getYear() : java.time.LocalDate.now().getYear();
            if (birthYear < refYear - 100 || birthYear > refYear - 5) {
                throw new IllegalArgumentException("invalid_birth_year");
            }
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

        String identity = SegmentEffortDedup.identityKey(c.getId(), type.name(), displayName);
        Optional<SegmentEffort> existingOpt = effortRepository
                .findFirstByChallengeIdAndKindAndStatusAndDedupeKey(c.getId(), EffortKind.PUBLIC, EffortStatus.VALID, identity);
        if (existingOpt.isPresent()) {
            SegmentEffort ex = existingOpt.get();
            ex.setAttemptCount(ex.getAttemptCount() + 1);   // every valid upload counts as an attempt
            if (gender != null) {
                ex.setGender(gender);
            }
            if (birthYear != null) {
                ex.setBirthYear(birthYear);
            }
            if (match.getElapsedSeconds() < ex.getElapsedSeconds()) {
                // new personal best — update time/metrics/track in place
                ex.setElapsedSeconds(match.getElapsedSeconds());
                ex.setAvgSpeedKmh(match.getAvgSpeedKmh());
                ex.setAvgPaceSecondsPerKm(match.getAvgPaceSecondsPerKm());
                ex.setTrackJson(serializeTrack(match.getCroppedTrack()));
                ex.setSourceFormat("GPX");
                ex.setDisplayName(displayName.trim());
                ex.setIpHash(ipHash);
                ex.setCreatedAt(LocalDateTime.now());
            }
            effortRepository.save(ex);
            return buildResult(c, type, ex);
        }

        SegmentEffort e = new SegmentEffort();
        e.setChallenge(c);
        e.setActivityType(type);
        e.setKind(EffortKind.PUBLIC);
        e.setCategory(EffortCategory.COMMUNITY);
        e.setDisplayName(displayName.trim());
        e.setGender(gender);
        e.setBirthYear(birthYear);
        e.setAttemptCount(1);
        e.setElapsedSeconds(match.getElapsedSeconds());
        e.setAvgSpeedKmh(match.getAvgSpeedKmh());
        e.setAvgPaceSecondsPerKm(match.getAvgPaceSecondsPerKm());
        e.setTrackJson(serializeTrack(match.getCroppedTrack()));
        e.setSourceFormat("GPX");
        e.setStatus(EffortStatus.VALID);
        e.setEditToken(UUID.randomUUID().toString());
        e.setIpHash(ipHash);
        e.setDedupeKey(identity);
        e.setCreatedAt(LocalDateTime.now());
        effortRepository.save(e);

        return buildResult(c, type, e);
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test -Dtest=SegmentChallengeServiceDedupeTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java \
        backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceDedupeTest.java
git commit -m "Count upload attempts and capture optional gender/birth year on submit"
```

---

## Task 5: Controller-Endpoints (Public leaderboard/upload + Admin reference)

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/controller/PublicSegmentChallengeController.java`
- Modify: `backend/src/main/java/com/trainingsplan/controller/AdminSegmentChallengeController.java`
- Modify: `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java` (addReferenceEffort signature)
- Test: `backend/src/test/java/com/trainingsplan/controller/PublicSegmentChallengeControllerTest.java`

- [ ] **Step 1: Update the controller test**

In `backend/src/test/java/com/trainingsplan/controller/PublicSegmentChallengeControllerTest.java`:

(a) The `submitPublicEffort` mock now has 8 args. Replace the `when(...)` in `submitEffort_returnsResultJson`:
```java
        when(service.submitPublicEffort(eq("heartbreak-hill-2026"), eq(ActivityType.RIDE),
                eq("Lukas"), any(), eq("ride.gpx"), any()))
                .thenReturn(new SegmentEffortResultDto(7L, "tok", 47, 312, 298, "4:58", 46, 85.0, null, null, "VALID"));
```
with:
```java
        when(service.submitPublicEffort(eq("heartbreak-hill-2026"), eq(ActivityType.RIDE),
                eq("Lukas"), any(), any(), any(), eq("ride.gpx"), any()))
                .thenReturn(new SegmentEffortResultDto(7L, "tok", 47, 312, 298, "4:58", 46, 85.0, null, null, "VALID"));
```
and in `submitEffort_rejectedMatch_returns422` replace:
```java
        when(service.submitPublicEffort(any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("start_gate_not_reached"));
```
with:
```java
        when(service.submitPublicEffort(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("start_gate_not_reached"));
```

(b) Add a test for the leaderboard scope params. Add these imports at the top:
```java
import com.trainingsplan.entity.LeaderboardScope;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
```
And add this test method:
```java
    @Test
    void leaderboard_passesScopeAndAgeGroupToService() throws Exception {
        when(service.getLeaderboard(eq("heartbreak-hill-2026"), eq(ActivityType.RUN),
                eq(LeaderboardScope.MEN), eq("40-44")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/public/challenges/heartbreak-hill-2026/leaderboard")
                        .param("type", "RUN")
                        .param("scope", "MEN")
                        .param("ageGroup", "40-44"))
                .andExpect(status().isOk());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test -Dtest=PublicSegmentChallengeControllerTest`
Expected: FAIL — controller methods don't accept the new params / `getLeaderboard` arity.

- [ ] **Step 3: Update `PublicSegmentChallengeController`**

Replace the `getLeaderboard` and `submitEffort` methods in `backend/src/main/java/com/trainingsplan/controller/PublicSegmentChallengeController.java`. Add imports:
```java
import com.trainingsplan.entity.Gender;
import com.trainingsplan.entity.LeaderboardScope;
```
Replace `getLeaderboard`:
```java
    @GetMapping("/{slug}/leaderboard")
    public ResponseEntity<?> getLeaderboard(@PathVariable String slug,
                                            @RequestParam(defaultValue = "RIDE") ActivityType type,
                                            @RequestParam(defaultValue = "OVERALL") LeaderboardScope scope,
                                            @RequestParam(value = "ageGroup", required = false) String ageGroup) {
        try {
            List<SegmentLeaderboardEntryDto> board = service.getLeaderboard(slug, type, scope, ageGroup);
            return ResponseEntity.ok(board);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
```
Replace `submitEffort`:
```java
    @PostMapping("/{slug}/efforts")
    public ResponseEntity<?> submitEffort(@PathVariable String slug,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam("displayName") String displayName,
                                          @RequestParam("type") ActivityType type,
                                          @RequestParam(value = "gender", required = false) Gender gender,
                                          @RequestParam(value = "birthYear", required = false) Integer birthYear,
                                          HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "empty_file"));
        }
        try {
            SegmentEffortResultDto result = service.submitPublicEffort(
                    slug, type, displayName, gender, birthYear, file.getBytes(), file.getOriginalFilename(),
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
```

- [ ] **Step 4: Update `addReferenceEffort` (service + admin controller)**

In `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java`, change the `addReferenceEffort` signature and persist the new fields. Replace the method header and the `new SegmentEffort()` block:
```java
    @Transactional
    public Long addReferenceEffort(String slug, ActivityType type, String displayName,
                                   EffortCategory category, Gender gender, Integer birthYear,
                                   byte[] fileBytes, String originalFilename,
                                   Integer knownTimeSecondsOverride) {
```
and inside, after `e.setDisplayName(displayName.trim());` add:
```java
        e.setGender(gender);        // null → effectiveGender() derives MALE/FEMALE from PRO_* category
        e.setBirthYear(birthYear);  // null → no age group
        e.setAttemptCount(1);
```

In `backend/src/main/java/com/trainingsplan/controller/AdminSegmentChallengeController.java`, add import:
```java
import com.trainingsplan.entity.Gender;
```
and replace the `addReferenceEffort` mapping to accept and forward the new params:
```java
    @PostMapping("/{slug}/reference-efforts")
    public ResponseEntity<?> addReferenceEffort(@PathVariable String slug,
                                                @RequestParam("file") MultipartFile file,
                                                @RequestParam("displayName") String displayName,
                                                @RequestParam("type") ActivityType type,
                                                @RequestParam("category") EffortCategory category,
                                                @RequestParam(value = "gender", required = false) Gender gender,
                                                @RequestParam(value = "birthYear", required = false) Integer birthYear,
                                                @RequestParam(value = "knownTimeSeconds", required = false) Integer knownTimeSeconds) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "empty_file"));
        }
        try {
            Long id = service.addReferenceEffort(slug, type, displayName, category, gender, birthYear,
                    file.getBytes(), file.getOriginalFilename(), knownTimeSeconds);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("effortId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("reason", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("reason", "io_error"));
        }
    }
```

- [ ] **Step 5: Run the full backend suite**

Run: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test`
Expected: PASS — all segment tests green, no compilation errors.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/controller/PublicSegmentChallengeController.java \
        backend/src/main/java/com/trainingsplan/controller/AdminSegmentChallengeController.java \
        backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java \
        backend/src/test/java/com/trainingsplan/controller/PublicSegmentChallengeControllerTest.java
git commit -m "Expose ranking scope/age-group and demographics through the segment controllers"
```

---

## Task 6: Frontend Model + Service

**Files:**
- Modify: `frontend/src/app/models/heartbreak-hill.model.ts`
- Modify: `frontend/src/app/services/heartbreak-hill.service.ts`
- Test: `frontend/src/app/services/heartbreak-hill.service.spec.ts`

- [ ] **Step 1: Write the failing service tests**

In `frontend/src/app/services/heartbreak-hill.service.spec.ts`, replace the `getLeaderboard` test and add a demographics test:

Replace:
```js
  it('getLeaderboard passes the activity type as a query param', () => {
    service.getLeaderboard('RUN').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/leaderboard') && r.params.get('type') === 'RUN');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
```
with:
```js
  it('getLeaderboard passes type and scope, defaulting scope to OVERALL', () => {
    service.getLeaderboard('RUN').subscribe();
    const req = httpMock.expectOne(r =>
      r.url.endsWith('/leaderboard') && r.params.get('type') === 'RUN' && r.params.get('scope') === 'OVERALL');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getLeaderboard adds ageGroup only when provided', () => {
    service.getLeaderboard('RIDE', 'MEN', '40-44').subscribe();
    const req = httpMock.expectOne(r =>
      r.url.endsWith('/leaderboard') && r.params.get('scope') === 'MEN' && r.params.get('ageGroup') === '40-44');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
```
And add after the existing `submitEffort` test:
```js
  it('submitEffort appends gender and birthYear when present', () => {
    const file = new File(['<gpx/>'], 'ride.gpx', { type: 'application/gpx+xml' });
    service.submitEffort('RIDE', 'Lukas', file, 'MALE', 1990).subscribe();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/efforts'));
    const body = req.request.body as FormData;
    expect(body.get('gender')).toBe('MALE');
    expect(body.get('birthYear')).toBe('1990');
    req.flush({ effortId: 1, rank: 1 });
  });

  it('submitEffort omits gender and birthYear when not provided', () => {
    const file = new File(['<gpx/>'], 'ride.gpx', { type: 'application/gpx+xml' });
    service.submitEffort('RIDE', 'Lukas', file).subscribe();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/efforts'));
    const body = req.request.body as FormData;
    expect(body.get('gender')).toBeNull();
    expect(body.get('birthYear')).toBeNull();
    req.flush({ effortId: 1, rank: 1 });
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npm test -- --run heartbreak-hill.service`
Expected: FAIL — `getLeaderboard`/`submitEffort` don't accept the new args / don't set the params.

- [ ] **Step 3: Extend the model**

In `frontend/src/app/models/heartbreak-hill.model.ts`, add after the `ActivityType` line:
```ts
export type Gender = 'MALE' | 'FEMALE' | 'DIVERS';
export type LeaderboardScope = 'OVERALL' | 'MEN' | 'WOMEN' | 'MOST_ATTEMPTS';

/** Ironman 5-year age-group keys (must match backend AgeGroup.getKey()). */
export const AGE_GROUPS: string[] = [
  'U18', '18-24', '25-29', '30-34', '35-39', '40-44',
  '45-49', '50-54', '55-59', '60-64', '65-69', '70+'
];
```
And extend `LeaderboardEntry` with three fields (add before the closing brace of the interface):
```ts
  gender: string | null;
  ageGroup: string | null;
  attemptCount: number;
```

- [ ] **Step 4: Update the service**

In `frontend/src/app/services/heartbreak-hill.service.ts`, update the imports line to include the new types:
```ts
import {
  ActivityType, SegmentChallenge, LeaderboardEntry, EffortResult, EffortTrack, Gender, LeaderboardScope
} from '../models/heartbreak-hill.model';
```
Replace `getLeaderboard`:
```ts
  getLeaderboard(type: ActivityType, scope: LeaderboardScope = 'OVERALL',
                 ageGroup?: string | null): Observable<LeaderboardEntry[]> {
    let params = new HttpParams().set('type', type).set('scope', scope);
    if (ageGroup) {
      params = params.set('ageGroup', ageGroup);
    }
    return this.http.get<LeaderboardEntry[]>(`${BASE}/leaderboard`, { params });
  }
```
Replace `submitEffort`:
```ts
  submitEffort(type: ActivityType, displayName: string, file: File,
               gender?: Gender | null, birthYear?: number | null): Observable<EffortResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('displayName', displayName);
    form.append('type', type);
    if (gender) {
      form.append('gender', gender);
    }
    if (birthYear != null) {
      form.append('birthYear', String(birthYear));
    }
    return this.http.post<EffortResult>(`${BASE}/efforts`, form);
  }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd frontend && npm test -- --run heartbreak-hill.service`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/models/heartbreak-hill.model.ts \
        frontend/src/app/services/heartbreak-hill.service.ts \
        frontend/src/app/services/heartbreak-hill.service.spec.ts
git commit -m "Add ranking scope, age group and demographics to the frontend model/service"
```

---

## Task 7: Upload-Formular — Geschlecht + Geburtsjahr

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts`
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html`
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss`
- Modify: `frontend/src/assets/i18n/de.json`, `frontend/src/assets/i18n/en.json`

- [ ] **Step 1: Add component state + submit wiring**

In `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts`:

Update the model import to add `Gender`:
```ts
import {
  ActivityType, SegmentChallenge, LeaderboardEntry, EffortResult, Gender
} from '../../models/heartbreak-hill.model';
```
Add these signals next to the other upload-state signals (after `displayName = signal('');`):
```ts
  gender = signal<Gender | ''>('');
  birthYear = signal<number | null>(null);
```
Add a computed for the birth-year dropdown options (after the `profile` computed, before `use3d`):
```ts
  /** Selectable birth years: referenceYear-5 down to referenceYear-100 (matches backend validation). */
  birthYearOptions = computed<number[]>(() => {
    const c = this.challenge();
    const ref = c?.eventDate ? new Date(c.eventDate).getFullYear() : new Date().getFullYear();
    const years: number[] = [];
    for (let y = ref - 5; y >= ref - 100; y--) {
      years.push(y);
    }
    return years;
  });
```
In `submit()`, pass the demographics through. Replace the `this.service.submitEffort(...)` call:
```ts
    this.service.submitEffort(this.activeTab(), this.displayName().trim(), file).subscribe({
```
with:
```ts
    const g = this.gender() || null;
    this.service.submitEffort(this.activeTab(), this.displayName().trim(), file, g, this.birthYear()).subscribe({
```

- [ ] **Step 2: Add the form fields to the template**

In `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html`, after the display-name input (the `<input class="field" [ngModel]="displayName()" ...>` block, around line 111-112) and before the `<div class="seg">`, insert:
```html
          <div class="demo-row">
            <select class="field" [ngModel]="gender()" (ngModelChange)="gender.set($event)">
              <option value="">{{ 'HEARTBREAK_HILL.GENDER_NONE' | translate }}</option>
              <option value="MALE">{{ 'HEARTBREAK_HILL.GENDER_MALE' | translate }}</option>
              <option value="FEMALE">{{ 'HEARTBREAK_HILL.GENDER_FEMALE' | translate }}</option>
              <option value="DIVERS">{{ 'HEARTBREAK_HILL.GENDER_DIVERS' | translate }}</option>
            </select>
            <select class="field" [ngModel]="birthYear()" (ngModelChange)="birthYear.set($event)">
              <option [ngValue]="null">{{ 'HEARTBREAK_HILL.BIRTHYEAR_NONE' | translate }}</option>
              @for (y of birthYearOptions(); track y) {
                <option [ngValue]="y">{{ y }}</option>
              }
            </select>
          </div>
          <p class="demo-hint">{{ 'HEARTBREAK_HILL.DEMO_HINT' | translate }}</p>
```

- [ ] **Step 3: Add styles**

Append to `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss`:
```scss
/* DEMOGRAPHICS (upload form) */
.demo-row { display: flex; gap: 10px; margin-top: 14px; }
.demo-row .field { margin-top: 0; flex: 1; }
.demo-hint { margin-top: 8px; font-size: 12px; color: var(--text-muted); }
```

- [ ] **Step 4: Add i18n keys (de + en)**

In `frontend/src/assets/i18n/de.json`, inside the `HEARTBREAK_HILL` object (e.g. after `"NAME_PLACEHOLDER": ...`), add:
```json
    "GENDER_NONE": "Geschlecht (optional)",
    "GENDER_MALE": "Männlich",
    "GENDER_FEMALE": "Weiblich",
    "GENDER_DIVERS": "Divers",
    "BIRTHYEAR_NONE": "Jahrgang (optional)",
    "DEMO_HINT": "Für die Geschlechts- und Altersklassenwertung — optional.",
```
In `frontend/src/assets/i18n/en.json`, at the analogous place inside `HEARTBREAK_HILL`, add:
```json
    "GENDER_NONE": "Gender (optional)",
    "GENDER_MALE": "Male",
    "GENDER_FEMALE": "Female",
    "GENDER_DIVERS": "Diverse",
    "BIRTHYEAR_NONE": "Birth year (optional)",
    "DEMO_HINT": "For the gender and age-group rankings — optional.",
```

- [ ] **Step 5: Verify build**

Run: `cd frontend && npm run build`
Expected: SUCCESS — no template/type errors.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts \
        frontend/src/app/components/heartbreak-hill/heartbreak-hill.html \
        frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss \
        frontend/src/assets/i18n/de.json frontend/src/assets/i18n/en.json
git commit -m "Add optional gender/birth-year fields to the Heartbreak Hill upload form"
```

---

## Task 8: Filter-Leiste, AK-Dropdown, Versuche-Badge & Meiste-Versuche-Ansicht

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts`
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html`
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss`
- Modify: `frontend/src/assets/i18n/de.json`, `frontend/src/assets/i18n/en.json`

- [ ] **Step 1: Add ranking-scope state to the component**

In `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts`:

Update the model import to add `LeaderboardScope` and `AGE_GROUPS`:
```ts
import {
  ActivityType, SegmentChallenge, LeaderboardEntry, EffortResult, Gender, LeaderboardScope, AGE_GROUPS
} from '../../models/heartbreak-hill.model';
```
Add scope signals + helpers next to the leaderboard signals (after `leaderboardLoading = signal(false);`):
```ts
  readonly ageGroups = AGE_GROUPS;
  rankingScope = signal<LeaderboardScope>('OVERALL');
  selectedAgeGroup = signal<string>(''); // '' = all age groups

  showAgeGroupFilter = computed(() =>
    this.rankingScope() === 'MEN' || this.rankingScope() === 'WOMEN');
  isMostAttempts = computed(() => this.rankingScope() === 'MOST_ATTEMPTS');
```
Add scope-change handlers (after the existing `selectTab` method):
```ts
  selectRankingScope(scope: LeaderboardScope): void {
    if (this.rankingScope() === scope) {
      return;
    }
    this.rankingScope.set(scope);
    if (scope !== 'MEN' && scope !== 'WOMEN') {
      this.selectedAgeGroup.set('');
    }
    this.loadLeaderboard();
  }

  onAgeGroupChange(value: string): void {
    this.selectedAgeGroup.set(value);
    this.loadLeaderboard();
  }
```
Replace `loadLeaderboard()` to pass scope + ageGroup:
```ts
  private loadLeaderboard(): void {
    this.leaderboardLoading.set(true);
    const scope = this.rankingScope();
    const ageGroup = this.showAgeGroupFilter() ? (this.selectedAgeGroup() || null) : null;
    this.service.getLeaderboard(this.activeTab(), scope, ageGroup).subscribe({
      next: entries => {
        this.leaderboard.set(entries);
        this.leaderboardLoading.set(false);
      },
      error: () => {
        this.leaderboard.set([]);
        this.leaderboardLoading.set(false);
      }
    });
  }
```

- [ ] **Step 2: Add the filter bar + age-group dropdown + attempt rendering to the template**

In `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html`, replace the leaderboard block. Find the `<div class="lb-tabs"> … </div>` (RIDE/RUN tabs, lines ~65-70) and immediately after that closing `</div>` insert the scope bar:
```html
        <div class="lb-scope">
          <button [class.on]="rankingScope() === 'OVERALL'" (click)="selectRankingScope('OVERALL')">{{ 'HEARTBREAK_HILL.SCOPE_OVERALL' | translate }}</button>
          <button [class.on]="rankingScope() === 'MEN'" (click)="selectRankingScope('MEN')">{{ 'HEARTBREAK_HILL.SCOPE_MEN' | translate }}</button>
          <button [class.on]="rankingScope() === 'WOMEN'" (click)="selectRankingScope('WOMEN')">{{ 'HEARTBREAK_HILL.SCOPE_WOMEN' | translate }}</button>
          <button [class.on]="rankingScope() === 'MOST_ATTEMPTS'" (click)="selectRankingScope('MOST_ATTEMPTS')">{{ 'HEARTBREAK_HILL.SCOPE_ATTEMPTS' | translate }}</button>
        </div>
        @if (showAgeGroupFilter()) {
          <select class="lb-ak" [ngModel]="selectedAgeGroup()" (ngModelChange)="onAgeGroupChange($event)">
            <option value="">{{ 'HEARTBREAK_HILL.AK_ALL' | translate }}</option>
            @for (ag of ageGroups; track ag) {
              <option [value]="ag">{{ ag }}</option>
            }
          </select>
        }
```
Then replace the empty-state paragraph and the `@for` row block. Replace:
```html
        } @else if (leaderboard().length === 0) {
          <div class="lb-empty">
            <h3>{{ 'HEARTBREAK_HILL.EMPTY_TITLE' | translate }}</h3>
            <p>{{ 'HEARTBREAK_HILL.EMPTY_BODY' | translate }}</p>
          </div>
        } @else {
          @for (entry of leaderboard(); track entry.effortId) {
            <div class="row" [class.ref]="entry.reference" [class.you]="isMyEffort(entry)">
              <div class="rk">{{ entry.rank }}</div>
              <div class="nm">
                {{ entry.displayName }}
                @if (entry.reference) {
                  <span class="badge pro">{{ referenceBadgeKey(entry) | translate }}</span>
                }
              </div>
              <div class="tm">{{ entry.elapsedFormatted }}</div>
              <div class="gap">{{ formatGap(entry.gapToLeaderSeconds) }}</div>
            </div>
          }
        }
```
with:
```html
        } @else if (leaderboard().length === 0) {
          <div class="lb-empty">
            <h3>{{ 'HEARTBREAK_HILL.EMPTY_TITLE' | translate }}</h3>
            <p>{{ (rankingScope() === 'OVERALL' ? 'HEARTBREAK_HILL.EMPTY_BODY' : 'HEARTBREAK_HILL.EMPTY_FILTERED_BODY') | translate }}</p>
          </div>
        } @else {
          @for (entry of leaderboard(); track entry.effortId) {
            <div class="row" [class.ref]="entry.reference" [class.you]="isMyEffort(entry)">
              <div class="rk">{{ entry.rank }}</div>
              <div class="nm">
                {{ entry.displayName }}
                @if (entry.reference) {
                  <span class="badge pro">{{ referenceBadgeKey(entry) | translate }}</span>
                }
                @if (entry.attemptCount > 1) {
                  <span class="badge tries">{{ 'HEARTBREAK_HILL.ATTEMPTS_BADGE' | translate:{ count: entry.attemptCount } }}</span>
                }
              </div>
              @if (isMostAttempts()) {
                <div class="tm">{{ 'HEARTBREAK_HILL.ATTEMPTS_COUNT' | translate:{ count: entry.attemptCount } }}</div>
                <div class="gap">{{ entry.elapsedFormatted }}</div>
              } @else {
                <div class="tm">{{ entry.elapsedFormatted }}</div>
                <div class="gap">{{ formatGap(entry.gapToLeaderSeconds) }}</div>
              }
            </div>
          }
        }
```

- [ ] **Step 3: Add styles**

Append to `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss`:
```scss
/* RANKING SCOPE FILTER */
.lb-scope { display: flex; flex-wrap: wrap; gap: 8px; padding: 14px 18px 4px; }
.lb-scope button { background: var(--bg-card); border: 1px solid var(--border); color: var(--text-muted); padding: 8px 14px; border-radius: 999px; font-weight: 700; font-size: 13px; cursor: pointer; font-family: inherit; }
.lb-scope button.on { background: var(--pp); color: #07120a; border-color: var(--pp); }
.lb-ak { margin: 10px 18px 0; background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; padding: 10px 12px; color: var(--text); font-family: inherit; }
.badge.tries { background: var(--pp-container); color: var(--pp); }
```

- [ ] **Step 4: Add i18n keys (de + en)**

In `frontend/src/assets/i18n/de.json`, inside `HEARTBREAK_HILL` (e.g. after `"EMPTY_BODY": ...`), add:
```json
    "EMPTY_FILTERED_BODY": "Noch keine Einträge in dieser Wertung — lade dein GPX mit Jahrgang und Geschlecht hoch.",
    "SCOPE_OVERALL": "Gesamt",
    "SCOPE_MEN": "Männer",
    "SCOPE_WOMEN": "Frauen",
    "SCOPE_ATTEMPTS": "Meiste Versuche",
    "AK_ALL": "Alle Altersklassen",
    "ATTEMPTS_BADGE": "×{{count}}",
    "ATTEMPTS_COUNT": "{{count}} Versuche",
```
In `frontend/src/assets/i18n/en.json`, at the analogous place, add:
```json
    "EMPTY_FILTERED_BODY": "No entries in this ranking yet — upload your GPX with birth year and gender.",
    "SCOPE_OVERALL": "Overall",
    "SCOPE_MEN": "Men",
    "SCOPE_WOMEN": "Women",
    "SCOPE_ATTEMPTS": "Most attempts",
    "AK_ALL": "All age groups",
    "ATTEMPTS_BADGE": "×{{count}}",
    "ATTEMPTS_COUNT": "{{count}} attempts",
```

- [ ] **Step 5: Verify build**

Run: `cd frontend && npm run build`
Expected: SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts \
        frontend/src/app/components/heartbreak-hill/heartbreak-hill.html \
        frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss \
        frontend/src/assets/i18n/de.json frontend/src/assets/i18n/en.json
git commit -m "Add ranking filter bar, age-group dropdown and attempt badge/leaderboard"
```

---

## Task 9: Version-Bump, Changelog & finale Verifikation

**Files:**
- Modify: `backend/pom.xml`, `frontend/package.json` (via script)
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Run the full test suites once more**

Run backend: `JAVA_HOME="<JDK21>" mvn -q -f backend/pom.xml test`
Run frontend: `cd frontend && npm test -- --run`
Expected: both green. (Pre-existing failure `app.spec.ts` / `window.matchMedia` may remain — note it, do not fix here.)

- [ ] **Step 2: Bump the version (minor — new feature/endpoints/columns)**

Run: `./version-bump.sh minor`
Then confirm `backend/pom.xml` `<version>` and `frontend/package.json` `"version"` moved to the next minor.

- [ ] **Step 3: Update the changelog**

In `CHANGELOG.md`, under `[Unreleased]` → `Added`, add:
```markdown
- Heartbreak Hill: gender-split and age-group rankings (Ironman 5-year classes, gender-separated) plus a "most attempts" leaderboard and an attempt-count badge. Gender and birth year are optional at upload; the age group is computed live from the birth year.
```

- [ ] **Step 4: Commit**

```bash
git add backend/pom.xml frontend/package.json CHANGELOG.md
git commit -m "Bump minor version and changelog for Heartbreak Hill ranking extensions"
```

- [ ] **Step 5 (optional): Manual smoke test**

With backend running (`JAVA_HOME="<JDK21>" mvn -f backend/pom.xml spring-boot:run`) and a seeded challenge, exercise:
- `GET /api/public/challenges/heartbreak-hill-2026/leaderboard?type=RIDE&scope=MEN&ageGroup=40-44`
- `GET …/leaderboard?type=RIDE&scope=MOST_ATTEMPTS`
- An upload with `gender=FEMALE&birthYear=1986`, then a second (slower) upload with the same name → attempt count 2, time unchanged, appears in WOMEN + the matching age group.

---

## Self-Review

**1. Spec coverage:**
- Gesamtranking unverändert → `OVERALL` scope, `buildLeaderboard` (Task 3). ✓
- M/W-Wertung → `filterForScope` MEN/WOMEN + `effectiveGender` (Task 3). ✓
- Altersklassen (Ironman 5-Jahres, geschlechtergetrennt, live) → `AgeGroup` (Task 1) + AK-Filter in `filterForScope` + `toDto` (Task 3). ✓
- Versuche honorieren (Badge + eigenes Leaderboard) → `attempt_count` (Task 2), Zähler (Task 4), `buildAttemptsLeaderboard` (Task 3), Badge + Ansicht (Task 8). ✓
- Abgestufte Logik (Geschlecht allein → M/W; +Jahr → AK; Divers/leer → Gesamt) → `filterForScope` (DIVERS nie in MEN/WOMEN; AK nur mit birthYear) (Task 3). ✓
- Nur Geburtsjahr, datensparsam → `birth_year INT` (Task 2), Validierung (Task 4). ✓
- Pros in M/W via category, keine AK ohne Jahr → `effectiveGender` + `toDto` ageGroup null ohne birthYear (Task 3); Admin kann gender/birthYear setzen (Task 5). ✓
- MOST_ATTEMPTS nur PUBLIC → `filterForScope` (Task 3, test in Task 3). ✓
- UI Filter-Leiste + AK-Dropdown → Task 8. ✓
- Geburtsjahr-Bereich refYear-100..refYear-5 → Backend (Task 4) + Frontend Dropdown (Task 7). Konsistent. ✓
- i18n de+en → Tasks 7, 8. ✓
- Version minor + Changelog → Task 9. ✓

**2. Placeholder scan:** `<JDK21>` is an intentional local-path placeholder for the JAVA_HOME workaround (documented in the conventions header), not a code gap. No TBD/TODO. ✓

**3. Type consistency:** `submitPublicEffort(slug, type, displayName, gender, birthYear, fileBytes, originalFilename, clientIp)` — 8 args, used identically in service (Task 4), controller (Task 5) and both backend tests (Tasks 4, 5). `getLeaderboard(slug, type, scope, ageGroup)` consistent across service/controller/test. DTO `SegmentLeaderboardEntryDto` 14 components, built only via `toDto`. Frontend `getLeaderboard(type, scope?, ageGroup?)` and `submitEffort(type, name, file, gender?, birthYear?)` consistent across service/spec/component. `AGE_GROUPS` keys equal backend `AgeGroup.getKey()` values. ✓
