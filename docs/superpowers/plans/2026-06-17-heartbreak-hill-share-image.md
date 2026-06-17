# Heartbreak Hill Share Image Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nach dem GPX-Upload auf `/heartbreak-hill` ein transparentes 9:16-Story-Bild (Höhenkurve + Tempo/Zeit/Rang + PACR-Logo) clientseitig rendern und herunterladen lassen — drei vom Nutzer wählbare Vorlagen.

**Architecture:** Reine Frontend-Erweiterung der bestehenden `HeartbreakHill`-Komponente plus eine minimale Backend-DTO-Durchreichung (Tempo/Pace liegen schon in der Entity). Das Bild wird auf einem Offscreen-`<canvas>` (1080×1920, Hintergrund nie gefüllt → transparent) gezeichnet; dieselbe Canvas dient skaliert als Live-Vorschau. Keine neue Entity, keine Migration.

**Tech Stack:** Angular 19 (standalone, Signals), `@ngx-translate`, Canvas 2D API, Vitest (Frontend), Spring Boot + JUnit5/Mockito (Backend).

**Hinweis Versionierung:** Laut `CLAUDE.md` wird vor jedem Commit die Version gebumpt. Für dieses Feature gilt das pragmatisch **einmal** (ein `minor`-Bump im Abschluss-Commit, Task 9). Die Zwischen-Commits der Tasks 1–8 **nicht** bumpen. Arbeit geht direkt auf `main` (kein Branch, kein PR).

---

## File Structure

**Neu**
- `frontend/src/app/components/heartbreak-hill/share-image.util.ts` — reine Helfer (`formatTempo`, `shareFileName`, Typen, Konstanten) + Canvas-Renderer (`drawShareImage`, `loadImage`, Templates A/B/C). Eine Datei, weil Renderer und seine Format-Helfer zusammen geändert werden.
- `frontend/src/app/components/heartbreak-hill/share-image.util.spec.ts` — Vitest-Tests der reinen Helfer.

**Geändert**
- `backend/src/main/java/com/trainingsplan/dto/SegmentEffortResultDto.java` — zwei Felder.
- `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java` — `buildResult()` reicht Tempo/Pace durch.
- `backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceDedupeTest.java` — Assertion auf Tempo/Pace.
- `frontend/src/app/models/heartbreak-hill.model.ts` — `EffortResult` um zwei Felder.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.ts` — `buildElevationPoints` extrahieren, `buildElevationProfile` darauf zurückführen.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.spec.ts` — Tests für `buildElevationPoints`.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts` — Share-Signals, Render/Download/Share-Methoden.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html` — Share-Block.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss` — Styles.
- `frontend/src/assets/i18n/de.json`, `frontend/src/assets/i18n/en.json` — `SHARE_*`-Keys.
- `pom.xml`, `frontend/package.json`, `CHANGELOG.md` — Version + Changelog (Task 9).

---

## Task 1: Backend — Tempo/Pace ins Result-DTO

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/dto/SegmentEffortResultDto.java`
- Modify: `backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java:288-306`
- Test: `backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceDedupeTest.java`

- [ ] **Step 1: Failing test schreiben**

In `SegmentChallengeServiceDedupeTest.java` als neue Methode (vor der schließenden `}` der Klasse) einfügen:

```java
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
                "<gpx/>".getBytes(), "run.gpx", "1.2.3.4");

        assertEquals(13.1, result.avgSpeedKmh(), "ride/run speed must flow through to the DTO");
        assertEquals(275, result.avgPaceSecondsPerKm(), "pace must flow through to the DTO");
    }
```

- [ ] **Step 2: Test laufen lassen — muss fehlschlagen (Kompilierfehler: `avgSpeedKmh()` existiert nicht)**

Run: `cd backend && mvn -q test -Dtest=SegmentChallengeServiceDedupeTest`
Expected: FAIL — `cannot find symbol method avgSpeedKmh()`.

- [ ] **Step 3: DTO um zwei Felder erweitern**

`SegmentEffortResultDto.java` vollständig ersetzen durch:

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
        Double avgSpeedKmh,
        Integer avgPaceSecondsPerKm,
        String status
) {}
```

- [ ] **Step 4: `buildResult()` die Werte durchreichen**

In `SegmentChallengeService.java`, im `return new SegmentEffortResultDto(...)` (aktuell Zeilen 303-305), die beiden neuen Felder **vor** `saved.getStatus().name()` einfügen:

```java
        return new SegmentEffortResultDto(saved.getId(), saved.getEditToken(), rank, total,
                saved.getElapsedSeconds(), formatElapsed(saved.getElapsedSeconds()), gap,
                Math.round(percentileBeaten * 10) / 10.0,
                saved.getAvgSpeedKmh(), saved.getAvgPaceSecondsPerKm(),
                saved.getStatus().name());
