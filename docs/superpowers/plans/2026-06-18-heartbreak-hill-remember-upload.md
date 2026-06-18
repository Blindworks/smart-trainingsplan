# Heartbreak Hill — Upload merken (localStorage) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Auf der öffentlichen `/heartbreak-hill`-Seite den anonymen Upload pro Aktivitätstyp im Browser merken, sodass Ergebnis-Panel und teilbares Story-Bild beim erneuten Besuch automatisch wieder erscheinen.

**Architecture:** Reine Frontend-Erweiterung. Eine neue, reine Util-Datei kapselt das `localStorage`-Lesen/Schreiben eines `EffortResult`-Snapshots je `ActivityType` (`RIDE`/`RUN`). Die bestehende `HeartbreakHill`-Komponente speichert nach dem Upload, stellt beim Laden/Tab-Wechsel wieder her und bietet einen „Verwerfen"-Link. Kein Backend, keine Migration, kein neues Model-Feld.

**Tech Stack:** Angular 19 (standalone, Signals), TypeScript, Vitest (`@angular/build:unit-test`), `@ngx-translate/core`.

**Spec:** [docs/superpowers/specs/2026-06-18-heartbreak-hill-remember-upload-design.md](../specs/2026-06-18-heartbreak-hill-remember-upload-design.md)

**Wichtige Projektregeln (aus CLAUDE.md / Projektgedächtnis):**
- Lokal direkt auf `main` committen — **kein** Feature-Branch, **kein** PR, **keine** Worktrees.
- Jeder `git add` nennt **exakte Dateipfade** — im Working Tree liegt eine unrelated Änderung (`.claude/agent-memory/springboot-backend-dev/MEMORY.md`), die **nicht** mitcommittet werden darf.
- Alle Befehle laufen unter Windows (Git Bash für `./version-bump.sh`, PowerShell/Bash für npm).

---

## File Structure

**Neu**
- `frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.ts` — reine Persistenz-Funktionen (`loadStoredEffort`, `saveStoredEffort`, `clearStoredEffort`) + Typ `StoredEffort`. Einzige Verantwortung: localStorage-Zugriff inkl. Fehler-/Korruptionsabsicherung.
- `frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.spec.ts` — Vitest-Unit-Tests für die Util.

**Geändert**
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts` — Signals `restored`/`restoredAt`, Methoden `restoreForCurrentTab()`/`discardStored()`, Anpassung `ngOnInit`/`selectTab`/`submit`.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html` — dezenter „gemerkt"-Hinweis + Verwerfen-Link im Result-Panel.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss` — Styles für den Hinweis.
- `frontend/src/assets/i18n/en.json`, `frontend/src/assets/i18n/de.json` — Keys `RESTORED_HINT`, `RESTORED_DISCARD`.
- `pom.xml`, `frontend/package.json`, `CHANGELOG.md` — minor-Version + Changelog.

---

## Task 1: Persistenz-Util (TDD)

**Files:**
- Create: `frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.ts`
- Test: `frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.spec.ts`

- [ ] **Step 1: Write the failing test**

Erstelle `frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.spec.ts`. Vitest-Globals (`describe`/`it`/`expect`/`beforeEach`/`vi`) sind im Projekt aktiv (bestehende `*.util.spec.ts` importieren sie nicht), `localStorage` ist im jsdom-Environment vorhanden.

