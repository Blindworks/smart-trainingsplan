# Heartbreak Hill Challenge — Public 2D Page (Plan 2/3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the public, no-login Angular page at `/heartbreak-hill` — a cinematic 2D landing page for the Heartbreak Hill Challenge: hero with a 2D elevation profile + segment stats, Rad/Lauf leaderboard (reference roster + public uploads), anonymous GPX upload with an instant result reveal, and the PACR sign-up funnel. (Real WebGL 3D is Plan 3; this page is fully shippable on its own with a 2D SVG profile.)

**Architecture:** One standalone Angular component (`HeartbreakHill`) on a public route (no `authGuard`, `data: { fullPage: true }`), backed by a dedicated `HeartbreakHillService` that calls the `/api/public/challenges/heartbreak-hill-2026/**` endpoints from Plan 1. Pure presentation/formatting logic lives in a testable `heartbreak-hill.util.ts`. The page degrades gracefully when the challenge has no polyline yet and when the leaderboard is empty.

**Tech Stack:** Angular 21 (standalone, signals, `@if`/`@for`), TypeScript, RxJS, ngx-translate, SCSS with the existing dark design tokens, Leaflet via the existing `RouteMiniMapComponent`. Tests: **Vitest** (the project's actual runner — `tsconfig.spec.json` uses `vitest/globals`; NOT Karma/Jasmine) with Angular `TestBed` + `provideHttpClient()`/`provideHttpClientTesting()`. Use Vitest matchers (`toBe(true)`, not Jasmine's `toBeTrue()`); mirror an existing `*.service.spec.ts` in the repo for the canonical HTTP-testing setup.

**Key conventions (from the existing codebase):**
- File naming: `components/heartbreak-hill/heartbreak-hill.ts` exporting `class HeartbreakHill` (no `.component` suffix). Template `heartbreak-hill.html`, styles `heartbreak-hill.scss`.
- DI via `inject()`, services `@Injectable({ providedIn: 'root' })`, `private readonly http = inject(HttpClient)`.
- URLs via `apiUrl('/path')` from `frontend/src/app/core/api-base.ts` (base `/api`). No `environment.ts`, no monolithic ApiService.
- Multipart upload: `FormData` with NO explicit `Content-Type`.
- Signals + `@if`/`@for` control flow (mirror `components/news-hub/news-hub.ts`).
- i18n: nested keys, PascalCase namespace + SCREAMING_SNAKE leaves; `{{ 'HEARTBREAK_HILL.X' | translate }}`; add to BOTH `en.json` and `de.json`.
- Tokens (dark): `--pp` `#8ffc2e`, `--pp-glow`, `--pp-container`, `--bg` `#0d1117`, `--bg-card`, `--surface-high`, `--text`, `--text-muted`, `--border`, `--overlay`, `--font-family`.

**All frontend commands run from `frontend/`:** `npm run build` (prod build = the main compile gate), `npm test` (Vitest; one-shot via `npx vitest run <filter>` or `npm test -- --run`, runs in node/jsdom — no browser/Chrome needed). The Karma-style flags `--browsers=ChromeHeadless` / `--include` do NOT apply.

---

## File Structure

**New files**
- `frontend/src/app/models/heartbreak-hill.model.ts` — TS interfaces for the API payloads.
- `frontend/src/app/services/heartbreak-hill.service.ts` — HTTP calls to `/api/public/challenges/...`.
- `frontend/src/app/services/heartbreak-hill.service.spec.ts` — service unit test.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.ts` — pure formatting/SVG helpers.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.spec.ts` — helper unit test.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts` — the standalone component.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html` — template.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss` — styles.

**Modified files**
- `frontend/src/app/app.routes.ts` — add the public route.
- `frontend/src/assets/i18n/en.json` and `frontend/src/assets/i18n/de.json` — add `HEARTBREAK_HILL` namespace.
- `CHANGELOG.md` — extend the `[Unreleased]` entry.

---

## Task 1: Models

**Files:**
- Create: `frontend/src/app/models/heartbreak-hill.model.ts`

These mirror the backend DTOs (`SegmentChallengeDto`, `SegmentLeaderboardEntryDto`, `SegmentEffortResultDto`, `SegmentTrackDto`).

- [ ] **Step 1: Create the model file**

```ts
export type ActivityType = 'RIDE' | 'RUN';

export interface SegmentChallenge {
  id: number;
  slug: string;
  name: string;
  subtitle: string | null;
  eventDate: string | null;       // ISO date
  distanceM: number | null;
  elevationGainM: number | null;
  avgGradePct: number | null;
  maxGradePct: number | null;
  polylineJson: string | null;    // JSON: [[lat,lng,ele], ...]
  terrainAssetRef: string | null;
  rideCount: number;
  runCount: number;
}

export interface LeaderboardEntry {
  effortId: number;
  rank: number;
  displayName: string;
  kind: 'REFERENCE' | 'PUBLIC';
  category: string | null;
  elapsedSeconds: number;
  elapsedFormatted: string;
  gapToLeaderSeconds: number | null;
  avgSpeedKmh: number | null;
  avgPaceSecondsPerKm: number | null;
  reference: boolean;
}

export interface EffortResult {
  effortId: number;
  editToken: string;
  rank: number;
  totalCount: number;
  elapsedSeconds: number;
  elapsedFormatted: string;
  gapToLeaderSeconds: number | null;
  percentileBeaten: number;
  status: string;
}

export interface EffortTrack {
  effortId: number;
  activityType: ActivityType;
  trackJson: string;              // JSON: [[lat,lng,ele,relSec], ...]
}
```

- [ ] **Step 2: Verify it compiles as part of the build later (no standalone check needed).** Commit.

```bash
git add frontend/src/app/models/heartbreak-hill.model.ts
git commit -m "Add Heartbreak Hill frontend models"
```

---

## Task 2: HeartbreakHillService (TDD)

**Files:**
- Create: `frontend/src/app/services/heartbreak-hill.service.ts`
- Test: `frontend/src/app/services/heartbreak-hill.service.spec.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HeartbreakHillService } from './heartbreak-hill.service';

