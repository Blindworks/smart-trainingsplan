# Heartbreak Hill — Story-Bild (Share Image) — Design / Spec

- **Datum:** 2026-06-17
- **Autor:** Benedikt Lind (mit Claude Code)
- **Status:** Entwurf zur Abnahme
- **Kontext:** Erweiterung des bestehenden [Heartbreak-Hill-Challenge](2026-06-16-heartbreak-hill-challenge-design.md)-Features. Nach dem anonymen GPX-Upload (Rad oder Lauf) sollen Nutzer im Result-Panel ein **teilbares Bild** erzeugen und herunterladen können — gedacht als **Overlay für Instagram-/TikTok-Stories**: transparenter Hintergrund, damit ein eigenes Foto dahintergelegt werden kann, darauf die markante Höhenkurve des Heartbreak Hill, die erreichten Werte (Tempo, Zeit, Rang) und ein PACR-Logo-Tag als organische Werbung.

---

## 1 · Ziele & Erfolgskriterien

- **Viralität / Werbung:** jeder Upload erzeugt einen teilbaren, gebrandeten Moment → kostenlose Reichweite für PACR.
- **Null Hürde:** funktioniert anonym auf der öffentlichen Seite, kein Login, kein Server-Roundtrip.
- **Instagram-Story-tauglich:** Format **9:16 (1080×1920)**, Hintergrund **transparent** → der Nutzer legt sein eigenes Foto dahinter.
- **Auswahl statt Zwang:** drei Gestaltungsvorlagen (A/B/C), der Nutzer wählt selbst.
- **Erfolgskriterium:** Klick auf „Download" liefert ein transparentes PNG mit korrekter Kurve, korrekten Werten und Logo; Vorschau entspricht exakt dem Ergebnis.

## 2 · Getroffene Entscheidungen (Brainstorming)

| Thema | Entscheidung |
|---|---|
| Format | **Nur Story 9:16** (1080×1920). Quadrat/Landscape vorerst nicht (YAGNI). |
| Inhalt | **Minimal**: Tempo (Rad = km/h, Lauf = min/km), Zeit, Rang + Höhenkurve + PACR-Logo-Tag. Kein Athletenname (Privacy + Klarheit). |
| Vorlagen | **Alle drei** (A · Ridge-Hero, B · Lower-Third-Card, C · Minimal) werden gebaut; **App-Nutzer wählt** per Switcher. Standard: A. |
| Rendering | **Clientseitig auf `<canvas>`** (kein Backend-Bild). Hintergrund nie gefüllt → transparent. |
| Vorschau | Dieselbe Canvas skaliert im UI → **Vorschau = exaktes Ergebnis**, ein Renderer. |
| Teilen | **Download** als Primärweg; auf Mobil zusätzlich **Web-Share-API** (`navigator.canShare({files})`) als progressive Enhancement. |
| Backend | `SegmentEffortResultDto` minimal um Tempo/Pace erweitern (Werte liegen bereits in der Entity). |

## 3 · Architektur-Ansatz

Reine **Frontend-Erweiterung** der bestehenden öffentlichen Komponente `HeartbreakHill` plus eine **minimale Backend-DTO-Ergänzung**. Keine neuen Entities, keine Migration.

Begründung für clientseitiges Canvas statt Server-Rendering: alle nötigen Daten liegen nach dem Upload bereits im Client (Challenge-Polyline, Ergebnis), transparentes PNG ist mit Canvas trivial (`toBlob('image/png')` ohne Hintergrundfüllung), es braucht keine serverseitigen Fonts/Asset-Pipelines, und das Feature läuft so vollständig auf der anonymen `/heartbreak-hill`-Seite ohne Auth. Backend-Rendering (Java2D/Batik) wäre deutlich schwerer ohne Mehrwert.

Wiederverwendung: die bestehende Geometrie-Logik aus `heartbreak-hill.util.ts` (`buildElevationProfile`) wird auf eine gemeinsame Hilfsfunktion `buildElevationPoints()` zurückgeführt, die sowohl das SVG-Hero-Profil als auch die Canvas-Kurve speist (kein Duplikat). PACR-Logo aus bestehendem Asset `assets/logo/PACR_logo_light_text_transparent.png`.

## 4 · UX-Fluss

Im Result-Panel ([heartbreak-hill.html:128](../../../frontend/src/app/components/heartbreak-hill/heartbreak-hill.html)) erscheint unter den Ergebniswerten ein neuer Block „Story-Bild":