```ts
import { loadStoredEffort, saveStoredEffort, clearStoredEffort } from './heartbreak-storage.util';
import { ActivityType, EffortResult } from '../../models/heartbreak-hill.model';

const STORAGE_KEY = 'pacr.heartbreak-hill.efforts.v1';

function makeResult(overrides: Partial<EffortResult> = {}): EffortResult {
  return {
    effortId: 42,
    editToken: 'tok-abc',
    rank: 7,
    totalCount: 120,
    elapsedSeconds: 933,
    elapsedFormatted: '15:33',
    gapToLeaderSeconds: 46,
    percentileBeaten: 88,
    avgSpeedKmh: 23.4,
    avgPaceSecondsPerKm: null,
    status: 'MATCHED',
    ...overrides,
  };
}

describe('heartbreak-storage.util', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('round-trips a saved effort for a type', () => {
    saveStoredEffort('RIDE', makeResult(), 'Ben');
    const loaded = loadStoredEffort('RIDE');
    expect(loaded).not.toBeNull();
    expect(loaded!.result.effortId).toBe(42);
    expect(loaded!.result.rank).toBe(7);
    expect(loaded!.displayName).toBe('Ben');
    expect(typeof loaded!.savedAt).toBe('string');
  });

  it('returns null for a type that was never saved', () => {
    saveStoredEffort('RIDE', makeResult(), 'Ben');
    expect(loadStoredEffort('RUN')).toBeNull();
  });

  it('keeps RIDE and RUN independent', () => {
    saveStoredEffort('RIDE', makeResult({ effortId: 1, rank: 3 }), 'Rider');
    saveStoredEffort('RUN', makeResult({ effortId: 2, rank: 9 }), 'Runner');
    expect(loadStoredEffort('RIDE')!.result.effortId).toBe(1);
    expect(loadStoredEffort('RUN')!.result.effortId).toBe(2);
    // re-saving RIDE must not wipe RUN
    saveStoredEffort('RIDE', makeResult({ effortId: 11, rank: 1 }), 'Rider2');
    expect(loadStoredEffort('RIDE')!.result.effortId).toBe(11);
    expect(loadStoredEffort('RUN')!.result.effortId).toBe(2);
  });

  it('clears only the given type', () => {
    saveStoredEffort('RIDE', makeResult(), 'Rider');
    saveStoredEffort('RUN', makeResult(), 'Runner');
    clearStoredEffort('RIDE');
    expect(loadStoredEffort('RIDE')).toBeNull();
    expect(loadStoredEffort('RUN')).not.toBeNull();
  });

  it('returns null for corrupt JSON', () => {
    localStorage.setItem(STORAGE_KEY, 'not-json{');
    expect(loadStoredEffort('RIDE')).toBeNull();
  });

  it('returns null when the snapshot lacks required fields', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ RIDE: { displayName: 'x', savedAt: 'y' } }));
    expect(loadStoredEffort('RIDE')).toBeNull();
  });

  it('does not throw when localStorage.setItem fails', () => {
    const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceeded');
    });
    expect(() => saveStoredEffort('RIDE', makeResult(), 'Ben')).not.toThrow();
    spy.mockRestore();
  });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && npm test`
Expected: FAIL — Vitest kann `./heartbreak-storage.util` nicht auflösen (Modul existiert noch nicht).

- [ ] **Step 3: Write the minimal implementation**

Erstelle `frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.ts`:

```ts
import { ActivityType, EffortResult } from '../../models/heartbreak-hill.model';

const STORAGE_KEY = 'pacr.heartbreak-hill.efforts.v1';

/** A remembered upload snapshot for one activity type. */
export interface StoredEffort {
  result: EffortResult;
  displayName: string;
  savedAt: string; // ISO timestamp
}

type StoredEfforts = Partial<Record<ActivityType, StoredEffort>>;

/** Reads and parses the whole map; returns {} on any failure (missing/corrupt/unavailable). */
function readAll(): StoredEfforts {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return {};
    }
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? (parsed as StoredEfforts) : {};
  } catch {
    return {};
  }
}

/** Persists the map; swallows quota/availability errors so the feature degrades silently. */
function writeAll(map: StoredEfforts): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(map));
  } catch {
    /* localStorage unavailable or full — degrade silently */
  }
}

/** Guards against an old/partial schema after a deploy. */
function isValidEntry(entry: StoredEffort | undefined): entry is StoredEffort {
  const r = entry?.result;
  return !!r && typeof r === 'object' && typeof r.effortId === 'number' && typeof r.rank === 'number';
}

export function loadStoredEffort(type: ActivityType): StoredEffort | null {
  const entry = readAll()[type];
  return isValidEntry(entry) ? entry : null;
}

export function saveStoredEffort(type: ActivityType, result: EffortResult, displayName: string): void {
  const map = readAll();
  map[type] = { result, displayName, savedAt: new Date().toISOString() };
  writeAll(map);
}

export function clearStoredEffort(type: ActivityType): void {
  const map = readAll();
  delete map[type];
  if (Object.keys(map).length === 0) {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
  } else {
    writeAll(map);
  }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd frontend && npm test`
