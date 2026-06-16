# Heartbreak Hill Challenge — Design / Spec

- **Datum:** 2026-06-16
- **Autor:** Benedikt Lind (mit Claude Code)
- **Status:** Entwurf zur Abnahme
- **Kontext:** Öffentliches Werbe-/Wachstums-Feature für PACR rund um den **Ironman Frankfurt am 28.06.2026**. Der Radkurs führt erstmals wieder durch **Bad Vilbel** über den berühmten **Heartbreak Hill**. Ziel: eine öffentliche, atemberaubende Seite, auf der sich jeder am Heartbreak Hill messen kann — virales Reichweiten-Feature **und** Conversion-Funnel in PACR.

---

## 1 · Ziele & Erfolgskriterien

- **Viralität:** möglichst niedrige Hürde zum Mitmachen (anonymer GPX-Upload, sofortiges Ergebnis, teilbares Resultat).
- **Conversion:** der „Win-Moment" (Ergebnis sichern, Verlauf, Ghost-Duelle) führt in die kostenlose PACR-Registrierung.
- **Emotion „miss dich mit den Großen":** Nutzer sehen ihren Rang direkt zwischen kuratierten Top-Athleten.
- **Wow-Faktor:** echte 3D-Ansicht des Hügels im PACR-Design mit Animationen.
- **Erfolgskriterien:** Seite vor dem 28.06. live; anonymer Upload → Ranking in < 10 s; messbare Signups aus dem Funnel; Feature ist nach dem Rennen für weitere Segment-Kampagnen wiederverwendbar.

## 2 · Getroffene Entscheidungen (Brainstorming)

| Thema | Entscheidung |
|---|---|
| Teilnahme-Modell | **Hybrid-Funnel**: anonymer Upload + Anzeigename → sofort im Ranking; „Ergebnis sichern", Verlauf, Ghost-Duelle = kostenloses PACR-Konto |
| Benchmark „die Großen" | **Admin-kuratiertes Roster**: Benedikt lädt nach und nach echte GPX-Tracks von Athleten (mit bekannten Zeiten) hoch → REFERENCE-Efforts. PACR führt die Bestenliste selbst. **Keine** Strava-Leaderboard-API (2020 abgeschaltet) und **kein** Scraping (ToS-Verstoß). Hinweis: nur öffentlich verfügbare Namen verwenden. |
| Strava | Bleibt optionaler **Input** (Segment-Definition + eigene Segmentzeit über `segment_efforts`) im authentifizierten Pfad — Phase 3, nicht MVP. |
| 3D-Ansicht | **Echtes WebGL-Terrain** (Three.js), gebackenes Höhenmodell als Asset, 2D-Höhenprofil als Fallback |
| Sportarten | **Rad + Lauf**, getrennte Bestenlisten und getrennte Ghosts |
| Architektur | **Isoliertes Modul** (neue Entities/Endpoints/Route), Wiederverwendung der bestehenden Parser & Geo-Utils |

## 3 · Architektur-Ansatz