```

- [ ] **Step 5: Test laufen lassen — muss bestehen**

Run: `cd backend && mvn -q test -Dtest=SegmentChallengeServiceDedupeTest`
Expected: PASS (alle 5 Szenarien grün).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/dto/SegmentEffortResultDto.java backend/src/main/java/com/trainingsplan/service/SegmentChallengeService.java backend/src/test/java/com/trainingsplan/service/SegmentChallengeServiceDedupeTest.java
git commit -m "Carry avg speed/pace through the segment effort result DTO"
```

---

## Task 2: Frontend-Model spiegeln

**Files:**
- Modify: `frontend/src/app/models/heartbreak-hill.model.ts:33-43`

- [ ] **Step 1: `EffortResult` um zwei Felder erweitern**

Den `EffortResult`-Block ersetzen durch:

```ts
export interface EffortResult {
  effortId: number;
  editToken: string;
  rank: number;
  totalCount: number;
  elapsedSeconds: number;
  elapsedFormatted: string;
  gapToLeaderSeconds: number | null;
  percentileBeaten: number;
  avgSpeedKmh: number | null;
  avgPaceSecondsPerKm: number | null;
  status: string;
}
```

- [ ] **Step 2: Typecheck**

Run: `cd frontend && npx tsc --noEmit -p tsconfig.app.json`
Expected: keine Fehler.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/models/heartbreak-hill.model.ts
git commit -m "Mirror speed/pace on the EffortResult frontend model"
```

---

## Task 3: `buildElevationPoints` extrahieren (Wiederverwendung)

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.ts`
- Test: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.spec.ts`

- [ ] **Step 1: Failing test schreiben**

In `heartbreak-hill.util.spec.ts` den Import-Header und einen neuen `describe`-Block ergänzen. Importzeile (Zeile 1) ersetzen durch:

```ts
import { buildElevationProfile, buildElevationPoints, formatGap, formatGrade } from './heartbreak-hill.util';
```

Und vor der letzten schließenden `});` der Datei einfügen:

```ts
  describe('buildElevationPoints', () => {
    it('returns an empty array for fewer than 2 points', () => {
      expect(buildElevationPoints([], 400, 200)).toEqual([]);
      expect(buildElevationPoints([[50, 8, 100]], 400, 200)).toEqual([]);
    });

    it('spreads x evenly and inverts elevation (highest → smallest y)', () => {
      const pts: [number, number, number][] = [[50, 8, 100], [50.001, 8, 110], [50.002, 8, 120]];
      const out = buildElevationPoints(pts, 400, 200);
      expect(out.length).toBe(3);
      expect(out[0].x).toBe(0);
      expect(out[2].x).toBe(400);
      // lowest elevation (first) sits lower on screen → larger y than the highest (last)
      expect(out[0].y).toBeGreaterThan(out[2].y);
    });
  });
```

- [ ] **Step 2: Test laufen lassen — muss fehlschlagen**

Run: `cd frontend && npx vitest run src/app/components/heartbreak-hill/heartbreak-hill.util.spec.ts`
Expected: FAIL — `buildElevationPoints is not a function` / Importfehler.

- [ ] **Step 3: `buildElevationPoints` einführen und `buildElevationProfile` darauf zurückführen**

In `heartbreak-hill.util.ts` die `buildElevationProfile`-Funktion (Zeilen 31-54) ersetzen durch:

```ts
export interface ElevationPoint { x: number; y: number; }

/**
 * Normalises [lat, lng, ele] points into a [0..width] x [0..height] box.
 * x is spread evenly by index; y inverts elevation (highest → smallest y) with
 * a small top/bottom padding. Returns [] for fewer than 2 points.
 */
export function buildElevationPoints(
  points: [number, number, number][], width: number, height: number
): ElevationPoint[] {
  if (!points || points.length < 2) {
    return [];
  }
  const eles = points.map(p => p[2] ?? 0);
  const minE = Math.min(...eles);
  const maxE = Math.max(...eles);
  const span = maxE - minE || 1;
  const pad = height * 0.12;
  const usable = height - pad * 2;

  return points.map((p, i) => {
    const x = (i / (points.length - 1)) * width;
    const norm = ((p[2] ?? 0) - minE) / span;       // 0 at lowest, 1 at highest
    const y = pad + (1 - norm) * usable;             // invert: highest → smallest y
    return { x: Math.round(x * 100) / 100, y: Math.round(y * 100) / 100 };
  });
}

/**
 * Builds SVG path strings for a 2D elevation profile, scaled into a
 * [0..width] x [0..height] viewBox. Returns null for < 2 points.
 */
export function buildElevationProfile(
  points: [number, number, number][], width: number, height: number
): ElevationProfile | null {
  const pts = buildElevationPoints(points, width, height);
  if (pts.length < 2) {
    return null;
  }
  const line = pts.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x},${c.y}`).join(' ');
  const area = `${line} L${width},${height} L0,${height} Z`;
  return { line, area };
}
```

(Die bestehende `ElevationProfile`-Interface-Deklaration darüber bleibt unverändert.)

- [ ] **Step 4: Tests laufen lassen — alt + neu müssen bestehen**