Expected: PASS — alle `heartbreak-storage.util`-Tests grün (bestehende Suiten bleiben grün).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.ts frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.spec.ts
git commit -m "$(cat <<'EOF'
Add localStorage persistence util for Heartbreak Hill efforts

Stores an EffortResult snapshot per activity type (RIDE/RUN) under a
versioned key. All access is guarded so a missing/corrupt/unavailable
localStorage degrades to no-op instead of throwing.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: i18n-Keys (en + de)

**Files:**
- Modify: `frontend/src/assets/i18n/en.json`
- Modify: `frontend/src/assets/i18n/de.json`

- [ ] **Step 1: Add the English keys**

In `frontend/src/assets/i18n/en.json` im `HEARTBREAK_HILL`-Block (zwischen `RESULT_PERCENTILE` und `FUNNEL_TITLE`) ersetzen:

Vorher:
```json
    "RESULT_PERCENTILE": "Faster than",
    "FUNNEL_TITLE": "Save your result permanently?",
```
Nachher:
```json
    "RESULT_PERCENTILE": "Faster than",
    "RESTORED_HINT": "Saved from your last upload",
    "RESTORED_DISCARD": "Discard",
    "FUNNEL_TITLE": "Save your result permanently?",
```

- [ ] **Step 2: Add the German keys**

In `frontend/src/assets/i18n/de.json` im `HEARTBREAK_HILL`-Block ersetzen:

Vorher:
```json
    "RESULT_PERCENTILE": "Schneller als",
    "FUNNEL_TITLE": "Ergebnis dauerhaft sichern?",
```
Nachher:
```json
    "RESULT_PERCENTILE": "Schneller als",
    "RESTORED_HINT": "Von deinem letzten Upload gemerkt",
    "RESTORED_DISCARD": "Verwerfen",
    "FUNNEL_TITLE": "Ergebnis dauerhaft sichern?",
```

- [ ] **Step 3: Verify JSON validity**

Run: `cd frontend && node -e "JSON.parse(require('fs').readFileSync('src/assets/i18n/en.json','utf8')); JSON.parse(require('fs').readFileSync('src/assets/i18n/de.json','utf8')); console.log('OK')"`
Expected: `OK` (kein Syntaxfehler durch fehlende/zu viele Kommas).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/assets/i18n/en.json frontend/src/assets/i18n/de.json
git commit -m "$(cat <<'EOF'
Add i18n keys for the remembered-upload hint (de + en)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Persistenz in die Komponente verdrahten

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts`

Nach diesem Task ist das Feature funktional aktiv (Upload wird gespeichert, beim Reload/Tab-Wechsel wiederhergestellt); der sichtbare Hinweis-Text folgt in Task 4.

- [ ] **Step 1: Add the util import**

Vorher (`heartbreak-hill.ts:17`):
```ts
import { isWebglAvailable } from './heartbreak-3d/heartbreak-3d.util';
```
Nachher:
```ts
import { isWebglAvailable } from './heartbreak-3d/heartbreak-3d.util';
import { loadStoredEffort, saveStoredEffort, clearStoredEffort } from './heartbreak-storage.util';
```

- [ ] **Step 2: Add the restored-state signals**

Vorher (`heartbreak-hill.ts:51`):
```ts
  result = signal<EffortResult | null>(null);
```
Nachher:
```ts
  result = signal<EffortResult | null>(null);

  /** True when the shown result was restored from localStorage (not a fresh upload). */
  restored = signal(false);
  restoredAt = signal<string | null>(null);