1. **Switcher A / B / C** (segmentierter Button, Standard A).
2. **Live-Vorschau** — die gerenderte Canvas, skaliert dargestellt (≈ 240 px breit, 9:16). Wechsel der Vorlage rendert neu.
3. **Download-Button** → transparentes PNG, Dateiname `heartbreak-hill-rang<rank>-<rad|lauf>.png`.
4. **Auf Mobil zusätzlich „Teilen"** (nur wenn `navigator.canShare` mit Files unterstützt) → teilt das PNG direkt über das System-Share-Sheet (Story).

## 5 · Die drei Vorlagen

Alle 1080×1920, transparenter Hintergrund. Gezeichnet werden ausschließlich Kurve, Werte und Logo-Tag.

- **A · Ridge-Hero** — Höhenkurve als große **gefüllte Silhouette** in PACR-Grün (`#8ffc2e`) am unteren ~43 %; darüber die drei Werte in einer Reihe (Rang-Wert grün), oben Segment-Tag, unten rechts PACR-Tag. Oberer Bildbereich frei fürs Foto. **Bold, maximal markentauglich.**
- **B · Lower-Third-Card** — halbtransparente dunkle Karte im unteren Drittel (`rgba(11,15,20,.58)`), darin dünne grüne Kurvenlinie als Akzent, Kopfzeile mit Segmentname + PACR, darunter Werte-Reihe. **Maximal lesbar auf jedem Foto.**
- **C · Minimal** — nur **dünne grüne Kurvenlinie** (keine Füllung) tief im Bild, Werte schwebend unten links mit Schatten, PACR-Tag unten rechts, Segment-Tag oben links. **Maximal durchsichtig, dezent.**

Gemeinsame Tokens: PACR-Grün `#8ffc2e`, Werte weiß mit dezentem Schatten zur Lesbarkeit, Logo-Tag als dunkle Pille (lesbar auf hellem wie dunklem Foto). Mikro-Labels in der Sprache der App.

## 6 · Daten aufs Bild

Rad → **km/h**, Lauf → **min/km** (Pace `m:ss`).

| Element | Quelle |
|---|---|
| Höhenkurve | `polylinePoints()` (bereits geparst) → `buildElevationPoints()`. Fallback: synthetischer Ridge wie im Hero, falls keine Geometrie geseedet ist. |
| Tempo / Pace | **neu** aus `EffortResult.avgSpeedKmh` (Rad) bzw. `EffortResult.avgPaceSecondsPerKm` (Lauf) |
| Zeit | `result().elapsedFormatted` |
| Rang | `result().rank` / `result().totalCount` |
| Segmentname | `challenge().name` (für das Segment-Tag) |
| Logo | `assets/logo/PACR_logo_light_text_transparent.png` |

## 7 · Rendering-Pipeline (Canvas)

1. Offscreen-`<canvas>` 1080×1920; **kein** `fillRect` des Hintergrunds → bleibt transparent.
2. `await document.fonts.ready`, damit `fillText` die App-Schrift nutzt.
3. Logo-`Image` (same-origin Asset → kein Canvas-Tainting) laden.
4. Template-spezifische Zeichenfunktion (`drawTemplateA|B|C`) zeichnet Kurve (Pfad aus normalisierten Punkten), Werte (`fillText`) und Logo-Tag (`drawImage` in abgerundete Pille).
5. **Vorschau:** dieselbe Canvas skaliert im DOM anzeigen.
6. **Download:** `canvas.toBlob(blob => …, 'image/png')` → Object-URL → `<a download>` → URL freigeben.
7. **Teilen (Mobil):** `new File([blob], name, {type:'image/png'})` → `navigator.share({files:[file]})`, falls `canShare`.

Neue Datei `share-image.util.ts`:
- `buildElevationPoints(points, w, h): {x,y}[]` — normalisierte Profilpunkte (von `buildElevationProfile` mitgenutzt).
- `formatTempo(type, avgSpeedKmh, avgPaceSecondsPerKm, locale): string` — „23,4 km/h" bzw. „4:02 /km".
- `shareFileName(rank, type): string`.
- `drawShareImage(ctx, data, template, labels, logo): void` — orchestriert A/B/C.

## 8 · Backend-Änderung (minimal)