Run: `cd frontend && npx vitest run src/app/components/heartbreak-hill/heartbreak-hill.util.spec.ts`
Expected: PASS (inkl. der bestehenden `buildElevationProfile`-Tests — Pfadausgabe ist identisch).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.ts frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.spec.ts
git commit -m "Extract buildElevationPoints for reuse by the share renderer"
```

---

## Task 4: `share-image.util.ts` — reine Helfer (TDD)

**Files:**
- Create: `frontend/src/app/components/heartbreak-hill/share-image.util.ts`
- Test: `frontend/src/app/components/heartbreak-hill/share-image.util.spec.ts`

- [ ] **Step 1: Failing tests schreiben**

`share-image.util.spec.ts` anlegen:

```ts
import { formatTempo, shareFileName } from './share-image.util';

describe('share-image.util', () => {
  describe('formatTempo', () => {
    it('formats ride speed as km/h with one decimal (de comma)', () => {
      expect(formatTempo('RIDE', 23.42, null, 'de-DE')).toBe('23,4 km/h');
    });
    it('formats ride speed with a decimal point in en', () => {
      expect(formatTempo('RIDE', 23.42, null, 'en-GB')).toBe('23.4 km/h');
    });
    it('formats run pace as m:ss /km', () => {
      expect(formatTempo('RUN', null, 275, 'de-DE')).toBe('4:35 /km');
      expect(formatTempo('RUN', null, 309, 'de-DE')).toBe('5:09 /km');
    });
    it('returns an em dash when the relevant value is missing', () => {
      expect(formatTempo('RIDE', null, null, 'de-DE')).toBe('—');
      expect(formatTempo('RUN', 20, null, 'de-DE')).toBe('—');
    });
  });

  describe('shareFileName', () => {
    it('builds a per-activity filename', () => {
      expect(shareFileName(6, 'RIDE')).toBe('heartbreak-hill-rang6-rad.png');
      expect(shareFileName(12, 'RUN')).toBe('heartbreak-hill-rang12-lauf.png');
    });
  });
});
```

- [ ] **Step 2: Test laufen lassen — muss fehlschlagen**

Run: `cd frontend && npx vitest run src/app/components/heartbreak-hill/share-image.util.spec.ts`
Expected: FAIL — Modul/Funktionen existieren nicht.

- [ ] **Step 3: Helfer + Typen implementieren**

`share-image.util.ts` anlegen:

```ts
import { ActivityType } from '../../models/heartbreak-hill.model';

export type ShareTemplate = 'A' | 'B' | 'C';

export const SHARE_W = 1080;
export const SHARE_H = 1920;

export interface ShareImageData {
  segmentName: string;
  activityType: ActivityType;
  elevation: [number, number, number][] | null;
  tempo: string;     // pre-formatted ("23,4 km/h" | "4:35 /km" | "—")
  time: string;      // elapsedFormatted ("4:02")
  rank: number;
  totalCount: number;
}

export interface ShareLabels {
  tempo: string;     // "TEMPO" / "SPEED" / "PACE"
  time: string;      // "ZEIT" / "TIME"
  rank: string;      // "RANG" / "RANK"
  of: string;        // "von" / "of"
}

/** Ride → "23,4 km/h" (locale-aware), Run → "4:35 /km". Missing value → em dash. */
export function formatTempo(
  type: ActivityType,
  avgSpeedKmh: number | null,
  avgPaceSecondsPerKm: number | null,
  locale: string
): string {
  if (type === 'RIDE') {
    if (avgSpeedKmh == null) {
      return '—';
    }
    const n = new Intl.NumberFormat(locale, {
      minimumFractionDigits: 1, maximumFractionDigits: 1
    }).format(avgSpeedKmh);
    return `${n} km/h`;
  }
  if (avgPaceSecondsPerKm == null) {
    return '—';
  }
  const m = Math.floor(avgPaceSecondsPerKm / 60);
  const s = avgPaceSecondsPerKm % 60;
  return `${m}:${s < 10 ? '0' + s : s} /km`;
}

/** e.g. "heartbreak-hill-rang6-rad.png". */
export function shareFileName(rank: number, type: ActivityType): string {
  return `heartbreak-hill-rang${rank}-${type === 'RIDE' ? 'rad' : 'lauf'}.png`;
}
```

- [ ] **Step 4: Test laufen lassen — muss bestehen**

Run: `cd frontend && npx vitest run src/app/components/heartbreak-hill/share-image.util.spec.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/share-image.util.ts frontend/src/app/components/heartbreak-hill/share-image.util.spec.ts
git commit -m "Add share-image formatting helpers (tempo, filename)"
```

---

## Task 5: `share-image.util.ts` — Canvas-Renderer (A/B/C)

Kein Unit-Test (jsdom hat kein echtes 2D-Canvas) → Typecheck jetzt, Sichtprüfung in Task 9.

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/share-image.util.ts`

- [ ] **Step 1: Import ergänzen**

Oben in `share-image.util.ts`, direkt unter dem bestehenden Import, einfügen:

```ts
import { buildElevationPoints } from './heartbreak-hill.util';
```

- [ ] **Step 2: Renderer-Code ans Dateiende anhängen**

Folgenden Block ans Ende von `share-image.util.ts` anhängen:

```ts
const GREEN = '#8ffc2e';
const SYNTHETIC_ELE = [
  100, 103, 109, 112, 110, 118, 126, 130, 128, 138,
  150, 158, 166, 170, 176, 184, 190, 198, 206, 214
];

/** Loads an <img> from a same-origin asset URL (no canvas tainting). */
export function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

function fontFamily(): string {
  const v = getComputedStyle(document.documentElement)
    .getPropertyValue('--font-family').trim();
  return v || 'system-ui, sans-serif';
}

function syntheticPoints(): [number, number, number][] {
  return SYNTHETIC_ELE.map((e, i) => [0, i, e]);
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number): void {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

function shadowText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number): void {
  ctx.save();
  ctx.shadowColor = 'rgba(0,0,0,0.55)';
  ctx.shadowBlur = 10;
  ctx.shadowOffsetY = 2;
  ctx.fillText(text, x, y);
  ctx.restore();
}

function textWidth(ctx: CanvasRenderingContext2D, text: string, font: string): number {
  ctx.font = font;
  return ctx.measureText(text).width;
}

function drawLogoTag(ctx: CanvasRenderingContext2D, logo: HTMLImageElement | null, anchorX: number, baseY: number): void {
  const fam = fontFamily();
  const h = 64, padX = 24, logoH = 32;
  const logoW = logo ? (logo.width / logo.height) * logoH : 0;
  const contentW = logo ? logoW : textWidth(ctx, 'PACR', `500 32px ${fam}`);
  const pillW = contentW + padX * 2;
  const px = anchorX - pillW;            // right-anchored
  const py = baseY - h;
  ctx.fillStyle = 'rgba(8,13,9,0.45)';
  roundRect(ctx, px, py, pillW, h, 16);
  ctx.fill();
  if (logo) {
    ctx.drawImage(logo, px + padX, py + (h - logoH) / 2, logoW, logoH);
  } else {
    ctx.fillStyle = GREEN;
    ctx.font = `500 32px ${fam}`;
    ctx.textAlign = 'left';
    ctx.textBaseline = 'middle';
    ctx.fillText('PACR', px + padX, py + h / 2 + 1);
    ctx.textBaseline = 'alphabetic';
  }
}

interface Col { label: string; value: string; sub?: string; green: boolean; }

function columns(data: ShareImageData, labels: ShareLabels): Col[] {
  return [
    { label: labels.tempo, value: data.tempo, green: false },
    { label: labels.time, value: data.time, green: false },
    { label: labels.rank, value: `#${data.rank}`, sub: `${labels.of} ${data.totalCount}`, green: true }
  ];
}

function strokeRidge(ctx: CanvasRenderingContext2D, pts: { x: number; y: number }[], offsetX: number, offsetY: number): void {
  ctx.beginPath();
  pts.forEach((p, i) => (i ? ctx.lineTo(offsetX + p.x, offsetY + p.y) : ctx.moveTo(offsetX + p.x, offsetY + p.y)));
  ctx.stroke();
}

/** Renders the chosen template onto a transparent 1080×1920 context. */
export function drawShareImage(
  ctx: CanvasRenderingContext2D,
  data: ShareImageData,
  template: ShareTemplate,
  labels: ShareLabels,
  logo: HTMLImageElement | null
): void {
  ctx.clearRect(0, 0, SHARE_W, SHARE_H);
  ctx.lineJoin = 'round';
  ctx.lineCap = 'round';
  const ele = data.elevation && data.elevation.length >= 2 ? data.elevation : syntheticPoints();
  if (template === 'A') {
    drawTemplateA(ctx, data, labels, logo, ele);
  } else if (template === 'B') {
    drawTemplateB(ctx, data, labels, logo, ele);
  } else {
    drawTemplateC(ctx, data, labels, logo, ele);
  }
}