Eigenständiges, von der PRO-gegateten Community-Routes-Welt **entkoppeltes** Modul. Begründung: die bestehende Leaderboard-Infrastruktur (`CommunityRoute`, `RouteAttempt`, `LeaderboardService`) ist vollständig hinter Login + `@RequiresSubscription(PRO)` und setzt überall einen eingeloggten User voraus — ungeeignet für ein öffentliches, anonymes Feature. Das neue Modul ist generisch (`SegmentChallenge`), sodass nach dem Ironman weitere berühmte Segmente als Kampagnen möglich sind (Alpe d'Huez, Mortirolo …).

Wiederverwendet werden: `GpxParsingService` (+ FIT/TCX-Parser), Haversine-/Geo-Utilities, Leaflet/`route-mini-map`-Muster, das Design-System (CSS-Variablen, `--pp: #8ffc2e`).

## 4 · Datenmodell

### Entity `SegmentChallenge`
Die Kampagne / das Segment selbst.

- `id`
- `slug` (unique, z. B. `heartbreak-hill-2026`) → öffentliche URL
- `name`, `subtitle`, `eventDate` (`2026-06-28`)
- `startLat`, `startLng`, `endLat`, `endLng` — die „Tore" des Segments
- `distanceM`, `elevationGainM`, `avgGradePct`, `maxGradePct`
- `polylineJson` (LONGTEXT) — kanonische Ideallinie `[[lat,lng,ele],…]`; speist 3D, Karte und Matching-Korridor
- `terrainAssetRef` — Verweis auf das gebackene Höhen-Asset
- `boundingBoxJson` — Min/Max lat/lng (für Terrain & Korridor)
- `active` (boolean), `createdAt`, `updatedAt`

### Entity `SegmentEffort`
Jeder Eintrag in der Bestenliste.

- `id`, `challengeId` (FK)
- `activityType` (enum `RIDE` | `RUN`)
- `kind` (enum `REFERENCE` = Admin-Roster „die Großen" | `PUBLIC` = Upload)
- `displayName`
- `category` (nullable enum: `PRO_MEN`, `PRO_WOMEN`, `KONA_QUALIFIER`, `AGE_GROUP`, …)
- `elapsedSeconds`, `avgSpeedKmh`, `avgPaceSecPerKm`, `avgPowerW` (nullable), `avgHr` (nullable)
- `trackJson` (LONGTEXT) — auf das Segment zugeschnittener Track `[[lat,lng,ele,tSec],…]` → Ghost-Animation
- `sourceFormat` (`GPX`|`FIT`|`TCX`|`STRAVA`)
- `claimedByUserId` (nullable FK `User`) — gesetzt, wenn ein anonymer Upload per Signup beansprucht wird
- `editToken` (nullable, opaque) — erlaubt dem anonymen Uploader, seinen Eintrag später zu verwalten/claimen
- `status` (enum `VALID` | `PENDING` | `REJECTED`)
- `ipHash` (nullable) — leichtes Anti-Abuse
- `createdAt`

**Index:** `(challengeId, activityType, status, elapsedSeconds)` für die Bestenlisten-Sortierung.

**Migration:** Liquibase `140-create-segment-challenge.xml` (zwei changeSets: `140-1` Challenges, `140-2` Efforts inkl. FK + Leaderboard-Index), `preConditions` **vor** `comment`, Include in `db.changelog-master.xml`. (Letzte vorhandene Migration ist 139.)

## 5 · Backend

### `SegmentMatchingService` (Kernlogik, TDD)
Eingabe: geparster Track (latlng + Höhe + Zeit pro Punkt). Ablauf:
1. Finde Einstiegspunkt = nächster Trackpunkt zum **Start-Tor** innerhalb Radius (z. B. 35 m).
2. Finde Ausstiegspunkt = nächster Trackpunkt zum **End-Tor** **nach** dem Einstieg.
3. `elapsedSeconds = t(exit) − t(entry)`.
4. Validierung: Reihenfolge & Richtung korrekt; Stichprobe der Zwischenpunkte liegt im Korridor um die Polyline; Plausibilität (max. Speed, Mindestdauer, GPS-Punktdichte).
5. Rückgabe: `elapsedSeconds` + zugeschnittener `trackJson` + Metriken **oder** strukturierter Ablehnungsgrund.

> Erweiterung des `GpxParsingService` nötig: Höhe (`<ele>`) und absolute/relative Zeit **pro Punkt** zurückgeben (heute werden u. a. nur aggregierte Werte berechnet).

### `SegmentChallengeService`
- `getBySlug(slug)` — Challenge-Meta + Polyline + Terrain-Referenz
- `leaderboard(challengeId, activityType, filter)` — REFERENCE + PUBLIC gemischt, sortiert nach `elapsedSeconds` ASC; berechnet Rang, Perzentil, Abstand zur Top-/Profi-Zeit
- `submitPublicEffort(slug, activityType, displayName, file)` — parsen → matchen → validieren → als `PUBLIC` speichern → Ergebnis (Rang/Perzentil/Gap) + `editToken` zurück
- `getEffortTrack(effortId)` — Ghost-Track für die 3D-Ansicht
- `addReferenceEffort(...)` — Admin: benannter GPX + Kategorie + (bekannte) Zeit
- `claimEffort(effortId, editToken, user)` — verknüpft anonymen Effort nach Signup mit User

### Controller / Endpoints
- **Öffentlich** (`permitAll`, neuer Pfad `/api/public/**`):
  - `GET /api/public/challenges/{slug}`
  - `GET /api/public/challenges/{slug}/leaderboard?type=RIDE|RUN`
  - `GET /api/public/challenges/{slug}/efforts/{id}/track`
  - `POST /api/public/challenges/{slug}/efforts` (multipart: `file`, `displayName`, `type`)
- **Admin** (bestehendes Admin-Gate):
  - `POST /api/admin/challenges` (anlegen/aktualisieren, Tore/Polyline setzen)
  - `POST /api/admin/challenges/{slug}/reference-efforts` (Datei + Name + Kategorie + Typ + optionale Zeit-Override)
  - `PUT`/`DELETE` zum Moderieren/Verbergen von Efforts

### Security & Anti-Abuse
- `/api/public/**` in `SecurityConfig` auf `permitAll` (GET + POST der Effort-Submission).
- Dateigrößen-Limit; Rate-Limit pro IP für POST; Plausibilitätschecks im Matching; `status`-Moderation (Admin kann verbergen/ablehnen); `ipHash` statt Roh-IP (DSGVO).
- Keine User-Annahmen im öffentlichen Pfad (kein `securityUtils.getCurrentUser()` erzwungen).

## 6 · Frontend

- **Öffentliche Angular-Route** `/heartbreak-hill` (standalone Component, **ohne** `authGuard`).
- Seitenaufbau (Scroll-Narrativ, PACR-Dark, `--pp: #8ffc2e`, Animationen):
  1. **Hero** — 3D-Terrain (Phase 2; Phase 1: 2D-Höhenprofil), Titel, Event-Zeile, Segment-Stat-Chips, Haupt-CTA.
  2. **3D-Duell** — Athlet aus Liste wählen → sein Ghost klettert neben deinem; drehbar; 2D-Fallback.
  3. **Bestenliste** — Tabs Rad/Lauf, REFERENCE (Badges) + PUBLIC gemischt, eigene Zeile hervorgehoben (Rang + Gap).
  4. **Mitmachen** — Drag-&-Drop-Upload, Anzeigename, Rad/Lauf-Umschalter → sofortiges Ergebnis-Reveal → Funnel-CTA + Teilen/Badge.
  5. **PACR-Pitch** — Abschluss-CTA zur Registrierung.
- **i18n:** alle Texte in `en.json` + `de.json` (ngx-translate), Nutzung via `translate`-Pipe.
- **Karten/Mini-Map:** optional Leaflet (bestehendes Muster) für eine 2D-Streckenübersicht.

## 7 · 3D-Terrain (Phase 2)

- **Offline-Baking-Skript** (einmalig, nicht im Laufzeitpfad): zieht ein DEM (Höhenraster) für die Bounding-Box des Hügels, downsampled auf z. B. 128×128, exportiert als JSON/PNG-Asset → eingecheckt unter den Frontend-Assets. Keine Live-Geo-API im Betrieb.
- **`Heartbreak3dComponent`** (gekapselt, lazy): Three.js — displaced Plane aus Heightmap, low-poly Stil in PACR-Grün, leuchtende Route auf dem Hang, Auto-Orbit + Drag-Orbit, **Ghost-Marker** animiert aus echten `trackJson`-Daten (Sekunde für Sekunde).
- **Fallback:** kein WebGL / schwache GPU / Fehler → automatisches 2D-Höhenprofil (SVG/Canvas), gleiche Datenbasis.

## 8 · Funnel & Conversion

1. Anonym: Upload + Anzeigename → Ergebnis-Reveal (Rang, Gap zur Profi-Zeit, Perzentil).
2. Win-Moment: „Ergebnis sichern / Verlauf / Ghost-Duelle" → kostenloses PACR-Konto. Nach Signup `claimEffort(editToken)`.
3. Teilen: teilbares Ergebnis (Badge/Link) als Reichweiten-Loop.
4. Abschluss-Pitch: „Trainier für deinen eigenen Heartbreak Hill" → Registrierung.

## 9 · Scope / MVP & Phasen

**Phase 1 — Fundament & funktionierende Challenge (ohne 3D), launchbar:**
Entities + Migration; `SecurityConfig`; `SegmentMatchingService` (TDD); öffentliche Endpoints + Admin-Roster-Upload; Public-Route mit voller Seite (Hero als 2D-Höhenprofil), Bestenliste, Upload, Ergebnis-Reveal, Funnel, Pitch; i18n.

**Phase 2 — 3D-Wow:** Baking-Skript + Asset; `Heartbreak3dComponent` (Three.js) mit Ghost-Animation; 2D-Fallback.

**Phase 3 — Politur & Viralität (Stretch):** interaktives Ghost-Duell (Timeline-Scrub); generierte teilbare Badge-Grafik; Countdown bis 28.06.; FIT/TCX-Upload; Strava-Connect-Auto-Import (auth. Pfad).

## 10 · Risiken & Mitigation

- **3D rutscht zeitlich** → Phase 1 ist bereits vollwertig live; 3D ist additiv.
- **GPX-Matching ungenau** → TDD mit echten Tracks (von Benedikt) als Fixtures; Korridor- & Plausibilitätschecks.
- **Missbrauch öffentlicher Uploads** → Limits, `ipHash`, Admin-Moderation, `status`-Workflow.
- **Rechtliches (Athletennamen)** → nur öffentlich verfügbare Namen im Roster.

## 11 · Out of Scope (YAGNI)

Echte Strava-Leaderboard-API / Scraping; Profi-Klarnamen ohne öffentliche Quelle; Live-Geo-API im Betrieb; Microsite-Doppelinfrastruktur.

## 12 · Annahmen / Offene Punkte

- Segment-Stats (Länge/Hm/Steigung) und Polyline werden vor dem Launch mit echten Daten befüllt (aus dem Strava-Segment bzw. einem Referenz-GPX).
- Admin-Bereich nutzt das bestehende Admin-Shell-/Routing-Muster (kein MatDialog).
- Arbeit direkt auf `main` (kein PR, kein Worktree).