- `SegmentEffortResultDto`: zwei Felder ergänzen — `Double avgSpeedKmh`, `Integer avgPaceSecondsPerKm`.
- `SegmentChallengeService.buildResult(...)` ([Zeile 288](../../../backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java)): `saved.getAvgSpeedKmh()` und `saved.getAvgPaceSecondsPerKm()` durchreichen.
- Frontend-Model `EffortResult` ([heartbreak-hill.model.ts:33](../../../frontend/src/app/models/heartbreak-hill.model.ts)): beide Felder spiegeln.

Keine Migration (Spalten existieren bereits in `segment_efforts`).

## 9 · Edge Cases

- **Keine Geometrie** (`polylineJson` null) → synthetischer Ridge, Bild rendert trotzdem.
- **Tempo/Pace null** (GPX ohne ausreichende Daten) → Wert ausblenden, restliches Layout bleibt stabil.
- **Fonts noch nicht geladen** → erst nach `document.fonts.ready` zeichnen.
- **Web-Share nicht unterstützt** (Desktop) → „Teilen"-Button nicht anzeigen, Download genügt.
- **Vorlagenwechsel** während des Renderns → letzte Auswahl gewinnt (Renderer ist synchron bis auf Logo-Load; Logo einmalig cachen).

## 10 · i18n

Neue Keys unter `HEARTBREAK_HILL.SHARE_*` in `en.json` **und** `de.json`, u. a.:
`SHARE_TITLE`, `SHARE_HINT`, `SHARE_DOWNLOAD`, `SHARE_SHARE`, `SHARE_TPL_A`, `SHARE_TPL_B`, `SHARE_TPL_C`, sowie die auf die Canvas gezeichneten Labels `SHARE_LBL_TEMPO`, `SHARE_LBL_PACE`, `SHARE_LBL_TIME`, `SHARE_LBL_RANK`, `SHARE_LBL_OF`.

## 11 · Tests

- **Backend:** `SegmentChallengeServiceDedupeTest` (ruft bereits `submitPublicEffort` auf und prüft das `SegmentEffortResultDto`) um eine Assertion erweitern, dass `avgSpeedKmh` (Rad) bzw. `avgPaceSecondsPerKm` (Lauf) im DTO ankommen.
- **Frontend (Vitest):** reine Helfer testen — `buildElevationPoints` (Normalisierung, < 2 Punkte → leer), `formatTempo` (Rad-km/h vs. Lauf-Pace, null-Fälle), `shareFileName`. Das Canvas-Pixelergebnis wird **nicht** unit-getestet (jsdom hat kein echtes 2D-Canvas) → **manuelle Sichtprüfung** beim App-Start (alle drei Vorlagen, Rad + Lauf, mit/ohne Foto dahinter).

## 12 · Version & Changelog

- Neues Feature → **minor**-Bump via `./version-bump.sh minor` (pom.xml + package.json im selben Commit).
- `CHANGELOG.md` unter `[Unreleased] / Added`: teilbares transparentes Story-Bild für die Heartbreak-Hill-Challenge (drei Vorlagen, Tempo/Zeit/Rang, PACR-Logo).

## 13 · Betroffene Dateien

**Neu**
- `frontend/src/app/components/heartbreak-hill/share-image.util.ts`
- `frontend/src/app/components/heartbreak-hill/share-image.util.spec.ts`

**Geändert**
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts` — Share-Signals, Render-/Download-/Share-Methoden, `TranslateService` für Canvas-Labels
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html` — Share-Block (Switcher, Vorschau-Canvas, Buttons)
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss` — Styles für Switcher/Vorschau
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.ts` — `buildElevationPoints` extrahieren, `buildElevationProfile` darauf zurückführen
- `frontend/src/app/models/heartbreak-hill.model.ts` — `EffortResult` um Tempo/Pace
- `frontend/src/assets/i18n/en.json`, `de.json` — `SHARE_*`-Keys
- `backend/src/main/java/com/trainingsplan/dto/SegmentEffortResultDto.java` — zwei Felder
- `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java` — `buildResult` reicht Tempo/Pace durch
- `backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceDedupeTest.java` — Assertion auf Tempo/Pace
- `pom.xml`, `frontend/package.json`, `CHANGELOG.md` — Version + Changelog

## 14 · Nicht im Scope (YAGNI)

- Quadrat-/Landscape-Formate.
- Zusätzliche Werte (Höhenmeter, Rückstand, Ø-Puls/-Watt) — bewusst minimal.
- Athletenname / Avatar auf dem Bild.
- Serverseitige Bildgenerierung, Persistenz oder Share-Links.