function drawTemplateA(ctx: CanvasRenderingContext2D, data: ShareImageData, labels: ShareLabels, logo: HTMLImageElement | null, ele: [number, number, number][]): void {
  const fam = fontFamily();
  const rh = Math.round(SHARE_H * 0.40);
  const ry = SHARE_H - rh;
  const pts = buildElevationPoints(ele, SHARE_W, rh);

  ctx.beginPath();
  ctx.moveTo(0, ry + pts[0].y);
  pts.forEach(p => ctx.lineTo(p.x, ry + p.y));
  ctx.lineTo(SHARE_W, SHARE_H);
  ctx.lineTo(0, SHARE_H);
  ctx.closePath();
  ctx.fillStyle = 'rgba(143,252,46,0.82)';
  ctx.fill();

  ctx.strokeStyle = '#d7ff9e';
  ctx.lineWidth = 7;
  strokeRidge(ctx, pts, 0, ry);

  ctx.textBaseline = 'alphabetic';
  ctx.textAlign = 'left';
  ctx.fillStyle = 'rgba(255,255,255,0.92)';
  ctx.font = `500 32px ${fam}`;
  shadowText(ctx, data.segmentName.toUpperCase(), 60, 132);

  const cols = columns(data, labels);
  const colW = SHARE_W / 3;
  const baseY = ry - 80;
  ctx.textAlign = 'center';
  cols.forEach((c, i) => {
    const cx = colW * i + colW / 2;
    ctx.font = `500 28px ${fam}`;
    ctx.fillStyle = '#bdefae';
    ctx.fillText(c.label.toUpperCase(), cx, baseY);
    ctx.font = `800 66px ${fam}`;
    ctx.fillStyle = c.green ? GREEN : '#ffffff';
    shadowText(ctx, c.value, cx, baseY + 72);
    if (c.sub) {
      ctx.font = `500 30px ${fam}`;
      ctx.fillStyle = 'rgba(255,255,255,0.8)';
      ctx.fillText(c.sub, cx, baseY + 116);
    }
  });

  drawLogoTag(ctx, logo, SHARE_W - 60, SHARE_H - 64);
}

function drawTemplateB(ctx: CanvasRenderingContext2D, data: ShareImageData, labels: ShareLabels, logo: HTMLImageElement | null, ele: [number, number, number][]): void {
  const fam = fontFamily();
  const cardX = 48, cardW = SHARE_W - 96, cardH = 440;
  const cardY = SHARE_H - 60 - cardH;
  const padX = 44;

  ctx.fillStyle = 'rgba(11,15,20,0.58)';
  roundRect(ctx, cardX, cardY, cardW, cardH, 28);
  ctx.fill();

  ctx.textBaseline = 'alphabetic';
  ctx.textAlign = 'left';
  ctx.fillStyle = '#cfe9c2';
  ctx.font = `500 28px ${fam}`;
  ctx.fillText(data.segmentName.toUpperCase(), cardX + padX, cardY + 74);
  drawLogoTag(ctx, logo, cardX + cardW - padX, cardY + 86);

  const rh = 150;
  const pts = buildElevationPoints(ele, cardW - padX * 2, rh);
  ctx.strokeStyle = GREEN;
  ctx.lineWidth = 6;
  strokeRidge(ctx, pts, cardX + padX, cardY + 110);

  const cols = columns(data, labels);
  const colW = cardW / 3;
  const rowY = cardY + cardH - 120;
  ctx.textAlign = 'center';
  cols.forEach((c, i) => {
    const cx = cardX + colW * i + colW / 2;
    ctx.font = `500 26px ${fam}`;
    ctx.fillStyle = '#bdefae';
    ctx.fillText(c.label.toUpperCase(), cx, rowY);
    ctx.font = `800 60px ${fam}`;
    ctx.fillStyle = c.green ? GREEN : '#ffffff';
    ctx.fillText(c.value, cx, rowY + 66);
    if (c.sub) {
      ctx.font = `500 28px ${fam}`;
      ctx.fillStyle = 'rgba(255,255,255,0.75)';
      ctx.fillText(c.sub, cx, rowY + 106);
    }
  });
}

function drawTemplateC(ctx: CanvasRenderingContext2D, data: ShareImageData, labels: ShareLabels, logo: HTMLImageElement | null, ele: [number, number, number][]): void {
  const fam = fontFamily();
  const rh = Math.round(SHARE_H * 0.34);
  const ry = Math.round(SHARE_H * 0.46);
  const pts = buildElevationPoints(ele, SHARE_W, rh);

  ctx.save();
  ctx.shadowColor = 'rgba(0,0,0,0.6)';
  ctx.shadowBlur = 8;
  ctx.shadowOffsetY = 2;
  ctx.strokeStyle = GREEN;
  ctx.lineWidth = 6;
  strokeRidge(ctx, pts, 0, ry);
  ctx.restore();

  ctx.textBaseline = 'alphabetic';
  ctx.textAlign = 'left';
  ctx.fillStyle = 'rgba(255,255,255,0.92)';
  ctx.font = `500 32px ${fam}`;
  shadowText(ctx, data.segmentName.toUpperCase(), 60, 132);

  const rows = [
    { value: data.tempo, label: labels.tempo, green: false },
    { value: data.time, label: labels.time, green: false },
    { value: `#${data.rank}`, label: `${labels.of} ${data.totalCount}`, green: true }
  ];
  let y = SHARE_H - 250;
  rows.forEach(r => {
    const valueFont = `800 76px ${fam}`;
    ctx.font = valueFont;
    ctx.fillStyle = r.green ? GREEN : '#ffffff';
    shadowText(ctx, r.value, 60, y);
    ctx.font = `500 28px ${fam}`;
    ctx.fillStyle = '#bdefae';
    shadowText(ctx, r.label.toUpperCase(), 60 + textWidth(ctx, r.value, valueFont) + 20, y);
    y += 88;
  });

  drawLogoTag(ctx, logo, SHARE_W - 60, SHARE_H - 64);
}
```

- [ ] **Step 3: Typecheck**

Run: `cd frontend && npx tsc --noEmit -p tsconfig.app.json`
Expected: keine Fehler.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/share-image.util.ts
git commit -m "Add transparent canvas renderer with three share templates"
```