describe('HeartbreakHillService', () => {
  let service: HeartbreakHillService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(HeartbreakHillService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getChallenge GETs the challenge by slug', () => {
    service.getChallenge().subscribe();
    const req = httpMock.expectOne(r =>
      r.method === 'GET' && r.url.endsWith('/public/challenges/heartbreak-hill-2026'));
    expect(req.request.method).toBe('GET');
    req.flush({ slug: 'heartbreak-hill-2026', rideCount: 0, runCount: 0 });
  });

  it('getLeaderboard passes the activity type as a query param', () => {
    service.getLeaderboard('RUN').subscribe();
    const req = httpMock.expectOne(r => r.url.endsWith('/leaderboard') && r.params.get('type') === 'RUN');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('submitEffort POSTs multipart form data', () => {
    const file = new File(['<gpx/>'], 'ride.gpx', { type: 'application/gpx+xml' });
    service.submitEffort('RIDE', 'Lukas', file).subscribe();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/efforts'));
    const body = req.request.body as FormData;
    expect(body.get('displayName')).toBe('Lukas');
    expect(body.get('type')).toBe('RIDE');
    expect(body.get('file')).toBeTruthy();
    req.flush({ effortId: 1, rank: 1 });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- --watch=false --browsers=ChromeHeadless --include='**/heartbreak-hill.service.spec.ts'`
Expected: FAIL — `HeartbreakHillService` does not exist.

> If the `--include` flag is not supported by this project's Karma/test setup, run `npm test -- --watch=false --browsers=ChromeHeadless` and locate the suite in the output.

- [ ] **Step 3: Implement the service**

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';
import {
  ActivityType, SegmentChallenge, LeaderboardEntry, EffortResult, EffortTrack
} from '../models/heartbreak-hill.model';

const SLUG = 'heartbreak-hill-2026';
const BASE = apiUrl(`/public/challenges/${SLUG}`);

@Injectable({ providedIn: 'root' })
export class HeartbreakHillService {
  private readonly http = inject(HttpClient);

  getChallenge(): Observable<SegmentChallenge> {
    return this.http.get<SegmentChallenge>(BASE);
  }

  getLeaderboard(type: ActivityType): Observable<LeaderboardEntry[]> {
    return this.http.get<LeaderboardEntry[]>(`${BASE}/leaderboard`,
      { params: new HttpParams().set('type', type) });
  }

  submitEffort(type: ActivityType, displayName: string, file: File): Observable<EffortResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('displayName', displayName);
    form.append('type', type);
    return this.http.post<EffortResult>(`${BASE}/efforts`, form);
  }

  getTrack(effortId: number): Observable<EffortTrack> {
    return this.http.get<EffortTrack>(`${BASE}/efforts/${effortId}/track`);
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- --watch=false --browsers=ChromeHeadless --include='**/heartbreak-hill.service.spec.ts'`
Expected: PASS (3 specs).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/services/heartbreak-hill.service.ts frontend/src/app/services/heartbreak-hill.service.spec.ts
git commit -m "Add Heartbreak Hill API service"
```

---

## Task 3: Pure helpers (TDD)

**Files:**
- Create: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.ts`
- Test: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.spec.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { buildElevationProfile, formatGap, formatGrade } from './heartbreak-hill.util';

describe('heartbreak-hill.util', () => {
  describe('formatGap', () => {
    it('formats a positive gap with a leading +', () => {
      expect(formatGap(46)).toBe('+0:46');
      expect(formatGap(135)).toBe('+2:15');
    });
    it('renders the leader gap (0) as a dash', () => {
      expect(formatGap(0)).toBe('—');
    });
    it('handles null as a dash', () => {
      expect(formatGap(null)).toBe('—');
    });
  });

  describe('formatGrade', () => {
    it('formats a percentage with one decimal and a percent sign', () => {
      expect(formatGrade(6.4)).toBe('6,4 %');
    });
    it('returns an em dash for null', () => {
      expect(formatGrade(null)).toBe('—');
    });
  });

  describe('buildElevationProfile', () => {
    it('returns null when there are fewer than 2 points', () => {
      expect(buildElevationProfile([], 400, 200)).toBeNull();
      expect(buildElevationProfile([[50, 8, 100]], 400, 200)).toBeNull();
    });

    it('maps points to an SVG line + area path scaled to the viewBox', () => {
      const pts: [number, number, number][] = [[50, 8, 100], [50.001, 8, 110], [50.002, 8, 120]];
      const profile = buildElevationProfile(pts, 400, 200);
      expect(profile).not.toBeNull();
      // x spans 0..400 across the 3 points
      expect(profile!.line.startsWith('M0,')).toBeTrue();
      expect(profile!.line).toContain('400,');
      // lowest elevation maps to the bottom (larger y), highest to the top (smaller y)
      // first point is the lowest → its y must be greater than the last point's y
      const firstY = Number(profile!.line.split(' ')[0].split(',')[1]);
      const lastSeg = profile!.line.trim().split(/[ ]/).pop()!;
      const lastY = Number(lastSeg.split(',')[1]);
      expect(firstY).toBeGreaterThan(lastY);
      // area path closes back to the baseline
      expect(profile!.area.endsWith('Z')).toBeTrue();
    });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- --watch=false --browsers=ChromeHeadless --include='**/heartbreak-hill.util.spec.ts'`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Implement the helpers**

```ts
/** Formats a gap (seconds behind the leader) as "+m:ss"; 0 or null → em dash. */
export function formatGap(seconds: number | null): string {
  if (seconds === null || seconds === 0) {
    return '—';
  }
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `+${m}:${s < 10 ? '0' + s : s}`;
}

/** Formats a grade percentage German-style ("6,4 %"); null → em dash. */
export function formatGrade(pct: number | null): string {
  if (pct === null || pct === undefined) {
    return '—';
  }
  return `${pct.toFixed(1).replace('.', ',')} %`;
}

export interface ElevationProfile {
  line: string;   // SVG path for the ridge line
  area: string;   // SVG path for the filled area under the ridge
}

/**
 * Builds SVG path strings for a 2D elevation profile from [lat, lng, ele] points,
 * scaled into a [0..width] x [0..height] viewBox. Returns null for < 2 points.
 * x is distributed evenly by index; y is the elevation normalised and inverted
 * (highest elevation near the top, y=0). A small top/bottom padding keeps the
 * line off the edges.
 */
export function buildElevationProfile(
  points: [number, number, number][], width: number, height: number
): ElevationProfile | null {
  if (!points || points.length < 2) {
    return null;
  }
  const eles = points.map(p => p[2] ?? 0);
  const minE = Math.min(...eles);
  const maxE = Math.max(...eles);
  const span = maxE - minE || 1;
  const pad = height * 0.12;
  const usable = height - pad * 2;

  const coords = points.map((p, i) => {
    const x = (i / (points.length - 1)) * width;
    const norm = ((p[2] ?? 0) - minE) / span;       // 0 at lowest, 1 at highest
    const y = pad + (1 - norm) * usable;             // invert: highest → smallest y
    return [Math.round(x * 100) / 100, Math.round(y * 100) / 100] as const;
  });

  const line = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c[0]},${c[1]}`).join(' ');
  const area = `${line} L${width},${height} L0,${height} Z`;
  return { line, area };
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- --watch=false --browsers=ChromeHeadless --include='**/heartbreak-hill.util.spec.ts'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.ts frontend/src/app/components/heartbreak-hill/heartbreak-hill.util.spec.ts
git commit -m "Add Heartbreak Hill presentation helpers"
```

---

## Task 4: Public route registration

**Files:**
- Modify: `frontend/src/app/app.routes.ts`

- [ ] **Step 1: Add the route**

Add this entry to the routes array **before** the empty-path (`path: ''`) catch-all / `LandingRedirect` entry. It has NO `canActivate` (fully public) and `fullPage: true` so the app shell/nav does not wrap it:

```ts
  {
    path: 'heartbreak-hill',
    loadComponent: () =>
      import('./components/heartbreak-hill/heartbreak-hill').then(m => m.HeartbreakHill),
    data: { fullPage: true }
  },
```

- [ ] **Step 2: Verify (after the component exists in Task 6 this resolves). For now just confirm the array is syntactically valid by building later.** Commit.

```bash
git add frontend/src/app/app.routes.ts
git commit -m "Register public /heartbreak-hill route"
```

> NOTE: `npm run build` will fail to resolve the dynamic import until Task 6 creates the component. That's expected; the build gate is Task 8. Do not run the full build here.

---

## Task 5: i18n keys

**Files:**
- Modify: `frontend/src/assets/i18n/en.json`
- Modify: `frontend/src/assets/i18n/de.json`

- [ ] **Step 1: Add the `HEARTBREAK_HILL` namespace to `en.json`**

Insert a new top-level key (keep valid JSON — add a comma after the previous top-level object):

```json
  "HEARTBREAK_HILL": {
    "KICKER": "Ironman Frankfurt · 28 June 2026 · Bad Vilbel",
    "TITLE": "HEARTBREAK",
    "TITLE_2": "HILL",
    "SUBTITLE": "The same ramp the pros suffer on. How fast are you to the top?",
    "STAT_LENGTH": "Length",
    "STAT_ELEVATION": "Elevation",
    "STAT_AVG_GRADE": "Avg grade",
    "STAT_MAX_GRADE": "Max",
    "CTA_UPLOAD": "Upload GPX & claim your spot",
    "CTA_LEADERBOARD": "View leaderboard",
    "SCROLL_CUE": "scroll — take on the greats",
    "LEADERBOARD_LABEL": "Leaderboard",
    "LEADERBOARD_TITLE": "Where do you stand among the greats?",
    "TAB_RIDE": "Ride",
    "TAB_RUN": "Run",
    "COL_RANK": "#",
    "COL_NAME": "Athlete",
    "COL_TIME": "Time",
    "COL_GAP": "Gap",
    "BADGE_PRO": "Pro",
    "BADGE_REFERENCE": "Reference",
    "EMPTY_TITLE": "No times yet",
    "EMPTY_BODY": "Be the first to conquer the hill — upload your climb below.",
    "UPLOAD_LABEL": "Take part",
    "UPLOAD_TITLE": "Upload your attempt — on the board in 10 seconds.",
    "UPLOAD_BODY": "No account needed to join. Save your result and unlock ghost duels with a free PACR account.",
    "DROP_HINT": "Drag your GPX here or click to choose",
    "NAME_PLACEHOLDER": "Your display name",
    "EVALUATE": "Evaluate & rank me",
    "RESULT_LABEL": "Your result",
    "RESULT_OF": "of {{count}} {{type}}",
    "RESULT_TIME": "Your time",
    "RESULT_GAP": "Gap to leader",
    "RESULT_PERCENTILE": "Faster than",
    "FUNNEL_TITLE": "Save your result permanently?",
    "FUNNEL_BODY": "A free PACR account keeps your spot, shows your history, and lets you race any ghost.",
    "FUNNEL_CTA": "Create account & save",
    "SHARE": "Share",
    "PITCH_LABEL": "Powered by PACR",
    "PITCH_TITLE": "Train for your own Heartbreak Hill.",
    "PITCH_BODY": "PACR builds the training plan that gets you to the top — readiness, VO₂max, routes, community.",
    "PITCH_CTA": "Start free",
    "ERROR_LOAD": "Could not load the challenge. Please try again later.",
    "ERROR_UPLOAD": "We couldn't match your file to the segment. Make sure your GPX covers the Heartbreak Hill climb.",
    "UNIT_KM": "km",
    "UNIT_M": "m",
    "TYPE_RIDERS": "riders",
    "TYPE_RUNNERS": "runners"
  }
```

- [ ] **Step 2: Add the same namespace to `de.json` (German values)**

```json
  "HEARTBREAK_HILL": {
    "KICKER": "Ironman Frankfurt · 28. Juni 2026 · Bad Vilbel",
    "TITLE": "HEARTBREAK",
    "TITLE_2": "HILL",
    "SUBTITLE": "Dieselbe Rampe, an der sich die Profis quälen. Wie schnell bist du oben?",
    "STAT_LENGTH": "Länge",
    "STAT_ELEVATION": "Höhenmeter",
    "STAT_AVG_GRADE": "Ø Steigung",
    "STAT_MAX_GRADE": "Max",
    "CTA_UPLOAD": "GPX hochladen & Platz sichern",
    "CTA_LEADERBOARD": "Bestenliste ansehen",
    "SCROLL_CUE": "scrollen — gegen die Großen antreten",
    "LEADERBOARD_LABEL": "Bestenliste",
    "LEADERBOARD_TITLE": "Wo stehst du zwischen den Großen?",
    "TAB_RIDE": "Rad",
    "TAB_RUN": "Lauf",
    "COL_RANK": "#",
    "COL_NAME": "Athlet",
    "COL_TIME": "Zeit",
    "COL_GAP": "Rückstand",
    "BADGE_PRO": "Profi",
    "BADGE_REFERENCE": "Referenz",
    "EMPTY_TITLE": "Noch keine Zeiten",
    "EMPTY_BODY": "Sei der Erste am Hügel — lade unten deine Fahrt hoch.",
    "UPLOAD_LABEL": "Mach mit",
    "UPLOAD_TITLE": "Lad deinen Versuch hoch — in 10 Sekunden im Ranking.",
    "UPLOAD_BODY": "Kein Account nötig zum Mitmachen. Ergebnis sichern und Ghost-Duelle freischalten mit einem kostenlosen PACR-Konto.",
    "DROP_HINT": "GPX hierher ziehen oder klicken zum Auswählen",
    "NAME_PLACEHOLDER": "Dein Anzeigename",
    "EVALUATE": "Auswerten & einordnen",
    "RESULT_LABEL": "Dein Ergebnis",
    "RESULT_OF": "von {{count}} {{type}}",
    "RESULT_TIME": "Deine Zeit",
    "RESULT_GAP": "Rückstand zur Spitze",
    "RESULT_PERCENTILE": "Schneller als",
    "FUNNEL_TITLE": "Ergebnis dauerhaft sichern?",
    "FUNNEL_BODY": "Mit einem kostenlosen PACR-Konto behältst du deinen Platz, siehst deinen Verlauf und kannst gegen jeden Ghost fahren.",
    "FUNNEL_CTA": "Konto erstellen & sichern",
    "SHARE": "Teilen",
    "PITCH_LABEL": "Powered by PACR",
    "PITCH_TITLE": "Trainier für deinen eigenen Heartbreak Hill.",
    "PITCH_BODY": "PACR baut dir den Trainingsplan, der dich da hochbringt — Readiness, VO₂max, Strecken, Community.",
    "PITCH_CTA": "Kostenlos starten",
    "ERROR_LOAD": "Die Challenge konnte nicht geladen werden. Bitte später erneut versuchen.",
    "ERROR_UPLOAD": "Wir konnten deine Datei dem Segment nicht zuordnen. Stelle sicher, dass dein GPX den Heartbreak-Hill-Anstieg enthält.",
    "UNIT_KM": "km",
    "UNIT_M": "m",
    "TYPE_RIDERS": "Radfahrern",
    "TYPE_RUNNERS": "Läufern"
  }
```

- [ ] **Step 3: Validate both JSON files parse**

Run: `cd frontend && node -e "JSON.parse(require('fs').readFileSync('src/assets/i18n/en.json','utf8')); JSON.parse(require('fs').readFileSync('src/assets/i18n/de.json','utf8')); console.log('OK')"`
Expected: prints `OK` (no JSON syntax error).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/assets/i18n/en.json frontend/src/assets/i18n/de.json
git commit -m "Add Heartbreak Hill i18n keys (en + de)"
```

---

## Task 6: Component class (`heartbreak-hill.ts`)

**Files:**
- Create: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts`

- [ ] **Step 1: Create the component**

```ts
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { HeartbreakHillService } from '../../services/heartbreak-hill.service';
import {
  ActivityType, SegmentChallenge, LeaderboardEntry, EffortResult
} from '../../models/heartbreak-hill.model';
import { buildElevationProfile, ElevationProfile, formatGap } from './heartbreak-hill.util';

@Component({
  selector: 'app-heartbreak-hill',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './heartbreak-hill.html',
  styleUrl: './heartbreak-hill.scss'
})
export class HeartbreakHill implements OnInit {
  private readonly service = inject(HeartbreakHillService);
  private readonly router = inject(Router);

  // viewBox dims for the hero elevation profile
  readonly profileWidth = 1200;
  readonly profileHeight = 360;

  /** Exposed so the template can format the leaderboard gap column. */
  readonly formatGap = formatGap;

  loading = signal(true);
  loadError = signal(false);
  challenge = signal<SegmentChallenge | null>(null);

  activeTab = signal<ActivityType>('RIDE');
  leaderboard = signal<LeaderboardEntry[]>([]);
  leaderboardLoading = signal(false);

  // upload state
  selectedFile = signal<File | null>(null);
  displayName = signal('');
  submitting = signal(false);
  uploadError = signal<string | null>(null);
  result = signal<EffortResult | null>(null);

  /** SVG elevation profile derived from the challenge polyline, or null if absent. */
  profile = computed<ElevationProfile | null>(() => {
    const c = this.challenge();
    if (!c?.polylineJson) {
      return null;
    }
    try {
      const pts = JSON.parse(c.polylineJson) as [number, number, number][];
      return buildElevationProfile(pts, this.profileWidth, this.profileHeight);
    } catch {
      return null;
    }
  });

  ngOnInit(): void {
    this.service.getChallenge().subscribe({
      next: c => {
        this.challenge.set(c);
        this.loading.set(false);
        this.loadLeaderboard();
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      }
    });
  }

  selectTab(type: ActivityType): void {
    if (this.activeTab() === type) {
      return;
    }
    this.activeTab.set(type);
    this.loadLeaderboard();
  }

  private loadLeaderboard(): void {
    this.leaderboardLoading.set(true);
    this.service.getLeaderboard(this.activeTab()).subscribe({
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

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
  }

  onFileDropped(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0] ?? null;
    if (file) {
      this.selectedFile.set(file);
    }
  }

  canSubmit = computed(() =>
    !!this.selectedFile() && this.displayName().trim().length > 0 && !this.submitting());

  submit(): void {
    const file = this.selectedFile();
    if (!file || !this.canSubmit()) {
      return;
    }
    this.submitting.set(true);
    this.uploadError.set(null);
    this.service.submitEffort(this.activeTab(), this.displayName().trim(), file).subscribe({
      next: res => {
        this.result.set(res);
        this.submitting.set(false);
        this.loadLeaderboard();          // refresh so the new entry shows
      },
      error: () => {
        this.uploadError.set('ERROR_UPLOAD');
        this.submitting.set(false);
      }
    });
  }

  /** Highlights the just-submitted effort row in the leaderboard. */
  isMyEffort(entry: LeaderboardEntry): boolean {
    return this.result()?.effortId === entry.effortId;
  }

  goToSignup(): void {
    this.router.navigate(['/signup']);
  }
}
```

- [ ] **Step 2: Commit** (the build runs in Task 8 once template + styles exist)

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts
git commit -m "Add Heartbreak Hill component class"
```

---

## Task 7: Component template (`heartbreak-hill.html`)

**Files:**
- Create: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html`

- [ ] **Step 1: Create the template**

```html
<div class="hh">
  @if (loading()) {
    <div class="hh-loading"><div class="spin"></div></div>
  } @else if (loadError()) {
    <div class="hh-error">{{ 'HEARTBREAK_HILL.ERROR_LOAD' | translate }}</div>
  } @else if (challenge(); as c) {

    <!-- HERO -->
    <header class="hero">
      <div class="hero-bg">
        <svg [attr.viewBox]="'0 0 ' + profileWidth + ' ' + profileHeight" preserveAspectRatio="xMidYMax slice">
          @if (profile(); as p) {
            <path [attr.d]="p.area" class="hero-area"></path>
            <path [attr.d]="p.line" class="hero-line"></path>
          } @else {
            <!-- graceful fallback when no polyline is seeded yet -->
            <path class="hero-area" d="M0,360 C300,330 600,250 900,160 C1050,120 1150,100 1200,92 L1200,360 Z"></path>
            <path class="hero-line" d="M0,330 C300,300 600,220 900,130 C1050,90 1150,72 1200,64"></path>
          }
        </svg>
        <div class="hero-veil"></div>
      </div>

      <div class="hero-content">
        <span class="kicker"><span class="dot"></span>{{ 'HEARTBREAK_HILL.KICKER' | translate }}</span>
        <h1>{{ 'HEARTBREAK_HILL.TITLE' | translate }}<br>{{ 'HEARTBREAK_HILL.TITLE_2' | translate }}</h1>
        <p class="hero-sub">{{ 'HEARTBREAK_HILL.SUBTITLE' | translate }}</p>

        <div class="stats">
          <div class="chip">
            <b>{{ c.distanceM ? (c.distanceM / 1000 | number:'1.1-1') : '—' }}</b>
            <span>{{ 'HEARTBREAK_HILL.STAT_LENGTH' | translate }} ({{ 'HEARTBREAK_HILL.UNIT_KM' | translate }})</span>
          </div>
          <div class="chip">
            <b>{{ c.elevationGainM ?? '—' }}</b>
            <span>{{ 'HEARTBREAK_HILL.STAT_ELEVATION' | translate }} ({{ 'HEARTBREAK_HILL.UNIT_M' | translate }})</span>
          </div>
          <div class="chip">
            <b>{{ c.avgGradePct ? (c.avgGradePct | number:'1.1-1') + ' %' : '—' }}</b>
            <span>{{ 'HEARTBREAK_HILL.STAT_AVG_GRADE' | translate }}</span>
          </div>
          <div class="chip">
            <b>{{ c.maxGradePct ? (c.maxGradePct | number:'1.0-0') + ' %' : '—' }}</b>
            <span>{{ 'HEARTBREAK_HILL.STAT_MAX_GRADE' | translate }}</span>
          </div>
        </div>

        <div class="hero-ctas">
          <a class="btn" href="#upload">{{ 'HEARTBREAK_HILL.CTA_UPLOAD' | translate }}</a>
          <a class="btn ghost" href="#leaderboard">{{ 'HEARTBREAK_HILL.CTA_LEADERBOARD' | translate }}</a>
        </div>
        <div class="scroll-cue">↓ {{ 'HEARTBREAK_HILL.SCROLL_CUE' | translate }}</div>
      </div>
    </header>

    <!-- LEADERBOARD -->
    <section id="leaderboard" class="section">
      <span class="label">{{ 'HEARTBREAK_HILL.LEADERBOARD_LABEL' | translate }}</span>
      <h2>{{ 'HEARTBREAK_HILL.LEADERBOARD_TITLE' | translate }}</h2>

      <div class="lb">
        <div class="lb-tabs">
          <button [class.on]="activeTab() === 'RIDE'" (click)="selectTab('RIDE')">
            🚴 {{ 'HEARTBREAK_HILL.TAB_RIDE' | translate }}</button>
          <button [class.on]="activeTab() === 'RUN'" (click)="selectTab('RUN')">
            🏃 {{ 'HEARTBREAK_HILL.TAB_RUN' | translate }}</button>
        </div>

        @if (leaderboardLoading()) {
          <div class="lb-loading"><div class="spin"></div></div>
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
                  <span class="badge pro">{{ 'HEARTBREAK_HILL.BADGE_PRO' | translate }}</span>
                }
              </div>
              <div class="tm">{{ entry.elapsedFormatted }}</div>
              <div class="gap">{{ formatGap(entry.gapToLeaderSeconds) }}</div>
            </div>
          }
        }
      </div>
    </section>

    <!-- UPLOAD + RESULT -->
    <section id="upload" class="section">
      <span class="label">{{ 'HEARTBREAK_HILL.UPLOAD_LABEL' | translate }}</span>
      <h2>{{ 'HEARTBREAK_HILL.UPLOAD_TITLE' | translate }}</h2>
      <p class="lead">{{ 'HEARTBREAK_HILL.UPLOAD_BODY' | translate }}</p>

      <div class="two-col">
        <div class="panel">
          <label class="drop" (dragover)="$event.preventDefault()" (drop)="onFileDropped($event)">
            <input type="file" accept=".gpx" hidden (change)="onFileSelected($event)">
            <div class="drop-icon">⬆</div>
            <p>{{ selectedFile()?.name || ('HEARTBREAK_HILL.DROP_HINT' | translate) }}</p>
          </label>

          <input class="field" [ngModel]="displayName()" (ngModelChange)="displayName.set($event)"
                 [placeholder]="'HEARTBREAK_HILL.NAME_PLACEHOLDER' | translate">

          <div class="seg">
            <span [class.on]="activeTab() === 'RIDE'" (click)="selectTab('RIDE')">🚴 {{ 'HEARTBREAK_HILL.TAB_RIDE' | translate }}</span>
            <span [class.on]="activeTab() === 'RUN'" (click)="selectTab('RUN')">🏃 {{ 'HEARTBREAK_HILL.TAB_RUN' | translate }}</span>
          </div>

          @if (uploadError(); as err) {
            <div class="upload-error">{{ ('HEARTBREAK_HILL.' + err) | translate }}</div>
          }

          <button class="btn block" [disabled]="!canSubmit()" (click)="submit()">
            @if (submitting()) { <span class="spin sm"></span> } @else { {{ 'HEARTBREAK_HILL.EVALUATE' | translate }} }
          </button>
        </div>

        @if (result(); as r) {
          <div class="panel result">
            <span class="label">{{ 'HEARTBREAK_HILL.RESULT_LABEL' | translate }}</span>
            <div class="pl">#{{ r.rank }}</div>
            <div class="of">{{ 'HEARTBREAK_HILL.RESULT_OF' | translate:{ count: r.totalCount, type: activeTab() === 'RIDE' ? ('HEARTBREAK_HILL.TYPE_RIDERS' | translate) : ('HEARTBREAK_HILL.TYPE_RUNNERS' | translate) } }}</div>
            <div class="meta">
              <div><small>{{ 'HEARTBREAK_HILL.RESULT_TIME' | translate }}</small><b>{{ r.elapsedFormatted }}</b></div>
              <div><small>{{ 'HEARTBREAK_HILL.RESULT_PERCENTILE' | translate }}</small><b>{{ r.percentileBeaten | number:'1.0-0' }} %</b></div>
            </div>
            <div class="funnel">
              <b>🔒 {{ 'HEARTBREAK_HILL.FUNNEL_TITLE' | translate }}</b>
              <p>{{ 'HEARTBREAK_HILL.FUNNEL_BODY' | translate }}</p>
              <button class="btn block" (click)="goToSignup()">{{ 'HEARTBREAK_HILL.FUNNEL_CTA' | translate }}</button>
            </div>
          </div>
        }
      </div>
    </section>

    <!-- PITCH -->
    <section class="section pitch">
      <span class="label">{{ 'HEARTBREAK_HILL.PITCH_LABEL' | translate }}</span>
      <h2>{{ 'HEARTBREAK_HILL.PITCH_TITLE' | translate }}</h2>
      <p class="lead center">{{ 'HEARTBREAK_HILL.PITCH_BODY' | translate }}</p>
      <button class="btn" (click)="goToSignup()">{{ 'HEARTBREAK_HILL.PITCH_CTA' | translate }}</button>
    </section>

  }
</div>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.html
git commit -m "Add Heartbreak Hill component template"
```

---

## Task 8: Component styles + build gate (`heartbreak-hill.scss`)

**Files:**
- Create: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss`

- [ ] **Step 1: Create the styles** (dark, cinematic, PACR tokens; component-scoped animations)

```scss
:host { display: block; background: var(--bg); color: var(--text); font-family: var(--font-family); }

.hh-loading, .lb-loading { display: flex; justify-content: center; padding: 80px 0; }
.spin { width: 34px; height: 34px; border: 3px solid var(--border); border-top-color: var(--pp); border-radius: 50%; animation: spin 1s linear infinite; }
.spin.sm { width: 18px; height: 18px; }
@keyframes spin { to { transform: rotate(360deg); } }
.hh-error { padding: 80px 22px; text-align: center; color: var(--text-muted); }

.btn { display: inline-flex; align-items: center; justify-content: center; gap: 8px; background: var(--pp); color: #07120a; font-weight: 800; border: none; border-radius: 12px; padding: 13px 20px; font-size: 15px; cursor: pointer; box-shadow: var(--pp-glow); text-decoration: none; }
.btn.ghost { background: transparent; color: var(--pp); border: 1px solid var(--pp-border, rgba(143,252,46,.4)); box-shadow: none; }
.btn.block { width: 100%; margin-top: 14px; }
.btn:disabled { opacity: .5; cursor: not-allowed; box-shadow: none; }

.label { font-size: 12px; letter-spacing: .18em; text-transform: uppercase; color: var(--text-muted); }
.section { max-width: 1080px; margin: 0 auto; padding: 70px 22px; }
.section h2 { font-size: clamp(28px, 4.4vw, 44px); font-weight: 900; letter-spacing: -.01em; margin: 8px 0 14px; }
.lead { font-size: 18px; color: var(--text-muted); max-width: 680px; line-height: 1.6; }
.lead.center { margin: 0 auto 22px; }

/* HERO */
.hero { position: relative; min-height: 88vh; display: flex; align-items: flex-end; overflow: hidden; border-bottom: 1px solid var(--border); }
.hero-bg { position: absolute; inset: 0; z-index: 0; }
.hero-bg svg { width: 100%; height: 100%; }
.hero-area { fill: var(--pp-container); }
.hero-line { fill: none; stroke: var(--pp); stroke-width: 4; stroke-linecap: round; filter: drop-shadow(0 0 12px rgba(143,252,46,.5)); }
.hero-veil { position: absolute; inset: 0; background: radial-gradient(120% 80% at 50% 8%, rgba(13,17,23,0) 30%, rgba(13,17,23,.6) 72%, var(--bg) 100%); }
.hero-content { position: relative; z-index: 2; max-width: 1080px; margin: 0 auto; padding: 0 22px 48px; width: 100%; }
.kicker { display: inline-flex; gap: 10px; align-items: center; font-size: 12.5px; letter-spacing: .16em; text-transform: uppercase; color: var(--pp); border: 1px solid var(--pp-border, rgba(143,252,46,.35)); border-radius: 999px; padding: 6px 14px; margin-bottom: 18px; }
.kicker .dot { width: 7px; height: 7px; border-radius: 50%; background: var(--pp); box-shadow: var(--pp-glow); animation: pulse 1.8s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: .35; } }
.hero h1 { font-size: clamp(46px, 9vw, 104px); line-height: .92; font-weight: 900; letter-spacing: -.02em; text-shadow: 0 6px 40px rgba(0,0,0,.6); }
.hero-sub { font-size: clamp(16px, 2.2vw, 22px); font-weight: 700; margin-top: 14px; max-width: 640px; }
.stats { display: flex; gap: 12px; flex-wrap: wrap; margin: 26px 0 24px; }
.chip { display: flex; flex-direction: column; gap: 2px; padding: 10px 16px; background: var(--panel-soft, rgba(255,255,255,.04)); border: 1px solid var(--border); border-radius: 12px; }
.chip b { font-size: 22px; font-weight: 900; font-variant-numeric: tabular-nums; }
.chip span { font-size: 11px; letter-spacing: .08em; text-transform: uppercase; color: var(--text-muted); }
.hero-ctas { display: flex; gap: 12px; flex-wrap: wrap; }
.scroll-cue { font-size: 12px; color: var(--text-muted); margin-top: 22px; letter-spacing: .12em; text-transform: uppercase; }

/* LEADERBOARD */
.lb { background: var(--panel-soft, rgba(255,255,255,.04)); border: 1px solid var(--border); border-radius: 18px; overflow: hidden; }
.lb-tabs { display: flex; border-bottom: 1px solid var(--border); }
.lb-tabs button { flex: 1; background: none; border: none; color: var(--text-muted); padding: 16px; font-weight: 800; font-size: 15px; cursor: pointer; font-family: inherit; }
.lb-tabs button.on { color: var(--pp); box-shadow: inset 0 -2px 0 var(--pp); }
.lb-empty { padding: 44px 22px; text-align: center; color: var(--text-muted); }
.lb-empty h3 { color: var(--text); font-size: 20px; font-weight: 800; margin-bottom: 8px; }
.row { display: grid; grid-template-columns: 46px 1fr auto auto; gap: 14px; align-items: center; padding: 13px 18px; border-bottom: 1px solid rgba(255,255,255,.05); }
.row .rk { font-weight: 900; font-size: 18px; color: var(--text-muted); text-align: center; }
.row .nm { font-weight: 700; }
.row .tm { font-weight: 900; font-variant-numeric: tabular-nums; }
.row .gap { font-size: 12px; color: var(--text-muted); font-variant-numeric: tabular-nums; }
.row.ref { background: rgba(143,252,46,.04); }
.row.you { background: linear-gradient(90deg, rgba(143,252,46,.16), rgba(143,252,46,.03)); box-shadow: inset 0 0 0 1px rgba(143,252,46,.4); }
.row.you .rk { color: var(--pp); }
.badge { display: inline-block; font-size: 10px; font-weight: 800; letter-spacing: .06em; text-transform: uppercase; padding: 2px 7px; border-radius: 6px; margin-left: 8px; vertical-align: middle; }
.badge.pro { background: var(--pp-container); color: var(--pp); }

/* UPLOAD + RESULT */
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; align-items: start; margin-top: 28px; }
.panel { background: var(--panel-soft, rgba(255,255,255,.04)); border: 1px solid var(--border); border-radius: 18px; padding: 26px; }
.drop { display: block; border: 2px dashed var(--pp-border, rgba(143,252,46,.4)); border-radius: 14px; padding: 34px; text-align: center; color: var(--text-muted); cursor: pointer; }
.drop-icon { font-size: 40px; color: var(--pp); }
.field { width: 100%; margin-top: 14px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; padding: 12px 14px; color: var(--text); font-family: inherit; }
.seg { display: flex; gap: 8px; padding: 6px; margin-top: 14px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; }
.seg span { flex: 1; text-align: center; padding: 8px; border-radius: 8px; cursor: pointer; color: var(--text-muted); font-weight: 700; }
.seg span.on { background: var(--pp); color: #07120a; }
.upload-error { margin-top: 14px; padding: 12px 14px; border-radius: 10px; background: rgba(248,81,73,.12); color: var(--error); font-size: 14px; }
.result { background: linear-gradient(160deg, rgba(143,252,46,.12), rgba(143,252,46,.02)); border: 1px solid var(--pp-border, rgba(143,252,46,.35)); animation: reveal .5s ease; }
@keyframes reveal { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }
.result .pl { font-size: 64px; font-weight: 900; line-height: 1; color: var(--pp); text-shadow: var(--pp-glow); margin-top: 6px; }
.result .of { color: var(--text-muted); margin-top: 4px; }
.result .meta { display: flex; gap: 18px; margin: 16px 0 4px; flex-wrap: wrap; }
.result .meta small { display: block; color: var(--text-muted); font-size: 11px; text-transform: uppercase; letter-spacing: .08em; }
.result .meta b { font-size: 22px; font-weight: 900; font-variant-numeric: tabular-nums; }
.funnel { margin-top: 18px; padding: 16px; border-radius: 12px; background: var(--pp-container); border: 1px solid var(--pp-border, rgba(143,252,46,.3)); font-size: 14px; }
.funnel p { color: var(--text-muted); margin: 6px 0 0; }

/* PITCH */
.pitch { text-align: center; border-top: 1px solid var(--border); }

@media (max-width: 820px) { .two-col { grid-template-columns: 1fr; } }
```

- [ ] **Step 2: Build the frontend (the real compile gate for Tasks 1–8)**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS — the lazy route resolves, the component, template, and styles all compile, no TS or template binding errors.

> If the build flags a strict-template error (e.g. a translate-pipe arg type), fix it minimally in the template/component to satisfy Angular's strict mode, keeping behaviour identical.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss
git commit -m "Add Heartbreak Hill component styles"
```

---

## Task 9: Frontend test suite, visual smoke, changelog

- [ ] **Step 1: Run the frontend unit tests (Vitest)**

Run: `cd frontend && npx vitest run` (or the project's `npm test -- --run`)
Expected: all specs pass (incl. the new service + util specs). Vitest runs in node/jsdom — no browser needed. Do NOT use Karma flags. If the runner can't start for an environment reason, report it and rely on `npm run build` + the build-time template type checking as the gate.

- [ ] **Step 2: Visual smoke test (best-effort)**

Use the `superpowers:webapp-testing` (Playwright) skill if available. Start the backend (set `PACR_AI_API_KEY` to a dummy value and `JAVA_HOME` to JDK 21 so it boots; ensure MariaDB is up) and the frontend (`npm start`), seed the challenge via the admin endpoint (see Plan 1 Task 16 Step 2), then:
- Navigate anonymously (no login) to `http://localhost:4200/heartbreak-hill`.
- Confirm: the page renders WITHOUT redirecting to `/login` (proves the public route + interceptor behave), the hero + stat chips show, the leaderboard shows its empty state (or seeded rows), and the upload panel is present.
- Capture a screenshot.
If the environment can't run the full stack (no MariaDB / no Chrome), report exactly what was and wasn't verified — do not fake it. The build (Task 8) + unit tests (Step 1) remain the authoritative gates.

- [ ] **Step 3: Update the changelog**

Extend the existing `[Unreleased] / ### Added` Heartbreak Hill entry in `CHANGELOG.md` (the version is already `0.50.0` from Plan 1; do NOT bump again) with a frontend line:

```markdown
- Public Heartbreak Hill landing page (`/heartbreak-hill`): cinematic 2D elevation-profile hero, Ride/Run leaderboard, anonymous GPX upload with instant rank reveal, and PACR sign-up funnel.
```

- [ ] **Step 4: Commit**

```bash
git add CHANGELOG.md
git commit -m "Document Heartbreak Hill public page in changelog"
```

---

## Self-Review

**Spec coverage** (against `2026-06-16-heartbreak-hill-challenge-design.md` §6, Phase 1 frontend):
- Public route `/heartbreak-hill`, no auth guard — Task 4. ✓
- Hero with 2D elevation profile + stat chips + CTA — Tasks 6/7/8 (3D is Plan 3). ✓
- Leaderboard tabs Ride/Run, reference + public mixed, your row highlighted — Tasks 6/7/8. ✓
- Anonymous upload (GPX) + instant result reveal + funnel CTA + pitch — Tasks 6/7/8. ✓
- i18n en + de — Task 5. ✓
- Service over the public endpoints — Task 2. ✓
- Graceful empty/no-polyline states — covered (profile fallback path; leaderboard empty state). ✓

**Deferred to Plan 3 (correct):** WebGL 3D terrain, ghost-race duel, share/badge image generation, the optional Leaflet course map (RouteMiniMap is available but not required for Phase 1).

**Type consistency:** Model interfaces match backend DTO fields (`reference`, `gapToLeaderSeconds`, `percentileBeaten`, `elapsedFormatted`). `ActivityType` union `'RIDE'|'RUN'` matches the backend enum names used as query/form params. `HeartbreakHillService` method names used by the component (`getChallenge`, `getLeaderboard`, `submitEffort`, `getTrack`) match Task 2. `buildElevationProfile`/`formatGap`/`formatGrade` signatures match their specs and the component import.

**Placeholder scan:** The hero has an explicit fallback path for when `polylineJson` is null (real data is seeded later per Plan 1 / the spec's open points) — this is intentional graceful degradation, not a placeholder TODO. No `TODO`/`TBD` in code.

**Note on helpers:** `formatGap` is exposed as a component field (`readonly formatGap = formatGap`) and used in the leaderboard `.gap` cell — it renders the leader/null as an em dash and others as `+m:ss`. `formatGrade` is a tested helper kept for richer grade formatting; the hero currently uses the `number` pipe for grades, so `formatGrade` is exercised by its spec rather than the template. Keep all template bindings type-correct for Angular's strict template compiler.