```

- [ ] **Step 3: Restore on load in ngOnInit**

Vorher (`heartbreak-hill.ts:83-89`):
```ts
  ngOnInit(): void {
    this.service.getChallenge().subscribe({
      next: c => {
        this.challenge.set(c);
        this.loading.set(false);
        this.loadLeaderboard();
      },
```
Nachher:
```ts
  ngOnInit(): void {
    this.service.getChallenge().subscribe({
      next: c => {
        this.challenge.set(c);
        this.loading.set(false);
        this.loadLeaderboard();
        this.restoreForCurrentTab();
      },
```

(Die Wiederherstellung läuft bewusst **nach** `challenge.set(c)`, weil `renderShare()` die Challenge-Polyline braucht.)

- [ ] **Step 4: Restore on tab switch + add restore/discard methods**

Vorher (`heartbreak-hill.ts:97-106`):
```ts
  selectTab(type: ActivityType): void {
    if (this.activeTab() === type) {
      return;
    }
    this.activeTab.set(type);
    this.result.set(null);
    this.sharePreviewUrl.set(null);
    this.uploadError.set(null);
    this.loadLeaderboard();
  }
```
Nachher:
```ts
  selectTab(type: ActivityType): void {
    if (this.activeTab() === type) {
      return;
    }
    this.activeTab.set(type);
    this.uploadError.set(null);
    this.loadLeaderboard();
    this.restoreForCurrentTab();
  }

  /** Restores the remembered effort (Snapshot) for the active tab, or clears the panel. */
  private restoreForCurrentTab(): void {
    const stored = loadStoredEffort(this.activeTab());
    if (stored) {
      this.result.set(stored.result);
      this.displayName.set(stored.displayName);
      this.restored.set(true);
      this.restoredAt.set(stored.savedAt);
      this.shareTemplate.set('A');
      void this.renderShare();
    } else {
      this.result.set(null);
      this.sharePreviewUrl.set(null);
      this.restored.set(false);
      this.restoredAt.set(null);
    }
  }

  /** Forgets the remembered effort for the active tab and closes the result panel. */
  discardStored(): void {
    clearStoredEffort(this.activeTab());
    this.result.set(null);
    this.sharePreviewUrl.set(null);
    this.restored.set(false);
    this.restoredAt.set(null);
  }
```

- [ ] **Step 5: Persist on successful upload**

Vorher (`heartbreak-hill.ts:147-153`):
```ts
      next: res => {
        this.result.set(res);
        this.shareTemplate.set('A');
        void this.renderShare();
        this.submitting.set(false);
        this.loadLeaderboard();          // refresh so the new entry shows
      },
```
Nachher:
```ts
      next: res => {
        this.result.set(res);
        saveStoredEffort(this.activeTab(), res, this.displayName().trim());
        this.restored.set(false);
        this.restoredAt.set(null);
        this.shareTemplate.set('A');
        void this.renderShare();
        this.submitting.set(false);
        this.loadLeaderboard();          // refresh so the new entry shows
      },
```

- [ ] **Step 6: Verify it compiles**

Run: `cd frontend && npm run build`
Expected: Build erfolgreich, keine TypeScript-Fehler (`restored`/`restoredAt`/`restoreForCurrentTab`/`discardStored` korrekt typisiert, Util-Importe aufgelöst).

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts
git commit -m "$(cat <<'EOF'
Wire upload persistence into the Heartbreak Hill component

Saves the result snapshot per type after upload, restores it on load
and tab switch, and exposes discardStored() to forget it.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: „Gemerkt"-Hinweis + Verwerfen-Link (Template + SCSS)

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html`
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss`

- [ ] **Step 1: Add the hint block to the result panel**

Vorher (`heartbreak-hill.html:129-131`):
```html
          <div class="panel result">
            <span class="label">{{ 'HEARTBREAK_HILL.RESULT_LABEL' | translate }}</span>
            <div class="pl">#{{ r.rank }}</div>
```
Nachher:
```html
          <div class="panel result">
            <span class="label">{{ 'HEARTBREAK_HILL.RESULT_LABEL' | translate }}</span>
            @if (restored()) {
              <div class="restored">
                <span>{{ 'HEARTBREAK_HILL.RESTORED_HINT' | translate }}@if (restoredAt(); as ts) { · {{ ts | date:'mediumDate' }} }</span>
                <button type="button" class="restored-discard" (click)="discardStored()">{{ 'HEARTBREAK_HILL.RESTORED_DISCARD' | translate }}</button>
              </div>
            }
            <div class="pl">#{{ r.rank }}</div>
```

(`DatePipe` ist über das bereits importierte `CommonModule` verfügbar.)

- [ ] **Step 2: Add the styles**

Vorher (`heartbreak-hill.scss:74`):
```scss
.result .meta b { font-size: 22px; font-weight: 900; font-variant-numeric: tabular-nums; }
```
Nachher:
```scss
.result .meta b { font-size: 22px; font-weight: 900; font-variant-numeric: tabular-nums; }
.result .restored { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 8px; font-size: 12px; color: var(--text-muted); }
.result .restored-discard { background: none; border: none; padding: 0; color: var(--text-muted); text-decoration: underline; cursor: pointer; font: inherit; }
.result .restored-discard:hover { color: var(--pp); }
```

- [ ] **Step 3: Verify it compiles**

Run: `cd frontend && npm run build`
Expected: Build erfolgreich (Template-Bindings `restored()`, `restoredAt()`, `discardStored()` und beide i18n-Keys auflösbar).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.html frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss
git commit -m "$(cat <<'EOF'
Show a "remembered" hint and discard action in the result panel

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Verifikation, Version & Changelog

**Files:**
- Modify: `pom.xml`, `frontend/package.json` (über das Bump-Script)
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Run the full unit-test suite**

Run: `cd frontend && npm test`
Expected: PASS — inkl. `heartbreak-storage.util` und allen bestehenden Suiten.

- [ ] **Step 2: Manual verification (dev server)**

Run: `cd frontend && npm start` → Browser auf `http://localhost:4200/heartbreak-hill`.
Prüfen:
1. GPX (Rad) hochladen → Result-Panel + Story-Vorschau erscheinen; **kein** „gemerkt"-Hinweis (frischer Upload).
2. Seite neu laden (F5) → Result-Panel + Vorschau erscheinen automatisch wieder, jetzt **mit** „Von deinem letzten Upload gemerkt"-Hinweis.
3. Auf den RUN-Tab wechseln (ohne RUN-Upload) → Panel verschwindet; zurück auf RIDE → gemerktes Ergebnis wieder da.
4. „Verwerfen" klicken → Panel schließt; nach F5 bleibt es leer.
5. (Optional) zweiten Rad-Upload machen → überschreibt den Snapshot; nach F5 erscheint der neue.

- [ ] **Step 3: Bump the version (minor)**

Run (Git Bash, Projekt-Root): `./version-bump.sh minor`
Expected: `pom.xml` und `frontend/package.json` werden von `0.51.0` auf `0.52.0` angehoben.

- [ ] **Step 4: Update the changelog**

In `CHANGELOG.md` unter `## [Unreleased]` im `### Added`-Abschnitt ergänzen (Abschnitt anlegen, falls nicht vorhanden):

```markdown
- Heartbreak Hill Challenge now remembers your upload per activity type in the browser (localStorage); your result and shareable story image reappear automatically when you return to the page, with a discard option.
```

- [ ] **Step 5: Final commit**

```bash
git add pom.xml frontend/package.json CHANGELOG.md
git commit -m "$(cat <<'EOF'
Heartbreak Hill remember upload: bump minor version and update changelog

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 6: Push**

Run: `git push`
Expected: Commits landen auf `origin/main` (direkt auf `main`, kein PR — Projektkonvention).

---

## Self-Review

**Spec coverage:**
- localStorage statt Cookie → Task 1 (`STORAGE_KEY`, reine Util). ✓
- Snapshot, kein Backend → keine Backend-Tasks; `EffortResult` unverändert übernommen. ✓
- Pro Typ getrennt → `StoredEfforts = Partial<Record<ActivityType, …>>`, Test „keeps RIDE and RUN independent" (Task 1), `restoreForCurrentTab()` nutzt `activeTab()` (Task 3). ✓
- Auto-Reveal → `ngOnInit` ruft `restoreForCurrentTab()` (Task 3). ✓
- „Gemerkt"-Hinweis + Verwerfen → `restored()`-Block + `discardStored()` (Tasks 3+4), i18n (Task 2). ✓
- Robustheit (kein localStorage / korrupt) → `try/catch` + `isValidEntry`, Tests „corrupt JSON" / „setItem fails" (Task 1). ✓
- Reihenfolge Reload nach Challenge-Load → Step 3 in Task 3 explizit. ✓
- Version minor + Changelog → Task 5. ✓

**Placeholder scan:** Keine TBD/TODO/„handle errors"; jeder Code-Schritt enthält vollständigen Code bzw. exakte Vorher/Nachher-Blöcke. ✓

**Type/name consistency:** `loadStoredEffort`/`saveStoredEffort`/`clearStoredEffort`, `StoredEffort.{result,displayName,savedAt}`, `restored`/`restoredAt`/`restoreForCurrentTab`/`discardStored` über alle Tasks identisch; `STORAGE_KEY`-Literal in Test (Task 1) == Util-Konstante. ✓