---

## Task 6: Komponente verdrahten

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts`

- [ ] **Step 1: Imports + TranslateService**

Importzeile für den Service ergänzen (oben bei den `@ngx-translate`-Imports):

```ts
import { TranslateModule, TranslateService } from '@ngx-translate/core';
```

(Die bestehende `import { TranslateModule } from '@ngx-translate/core';` dadurch ersetzen.)

Direkt darunter die Share-Util importieren:

```ts
import {
  SHARE_W, SHARE_H, ShareTemplate, ShareImageData, ShareLabels,
  formatTempo, shareFileName, drawShareImage, loadImage
} from './share-image.util';
```

- [ ] **Step 2: Service injizieren**

Bei den bestehenden `inject(...)`-Feldern ergänzen:

```ts
  private readonly translate = inject(TranslateService);
```

- [ ] **Step 3: Share-State + Methoden einfügen**

Vor der schließenden `}` der Klasse `HeartbreakHill` einfügen:

```ts
  // --- share image ---
  readonly shareTemplate = signal<ShareTemplate>('A');
  readonly sharePreviewUrl = signal<string | null>(null);
  readonly canNativeShare = signal<boolean>(
    typeof navigator !== 'undefined' && 'canShare' in navigator);

  private shareCanvas: HTMLCanvasElement | null = null;
  private logoPromise: Promise<HTMLImageElement | null> | null = null;

  private locale(): string {
    return this.translate.currentLang === 'en' ? 'en-GB' : 'de-DE';
  }

  selectTemplate(t: ShareTemplate): void {
    if (this.shareTemplate() === t) {
      return;
    }
    this.shareTemplate.set(t);
    void this.renderShare();
  }

  /** Builds the off-screen 1080×1920 canvas and publishes a preview data URL. */
  private async renderShare(): Promise<void> {
    const r = this.result();
    if (!r) {
      return;
    }
    await document.fonts.ready;
    if (!this.logoPromise) {
      this.logoPromise = loadImage('assets/logo/PACR_logo_light_text_transparent.png')
        .catch(() => null);
    }
    const logo = await this.logoPromise;

    const canvas = document.createElement('canvas');
    canvas.width = SHARE_W;
    canvas.height = SHARE_H;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }

    const type = this.activeTab();
    const data: ShareImageData = {
      segmentName: this.challenge()?.name ?? 'Heartbreak Hill',
      activityType: type,
      elevation: this.polylinePoints(),
      tempo: formatTempo(type, r.avgSpeedKmh, r.avgPaceSecondsPerKm, this.locale()),
      time: r.elapsedFormatted,
      rank: r.rank,
      totalCount: r.totalCount
    };
    const labels: ShareLabels = {
      tempo: this.translate.instant(
        type === 'RIDE' ? 'HEARTBREAK_HILL.SHARE_LBL_TEMPO' : 'HEARTBREAK_HILL.SHARE_LBL_PACE'),
      time: this.translate.instant('HEARTBREAK_HILL.SHARE_LBL_TIME'),
      rank: this.translate.instant('HEARTBREAK_HILL.SHARE_LBL_RANK'),
      of: this.translate.instant('HEARTBREAK_HILL.SHARE_LBL_OF')
    };

    drawShareImage(ctx, data, this.shareTemplate(), labels, logo);
    this.shareCanvas = canvas;
    this.sharePreviewUrl.set(canvas.toDataURL('image/png'));
  }

  downloadShare(): void {
    const canvas = this.shareCanvas;
    const r = this.result();
    if (!canvas || !r) {
      return;
    }
    canvas.toBlob(blob => {
      if (!blob) {
        return;
      }
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = shareFileName(r.rank, this.activeTab());
      a.click();
      URL.revokeObjectURL(url);
    }, 'image/png');
  }

  shareImage(): void {
    const canvas = this.shareCanvas;
    const r = this.result();
    if (!canvas || !r) {
      return;
    }
    canvas.toBlob(blob => {
      if (!blob) {
        return;
      }
      const file = new File([blob], shareFileName(r.rank, this.activeTab()), { type: 'image/png' });
      const nav = navigator as Navigator & {
        canShare?: (d: { files: File[] }) => boolean;
        share?: (d: { files: File[] }) => Promise<void>;
      };
      if (nav.canShare?.({ files: [file] }) && nav.share) {
        nav.share({ files: [file] }).catch(() => { /* user cancelled */ });
      }
    }, 'image/png');
  }
```

- [ ] **Step 4: Nach erfolgreichem Upload rendern**

In `submit()`, im `next:`-Callback nach `this.result.set(res);` ergänzen:

```ts
        this.shareTemplate.set('A');
        void this.renderShare();
