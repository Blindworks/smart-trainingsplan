# Heartbreak Hill — Erweiterte Rankings (Geschlecht, Altersklassen, Versuche)

**Datum:** 2026-06-18
**Status:** Design abgestimmt, Spec-Review ausstehend
**Kontext:** Erweiterung des bestehenden Heartbreak-Hill-Challenge-Features (Backend + 2D/3D-Frontend, v0.52.0). Siehe Vorgänger-Spec `2026-06-16-heartbreak-hill-challenge-design.md`.

## Ziel

Die öffentliche Heartbreak-Hill-Bestenliste bekommt zusätzliche Wertungsdimensionen:

1. **Gesamtranking** (allgemeine Liste) — existiert bereits, bleibt unverändert.
2. **Geschlechtergetrennte Wertung** — Männer / Frauen.
3. **Altersklassen-Wertung** — Ironman-5-Jahres-Schema, **geschlechtergetrennt** (z.B. „Männer 30-34").
4. **Versuche honorieren** — Anzahl der Uploads pro Person als Badge + eigenes „Meiste Versuche"-Leaderboard.

Teilnahme an Sonder-Wertungen ist **freiwillig und abgestuft**: Wer Geschlecht + Geburtsjahr angibt, erscheint zusätzlich in M/W bzw. AK. Wer nichts angibt, bleibt im Gesamtranking.

## Abgestimmte Entscheidungen (Decision Log)

| Frage | Entscheidung |
|-------|--------------|
| Versuche honorieren | Zeit bleibt Hauptwertung. Zusätzlich: Versuchs-Badge **und** eigenes „Meiste Versuche"-Leaderboard. |
| Geschlechts-Optionen | `MALE` / `FEMALE` / `DIVERS`. Divers wird gespeichert, erhält aber **keine** eigene Rangliste (nur Gesamt). |
| Geburtsdatum | Nur **Geburtsjahr** (datensparsam, DSGVO-freundlich). |
| AK-Schema | Ironman 5-Jahres: U18, 18-24, 25-29, 30-34, 35-39, 40-44, 45-49, 50-54, 55-59, 60-64, 65-69, 70+. |
| AK × Geschlecht | **Getrennt** — jede AK existiert für Männer und Frauen separat. |
| AK-Berechnung | **Live** aus dem Geburtsjahr: `alter = referenzJahr − geburtsjahr`, `referenzJahr = challenge.eventDate.year ?? aktuelles Jahr`. Nicht gespeichert → kein Stale-Problem beim Jahreswechsel; entspricht der Ironman-Logik „Alter am Jahresende". |
| Wertungs-Logik | Abgestuft: Geschlecht (M/W) allein → M/W-Wertung. Geschlecht (M/W) + Geburtsjahr → zusätzlich AK. Divers/leer → nur Gesamt. |
| UI-Navigation | Filter-Leiste `[Gesamt] [Männer] [Frauen] [Meiste Versuche]` unter den RIDE/RUN-Tabs; bei Männer/Frauen erscheint zusätzlich ein AK-Dropdown („Alle Altersklassen" / „30-34" / …). |
| Referenz-Efforts (Pros) | `gender` wird aus der bestehenden `category` abgeleitet (PRO_MEN→MALE, PRO_WOMEN→FEMALE), damit die Großen in der M/W-Wertung erscheinen. Ohne Geburtsjahr keine AK. Referenz-Efforts erscheinen **nicht** im „Meiste Versuche"-Leaderboard. |
| Architektur | **Ansatz A**: neue Spalten am `segment_efforts` + Live-Filterung im Service (kein Historie-Schema, keine materialisierten Ränge). |

## Architektur (Ansatz A)

Erweiterung der bestehenden Tabelle `segment_efforts` um drei Spalten; die Ranglisten werden zur Laufzeit aus diesen Feldern gefiltert und sortiert. Datenmengen sind klein (öffentliches Werbe-Feature), der vorhandene Index `idx_segment_efforts_leaderboard` trägt die Sortierung. Die Altersklasse wird nie persistiert, sondern bei jeder Leaderboard-Abfrage aus dem Geburtsjahr berechnet.

### 1. Datenmodell — Migration `144-add-segment-effort-demographics.xml`

> Nummer vor dem Anlegen gegen `db.changelog-master.xml` final prüfen (aktuell höchste = 143). `<preConditions onFail="MARK_RAN">` **vor** `<comment>` setzen.

Neue Spalten auf `segment_efforts`:

| Spalte | Typ | Null | Default | Zweck |
|--------|-----|------|---------|-------|
| `gender` | VARCHAR(10) | ja | — | `MALE` / `FEMALE` / `DIVERS`; null = keine Angabe |
| `birth_year` | INT | ja | — | Geburtsjahr; null = keine Angabe |
| `attempt_count` | INT | nein | 1 | Anzahl gewerteter Uploads dieser Person |

Bestehende Zeilen erhalten `attempt_count = 1` (Default). Kein Backfill für `gender`/`birth_year` nötig (Referenz-Gender wird live aus `category` abgeleitet).

### 2. Backend

**Neues Enum** `entity/Gender.java`: `{ MALE, FEMALE, DIVERS }`.

**Neue Klasse** `entity/AgeGroup.java` (oder `util/AgeGroups.java`) — Ironman-5-Jahres-Buckets mit stabilem API-Key:

```
U18    (alter < 18)        key "U18"
18-24  (18..24)            key "18-24"
25-29 … 65-69              key "25-29" …
70+    (alter >= 70)       key "70+"
```

- `AgeGroup forAge(int age)` → Bucket
- `AgeGroup fromBirthYear(int birthYear, int referenceYear)` → Bucket
- `AgeGroup fromKey(String key)` → für den API-Filter (null/ungültig → kein Filter)

**`SegmentEffort` Entity:** Felder `gender`, `birthYear`, `attemptCount` + Getter/Setter.

**`SegmentChallengeService`:**

- `submitPublicEffort(...)` bekommt zusätzlich `Gender gender, Integer birthYear`:
  - Validierung Geburtsjahr: plausibel, falls gesetzt (z.B. `referenzJahr − 100 ≤ birthYear ≤ referenzJahr − 5`); implausibel → `IllegalArgumentException("invalid_birth_year")` (422).
  - Neuer Effort: `gender`/`birthYear` setzen, `attemptCount = 1`.
  - Bestehende Identität (PB-Update-Zweig): `attemptCount++` **immer** (auch wenn der Versuch langsamer war); bei neuer Bestzeit zusätzlich Zeit/Tempo/Track aktualisieren; `gender`/`birthYear` mit den zuletzt gelieferten Werten überschreiben, falls vorhanden. Dedupe-Identität bleibt `(challengeId, type, displayName)` — Geschlecht/Jahr ändern den Schlüssel **nicht**.
- `getLeaderboard(slug, type, scope, ageGroupKey)`:
  - `scope ∈ { OVERALL, MEN, WOMEN, MOST_ATTEMPTS }` (Enum `LeaderboardScope`).
  - `OVERALL` → bisheriges Verhalten (REF + PUBLIC, nach Zeit).
  - `MEN` / `WOMEN` → Filter `effektivesGeschlecht == MALE/FEMALE`; optional zusätzlich AK-Filter via `ageGroupKey` (nur PUBLIC-Efforts mit Geburtsjahr fallen in eine AK; REFERENCE ohne Jahr erscheinen nur in der ungefilterten M/W-Liste). Sortierung nach Zeit.
  - `MOST_ATTEMPTS` → nur PUBLIC, sortiert nach `attemptCount` desc, Tie-Break Zeit asc.
  - „Effektives Geschlecht": gespeichertes `gender`, sonst für REFERENCE aus `category` abgeleitet (PRO_MEN→MALE, PRO_WOMEN→FEMALE), sonst null.
- Reihenfolge/Rang-Logik bleibt in `buildLeaderboard(...)` (Komparator + Tie-Rank), erweitert um eine Variante für „nach Versuchen sortiert".

**DTO `SegmentLeaderboardEntryDto`** +`String gender`, +`String ageGroup` (Key, null falls keine), +`int attemptCount`. Frontend-Model spiegeln.

**Controller `PublicSegmentChallengeController`:**
- `GET /{slug}/leaderboard` bekommt `@RequestParam(defaultValue="OVERALL") LeaderboardScope scope` und `@RequestParam(required=false) String ageGroup`.
- `POST /{slug}/efforts` bekommt optionale `@RequestParam(required=false) Gender gender` und `@RequestParam(required=false) Integer birthYear`.

**Admin-Referenz-Endpoint** (`AdminSegmentChallengeController` / `addReferenceEffort`): optionale `gender` + `birthYear` durchreichen; falls nicht gesetzt und `category` ist PRO_MEN/PRO_WOMEN → ableiten. (Damit kann ein Pro auch in eine AK gesetzt werden, falls gewünscht.)

### 3. Frontend (`frontend/src/app/components/heartbreak-hill/`)

**Upload-Formular** (`heartbreak-hill.html` / `.ts`):
- Geschlechts-Select: „Keine Angabe" (default) / Männlich / Weiblich / Divers.
- Geburtsjahr: Dropdown oder Number-Input (Bereich `referenzJahr−100 … referenzJahr−5`), optional.
- Dezenter Hinweis: „Für die Geschlechts- und Altersklassenwertung — optional."
- `submitEffort(...)` im Service reicht `gender`/`birthYear` mit.

**Filter-Leiste + AK-Dropdown:**
- Segmented Control: `[Gesamt] [Männer] [Frauen] [Meiste Versuche]`.
- Bei Männer/Frauen erscheint ein AK-`<select>` („Alle Altersklassen" + AK-Keys).
- Auswahl → Leaderboard-Reload mit `scope` (+ `ageGroup`).
- Signals: `rankingScope`, `selectedAgeGroup`. AK-Liste als Konstante im Component/Model.

**Leaderboard-Tabelle:**
- Neue Spalte/Badge „Versuche" (z.B. `🔁 ×5`), sichtbar in allen Zeit-Wertungen ab count ≥ 2.
- „Meiste Versuche"-Ansicht: Versuchszahl prominent (Hauptspalte), Zeit sekundär; Empty-State wenn leer.
- M/W- und AK-Ansichten: Empty-State „Noch keine Einträge — lade dein GPX mit Geschlecht/Jahrgang hoch."

**Model** (`heartbreak-hill.model.ts`): `LeaderboardEntry` +`gender`, +`ageGroup`, +`attemptCount`. Neue Typen `LeaderboardScope`, `AgeGroupKey`.

**i18n** (`assets/i18n/en.json` + `de.json`): Schlüssel unter `HEARTBREAK_HILL` für Scope-Labels, AK-Dropdown, Geschlechts-Optionen, Geburtsjahr-Label/Hinweis, Versuche-Badge, neue Empty-States.

### 4. Edge Cases & Validierung

- **Geburtsjahr implausibel** → 422 `invalid_birth_year`; Frontend zeigt Inline-Fehler. (Dropdown begrenzt den Bereich ohnehin.)
- **Nur Geburtsjahr ohne Geschlecht** → keine AK (AK ist M/W-getrennt), keine M/W-Wertung; nur Gesamt. Konsistent mit „abgestuft".
- **Divers** → in `MEN`/`WOMEN`/AK-Scopes ausgeschlossen; nur `OVERALL` + `MOST_ATTEMPTS`.
- **Mehrfach-Upload mit wechselnden Angaben** → letzter Upload gewinnt für `gender`/`birthYear`; `attemptCount` zählt jeden gewerteten Upload.
- **Referenz ohne ableitbares Geschlecht** (category COMMUNITY/KONA_QUALIFIER/AGE_GROUP) → nur Gesamt, außer Admin setzt `gender` explizit.
- **Leere AK** → Dropdown bietet alle Keys; leere Auswahl liefert Empty-State.

### 5. Tests

- `AgeGroup`-Util: Grenzfälle (17/18, 24/25, 69/70/71), `fromBirthYear` mit Referenzjahr, `fromKey` (gültig/ungültig).
- Leaderboard-Filter: OVERALL unverändert; MEN/WOMEN filtert korrekt inkl. abgeleitetem Referenz-Gender; AK-Filter; Divers ausgeschlossen; MOST_ATTEMPTS-Sortierung + Tie-Break; PUBLIC-only.
- Versuche-Zähler: zweiter Upload (langsamer) erhöht `attemptCount`, behält Bestzeit; schnellerer Upload erhöht Zähler und aktualisiert Zeit.
- Hinweis Test-Setup: Tests booten keinen Kontext / standaloneSetup (siehe Memory `feedback_backend_testing`).

### 6. Versionierung & Changelog

- `version-bump.sh minor` (neues Feature: Endpoints/Spalten/UI).
- `CHANGELOG.md` unter `[Unreleased]` → `Added`: geschlechtergetrennte + Altersklassen-Rangliste, Versuche-Wertung.

## Out of Scope (YAGNI)

- Versuchs-**Historie**/Verlaufskurve (nur Zähler).
- Eigene Divers-Rangliste.
- Materialisierte/gecachte Ränge.
- Punkte-/Kombi-Score (Zeit bleibt alleiniges Kriterium der Zeit-Wertungen).
- Pflicht-Verknüpfung Geburtsjahr an Account / Verifizierung des Alters.
```