```

- [ ] **Step 5: Beim Tabwechsel die alte Vorschau verwerfen**

In `selectTab()`, nach `this.result.set(null);` ergänzen:

```ts
    this.sharePreviewUrl.set(null);
```

- [ ] **Step 6: Typecheck**

Run: `cd frontend && npx tsc --noEmit -p tsconfig.app.json`
Expected: keine Fehler.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts
git commit -m "Wire share-image render, download and native share into the component"
```

---

## Task 7: Share-Block in HTML + SCSS

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html:137`
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss`

- [ ] **Step 1: Markup einfügen**

In `heartbreak-hill.html`, innerhalb von `<div class="panel result">`, **nach** dem schließenden `</div>` des `.meta`-Blocks (aktuell Zeile 137) und **vor** `<div class="funnel">`, einfügen:

```html
            <div class="share">
              <span class="label">{{ 'HEARTBREAK_HILL.SHARE_TITLE' | translate }}</span>
              <p class="share-hint">{{ 'HEARTBREAK_HILL.SHARE_HINT' | translate }}</p>

              <div class="tpl-seg">
                <span [class.on]="shareTemplate() === 'A'" (click)="selectTemplate('A')">{{ 'HEARTBREAK_HILL.SHARE_TPL_A' | translate }}</span>
                <span [class.on]="shareTemplate() === 'B'" (click)="selectTemplate('B')">{{ 'HEARTBREAK_HILL.SHARE_TPL_B' | translate }}</span>
                <span [class.on]="shareTemplate() === 'C'" (click)="selectTemplate('C')">{{ 'HEARTBREAK_HILL.SHARE_TPL_C' | translate }}</span>
              </div>

              @if (sharePreviewUrl(); as url) {
                <div class="share-preview"><img [src]="url" alt=""></div>
              }

              <div class="share-actions">
                <button class="btn block" (click)="downloadShare()">
                  <span class="material-symbols-outlined">download</span>{{ 'HEARTBREAK_HILL.SHARE_DOWNLOAD' | translate }}
                </button>
                @if (canNativeShare()) {
                  <button class="btn ghost block" (click)="shareImage()">
                    <span class="material-symbols-outlined">ios_share</span>{{ 'HEARTBREAK_HILL.SHARE_SHARE' | translate }}
                  </button>
                }
              </div>
            </div>
```

- [ ] **Step 2: Styles anhängen**

Ans Ende von `heartbreak-hill.scss` anhängen:

```scss
/* SHARE IMAGE */
.share { margin-top: 18px; padding-top: 18px; border-top: 1px solid var(--border); }
.share-hint { color: var(--text-muted); font-size: 13px; margin: 6px 0 12px; line-height: 1.5; }
.tpl-seg { display: flex; gap: 8px; padding: 6px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; }
.tpl-seg span { flex: 1; text-align: center; padding: 8px; border-radius: 8px; cursor: pointer; color: var(--text-muted); font-weight: 700; font-size: 13px; }
.tpl-seg span.on { background: var(--pp); color: #07120a; }
.share-preview { margin: 14px auto 0; width: 220px; aspect-ratio: 9 / 16; border-radius: 14px; overflow: hidden; border: 1px solid var(--border);
  background-color: #2a2f37;
  background-image:
    linear-gradient(45deg, #3a3f47 25%, transparent 25%, transparent 75%, #3a3f47 75%),
    linear-gradient(45deg, #3a3f47 25%, transparent 25%, transparent 75%, #3a3f47 75%);
  background-size: 22px 22px; background-position: 0 0, 11px 11px; }
.share-preview img { width: 100%; height: 100%; object-fit: contain; display: block; }
.share-actions { display: flex; gap: 10px; margin-top: 14px; }
.share-actions .btn.block { margin-top: 0; }
@media (max-width: 520px) { .share-actions { flex-direction: column; } }
```

(Der Karo-Hintergrund hinter der Vorschau signalisiert die Transparenz der PNG.)

- [ ] **Step 3: Build (Template-Check)**

Run: `cd frontend && npm run build`
Expected: Build erfolgreich (kein Template-Fehler, alle Bindings vorhanden).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.html frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss
git commit -m "Add share-image block (template switcher, preview, actions)"
```

---

## Task 8: i18n-Keys (de + en)

**Files:**
- Modify: `frontend/src/assets/i18n/de.json:2522`
- Modify: `frontend/src/assets/i18n/en.json:2522`

- [ ] **Step 1: Deutsche Keys**

In `de.json` die Zeile `"TYPE_RUNNERS": "Läufern"` (Zeile 2522) ersetzen durch (Komma anhängen + neue Keys):

```json
    "TYPE_RUNNERS": "Läufern",
    "SHARE_TITLE": "Story-Bild",
    "SHARE_HINT": "Lade dir ein transparentes Bild für deine Instagram-Story – leg einfach dein Foto dahinter.",
    "SHARE_DOWNLOAD": "Bild herunterladen",
    "SHARE_SHARE": "Teilen",
    "SHARE_TPL_A": "Kurve",
    "SHARE_TPL_B": "Karte",
    "SHARE_TPL_C": "Minimal",
    "SHARE_LBL_TEMPO": "Tempo",
    "SHARE_LBL_PACE": "Pace",
    "SHARE_LBL_TIME": "Zeit",
    "SHARE_LBL_RANK": "Rang",
    "SHARE_LBL_OF": "von"
```

- [ ] **Step 2: Englische Keys**

In `en.json` die Zeile `"TYPE_RUNNERS": "runners"` (Zeile 2522) ersetzen durch:

```json
    "TYPE_RUNNERS": "runners",
    "SHARE_TITLE": "Story image",
    "SHARE_HINT": "Download a transparent image for your Instagram story – just place your photo behind it.",
    "SHARE_DOWNLOAD": "Download image",
    "SHARE_SHARE": "Share",
    "SHARE_TPL_A": "Curve",
    "SHARE_TPL_B": "Card",
    "SHARE_TPL_C": "Minimal",
    "SHARE_LBL_TEMPO": "Speed",
    "SHARE_LBL_PACE": "Pace",
    "SHARE_LBL_TIME": "Time",
    "SHARE_LBL_RANK": "Rank",
    "SHARE_LBL_OF": "of"
```

- [ ] **Step 3: JSON-Validität prüfen**

Run: `cd frontend && node -e "require('./src/assets/i18n/de.json');require('./src/assets/i18n/en.json');console.log('json ok')"`
Expected: `json ok`.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/assets/i18n/de.json frontend/src/assets/i18n/en.json
git commit -m "Add i18n keys for the share-image block (de + en)"
```

---

## Task 9: Sichtprüfung, Version-Bump, Changelog

**Files:**
- Modify: `pom.xml`, `frontend/package.json` (via Script), `CHANGELOG.md`

- [ ] **Step 1: App starten und manuell prüfen**

Backend: `cd backend && mvn spring-boot:run` — Frontend: `cd frontend && npm start` → `http://localhost:4200/heartbreak-hill`.
Lade eine Test-GPX hoch (z. B. `backend/src/test/resources/gpx/heartbreak-real-run.gpx`). Im Result-Panel prüfen:
- Vorschau erscheint, Umschalten A/B/C ändert das Bild sofort.
- „Bild herunterladen" liefert ein PNG; in einem Bildbetrachter mit Karo-/Transparenzhintergrund ist **alles außer Kurve, Werten und Logo durchsichtig**.
- Werte stimmen: Rad → km/h, Lauf → min/km; Zeit und Rang wie im Panel.
- Logo-Tag sichtbar und lesbar.

Expected: Alle drei Vorlagen rendern korrekt für Rad **und** Lauf.

- [ ] **Step 2: Version bumpen (minor)**

Run: `bash version-bump.sh minor`
Expected: `pom.xml` und `frontend/package.json` auf die nächste Minor-Version gesetzt.

- [ ] **Step 3: Changelog ergänzen**

In `CHANGELOG.md` unter `## [Unreleased]` → `### Added` als Bullet einfügen:

```markdown
- Heartbreak Hill: shareable transparent 9:16 story image (elevation curve, speed/time/rank, PACR logo) with three user-selectable templates, download and native share.
```

- [ ] **Step 4: Abschluss-Commit**

```bash
git add pom.xml frontend/package.json CHANGELOG.md
git commit -m "Heartbreak Hill share image: bump minor version and update changelog"
```

---

## Self-Review (vom Plan-Autor durchgeführt)

**Spec-Abdeckung:** 9:16/transparent (Task 5 `clearRect`, kein Hintergrund-Fill ✓); drei Vorlagen + Switcher (Task 5/6/7 ✓); Tempo/Zeit/Rang (Task 1/4/5 ✓); Höhenkurve mit Fallback (Task 3/5 `syntheticPoints` ✓); PACR-Logo (Task 5 `drawLogoTag` ✓); Download + Web-Share (Task 6 ✓); Vorschau = Ergebnis (Task 6 eine Canvas ✓); Backend-Durchreichung (Task 1 ✓); i18n (Task 8 ✓); Tests (Task 1/3/4 ✓); Version/Changelog (Task 9 ✓). Keine offene Spec-Anforderung.

**Platzhalter-Scan:** keine TODO/„später" — jeder Code-Step zeigt vollständigen Code.

**Typkonsistenz:** `ShareTemplate`/`ShareImageData`/`ShareLabels`/`SHARE_W`/`SHARE_H`/`formatTempo`/`shareFileName`/`drawShareImage`/`loadImage` in Task 4/5 definiert und in Task 6 exakt so konsumiert. `buildElevationPoints` (Task 3) → genutzt in Task 5. DTO-Felder `avgSpeedKmh`/`avgPaceSecondsPerKm` (Task 1) ↔ Model (Task 2) ↔ `formatTempo`-Aufruf (Task 6) konsistent. i18n-Keys (Task 8) decken alle `translate`-Aufrufe (Task 6/7) ab.
