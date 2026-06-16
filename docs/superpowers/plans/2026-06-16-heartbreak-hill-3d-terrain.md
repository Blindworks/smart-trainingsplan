# Heartbreak Hill Challenge — 3D WebGL Terrain (Plan 3/3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the 2D hero of `/heartbreak-hill` with a real-time **WebGL 3D terrain** of the climb (Three.js): a procedurally-lofted hillside generated from the segment's elevation profile, a glowing brand-green route draped over it, an auto-orbiting camera, and a marker that climbs the route. The existing 2D SVG elevation profile stays as the automatic fallback when WebGL is unavailable or Three fails to initialise.

**Architecture:** A new encapsulated standalone child component `Heartbreak3d` (its own canvas + Three.js scene) is mounted inside the hero of the existing `HeartbreakHill` component when WebGL is supported, otherwise the current 2D SVG renders. The terrain geometry is produced by a **pure, THREE-free, unit-tested** helper (`heartbreak-3d.util.ts`) from the route polyline (`[lat,lng,ele][]`) — or a synthetic ridge when no polyline is seeded yet — so the heavy Three.js code stays free of testable math. Three.js lives only in the lazy `/heartbreak-hill` chunk (the route is already lazy-loaded), keeping it out of the initial bundle. A baked-DEM upgrade path is left open (the helper can later consume a real heightmap) but is **not** built here.

**Decision (from planning):** terrain geometry is **procedural from the elevation profile** now; real DEM baking is a future data step, not in this plan.

**Tech Stack:** Angular 21 (standalone, signals), Three.js (new dependency), TypeScript, Vitest (for the pure helper — the project uses **Vitest**, not Karma; use `toBe(...)` matchers and `npm test -- --watch=false`). All DOM/WebGL access stays inside `ngAfterViewInit`/browser lifecycle (no top-level `window`/WebGL access — safe for any build-time prerender).

**Commands run from `frontend/`:** `npm run build` (the compile + bundle gate), `npm test -- --watch=false` (Vitest, jsdom — no browser).

---

## File Structure

**New files**
- `frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.util.ts` — pure terrain-grid + route-curve builder + `isWebglAvailable()`.
- `frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.util.spec.ts` — unit tests for the builder.
- `frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.ts` — the Three.js component.
- `frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.scss` — canvas styling.

**Modified files**
- `frontend/package.json` (+ `package-lock.json`) — add `three` + `@types/three`.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts` — `polylinePoints` computed, `use3d` signal, import the child.
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html` — mount `<app-heartbreak-3d>` in the hero with the 2D SVG as fallback.
- `CHANGELOG.md` — add the 3D bullet under `[Unreleased]`.

---

## Task 1: Add the Three.js dependency

**Files:** Modify `frontend/package.json`, `frontend/package-lock.json`

- [ ] **Step 1: Install Three.js + types**

Run: `cd frontend && npm install three@^0.169.0 && npm install --save-dev @types/three@^0.169.0`
(If `^0.169.0` is unavailable, install the latest stable `three` and the matching `@types/three` major.)

> **Network required.** If npm cannot reach the registry in this environment, STOP and report BLOCKED — the rest of the plan depends on this.

- [ ] **Step 2: Verify the build still succeeds (Three installed but not yet imported)**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS (pre-existing budget warnings are fine; Three is not in any bundle yet because nothing imports it).

- [ ] **Step 3: Commit**

```bash
git add frontend/package.json frontend/package-lock.json
git commit -m "Add three.js dependency for Heartbreak Hill 3D terrain"
```

---

## Task 2: Pure terrain helper (TDD)

**Files:**
- Create: `frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.util.ts`
- Test: `frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.util.spec.ts`

The helper turns a `[lat,lng,ele][]` polyline into a heightfield grid matching a Three.js `PlaneGeometry(width, depth, segX, segZ)` vertex order (row-major: `index = iz*(segX+1) + ix`), plus the route centerline as `[x,y,z]` points. THREE-free so it unit-tests in jsdom.

- [ ] **Step 1: Write the failing test**

```ts
import { buildTerrainGrid, isWebglAvailable } from './heartbreak-3d.util';

describe('heartbreak-3d.util', () => {
  describe('buildTerrainGrid', () => {
    it('produces a heightfield matching PlaneGeometry vertex count', () => {
      const grid = buildTerrainGrid(null, 8, 4);   // synthetic, 8x4 segments
      expect(grid.segX).toBe(8);
      expect(grid.segZ).toBe(4);
      expect(grid.heights.length).toBe((8 + 1) * (4 + 1)); // 45
      expect(grid.routeXYZ.length).toBe(8 + 1);             // one route point per X column
      expect(grid.width).toBeGreaterThan(0);
      expect(grid.depth).toBeGreaterThan(0);
    });

    it('maps a rising elevation profile to a rising route (last point higher than first)', () => {
      const poly: [number, number, number][] = [
        [50.0, 8.0, 100], [50.001, 8.0, 130], [50.002, 8.0, 175]
      ];
      const grid = buildTerrainGrid(poly, 8, 4);
      const firstY = grid.routeXYZ[0][1];
      const lastY = grid.routeXYZ[grid.routeXYZ.length - 1][1];
      expect(lastY).toBeGreaterThan(firstY);
    });

    it('makes the center row the ridge (higher than an edge row at the same column)', () => {
      const grid = buildTerrainGrid(null, 8, 4);
      const segX = 8, segZ = 4;
      const col = segX;                       // the highest column of the synthetic ridge (peak at the end)
      const centerRow = segZ / 2;             // 2
      const edgeRow = 0;
      const centerH = grid.heights[centerRow * (segX + 1) + col];
      const edgeH = grid.heights[edgeRow * (segX + 1) + col];
      expect(centerH).toBeGreaterThan(edgeH);
    });

    it('returns a synthetic grid (no throw) for a too-short polyline', () => {
      expect(() => buildTerrainGrid([[50, 8, 100]], 8, 4)).not.toThrow();
      const grid = buildTerrainGrid([], 8, 4);
      expect(grid.heights.length).toBe(45);
    });
  });

  describe('isWebglAvailable', () => {
    it('returns a boolean without throwing (false in jsdom)', () => {
      expect(typeof isWebglAvailable()).toBe('boolean');
    });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- --watch=false --include="**/heartbreak-3d.util.spec.ts"`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Implement the helper**

```ts
export interface TerrainGrid {
  width: number;
  depth: number;
  segX: number;
  segZ: number;
  /** Heights per vertex, row-major: index = iz*(segX+1) + ix. World-Y heights. */
  heights: number[];
  /** Route centerline as [x, y, z] world coords, one per X column (length segX+1). */
  routeXYZ: number[][];
}

const WIDTH = 120;     // world units along the climb (X)
const DEPTH = 60;      // world units across the slope (Z)
const MAX_HEIGHT = 26; // world units of vertical relief

/** A synthetic monotonic-ish ascending profile in 0..1 (peak near the end). */
function syntheticProfile(n: number): number[] {
  const out: number[] = [];
  for (let i = 0; i < n; i++) {
    const x = i / (n - 1);
    // ease-in ramp with a small steep "heartbreak" kick near 0.7
    const base = Math.pow(x, 1.5);
    const kick = 0.12 * Math.exp(-Math.pow((x - 0.72) / 0.12, 2));
    out.push(Math.min(1, base + kick));
  }
  return out;
}

/** Normalised 0..1 elevation profile sampled to (segX+1) columns from the polyline. */
function profileFromPolyline(polyline: [number, number, number][] | null, cols: number): number[] {
  if (!polyline || polyline.length < 2) {
    return syntheticProfile(cols);
  }
  const eles = polyline.map(p => p[2] ?? 0);
  const minE = Math.min(...eles);
  const maxE = Math.max(...eles);
  const span = maxE - minE || 1;
  const out: number[] = [];
  for (let c = 0; c < cols; c++) {
    const t = c / (cols - 1);
    const idx = t * (polyline.length - 1);
    const i0 = Math.floor(idx);
    const i1 = Math.min(polyline.length - 1, i0 + 1);
    const frac = idx - i0;
    const e = eles[i0] + (eles[i1] - eles[i0]) * frac;
    out.push((e - minE) / span);
  }
  return out;
}

/** Lateral falloff: 1 at the center row, easing to ~0.25 at the edges. */
function crossFalloff(iz: number, segZ: number): number {
  const t = Math.abs(iz / segZ - 0.5) * 2; // 0 center .. 1 edge
  return 0.25 + 0.75 * Math.cos((t * Math.PI) / 2);
}

/**
 * Builds a heightfield + route centerline from a route polyline (or a synthetic
 * ridge when absent). Pure and deterministic — no THREE, no DOM.
 */
export function buildTerrainGrid(
  polyline: [number, number, number][] | null, segX = 96, segZ = 48
): TerrainGrid {
  const cols = segX + 1;
  const rows = segZ + 1;
  const profile = profileFromPolyline(polyline, cols);

  const heights: number[] = new Array(cols * rows);
  for (let iz = 0; iz < rows; iz++) {
    const fall = crossFalloff(iz, segZ);
    for (let ix = 0; ix < cols; ix++) {
      heights[iz * cols + ix] = profile[ix] * MAX_HEIGHT * fall;
    }
  }

  const routeXYZ: number[][] = [];
  for (let ix = 0; ix < cols; ix++) {
    const x = -WIDTH / 2 + (ix / segX) * WIDTH;
    const y = profile[ix] * MAX_HEIGHT + 0.6; // sit just above the ridge
    routeXYZ.push([x, y, 0]);                  // center row → world z = 0
  }

  return { width: WIDTH, depth: DEPTH, segX, segZ, heights, routeXYZ };
}

/** True when a WebGL context can be created. Safe in non-browser/jsdom (returns false). */
export function isWebglAvailable(): boolean {
  try {
    if (typeof document === 'undefined') return false;
    const canvas = document.createElement('canvas');
    return !!(canvas.getContext('webgl2') || canvas.getContext('webgl'));
  } catch {
    return false;
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- --watch=false --include="**/heartbreak-3d.util.spec.ts"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.util.ts frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.util.spec.ts
git commit -m "Add pure procedural terrain-grid helper for 3D hero"
```

---

## Task 3: `Heartbreak3d` Three.js component

**Files:**
- Create: `frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.ts`
- Create: `frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.scss`

- [ ] **Step 1: Create the component**

```ts
import {
  AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone,
  OnDestroy, Output, ViewChild, inject
} from '@angular/core';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { buildTerrainGrid, isWebglAvailable } from './heartbreak-3d.util';

@Component({
  selector: 'app-heartbreak-3d',
  standalone: true,
  template: `<canvas #canvas class="hb3d-canvas"></canvas>`,
  styleUrl: './heartbreak-3d.scss'
})
export class Heartbreak3d implements AfterViewInit, OnDestroy {
  /** Route polyline [lat,lng,ele][]; null/short → synthetic ridge. */
  @Input() polyline: [number, number, number][] | null = null;
  /** Emitted when WebGL is unavailable or init fails — parent shows the 2D fallback. */
  @Output() fallback = new EventEmitter<void>();

  @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  private readonly host = inject(ElementRef);
  private readonly zone = inject(NgZone);

  private renderer?: THREE.WebGLRenderer;
  private scene?: THREE.Scene;
  private camera?: THREE.PerspectiveCamera;
  private controls?: OrbitControls;
  private marker?: THREE.Mesh;
  private curve?: THREE.CatmullRomCurve3;
  private frameId = 0;
  private readonly clock = new THREE.Clock();
  private resizeObs?: ResizeObserver;

  ngAfterViewInit(): void {
    if (!isWebglAvailable()) {
      this.fallback.emit();
      return;
    }
    try {
      this.zone.runOutsideAngular(() => this.init());
    } catch {
      this.dispose();
      this.fallback.emit();
    }
  }

  private init(): void {
    const canvas = this.canvasRef.nativeElement;
    const el = this.host.nativeElement as HTMLElement;
    const w = el.clientWidth || 1200;
    const h = el.clientHeight || 600;

    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    this.renderer.setSize(w, h, false);

    this.scene = new THREE.Scene();
    this.scene.fog = new THREE.FogExp2(0x0d1117, 0.013);

    this.camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 1000);
    this.camera.position.set(10, 40, 78);

    this.scene.add(new THREE.AmbientLight(0xffffff, 0.55));
    const key = new THREE.DirectionalLight(0x8ffc2e, 1.15);
    key.position.set(-40, 70, 50);
    this.scene.add(key);
    const rim = new THREE.DirectionalLight(0x3fb0ff, 0.4);
    rim.position.set(60, 30, -40);
    this.scene.add(rim);

    // --- terrain ---
    const grid = buildTerrainGrid(this.polyline, 96, 48);
    const geo = new THREE.PlaneGeometry(grid.width, grid.depth, grid.segX, grid.segZ);
    geo.rotateX(-Math.PI / 2);
    const pos = geo.attributes['position'] as THREE.BufferAttribute;
    for (let i = 0; i < grid.heights.length; i++) {
      pos.setY(i, grid.heights[i]);
    }
    pos.needsUpdate = true;
    geo.computeVertexNormals();

    const terrain = new THREE.Mesh(
      geo,
      new THREE.MeshStandardMaterial({ color: 0x12331a, flatShading: true, metalness: 0, roughness: 1 })
    );
    this.scene.add(terrain);

    const wire = new THREE.Mesh(
      geo,
      new THREE.MeshBasicMaterial({ color: 0x8ffc2e, wireframe: true, transparent: true, opacity: 0.07 })
    );
    this.scene.add(wire);

    // --- glowing route + climbing marker ---
    const points = grid.routeXYZ.map(p => new THREE.Vector3(p[0], p[1], p[2]));
    if (points.length >= 2) {
      this.curve = new THREE.CatmullRomCurve3(points);
      const tube = new THREE.Mesh(
        new THREE.TubeGeometry(this.curve, 220, 0.5, 8, false),
        new THREE.MeshBasicMaterial({ color: 0x8ffc2e })
      );
      this.scene.add(tube);

      this.marker = new THREE.Mesh(
        new THREE.SphereGeometry(1.3, 16, 16),
        new THREE.MeshBasicMaterial({ color: 0xffffff })
      );
      this.scene.add(this.marker);
    }

    this.controls = new OrbitControls(this.camera, canvas);
    this.controls.enableZoom = false;
    this.controls.enablePan = false;
    this.controls.enableDamping = true;
    this.controls.autoRotate = true;
    this.controls.autoRotateSpeed = 0.55;
    this.controls.minPolarAngle = Math.PI * 0.18;
    this.controls.maxPolarAngle = Math.PI * 0.46;
    this.controls.target.set(0, 8, 0);
    this.controls.update();

    this.resizeObs = new ResizeObserver(() => this.onResize());
    this.resizeObs.observe(el);

    const animate = () => {
      this.frameId = requestAnimationFrame(animate);
      if (this.marker && this.curve) {
        const t = (this.clock.getElapsedTime() * 0.11) % 1;
        this.marker.position.copy(this.curve.getPointAt(t));
      }
      this.controls?.update();
      this.renderer!.render(this.scene!, this.camera!);
    };
    animate();
  }

  private onResize(): void {
    const el = this.host.nativeElement as HTMLElement;
    const w = el.clientWidth;
    const h = el.clientHeight;
    if (!w || !h || !this.renderer || !this.camera) {
      return;
    }
    this.renderer.setSize(w, h, false);
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
  }

  ngOnDestroy(): void {
    this.dispose();
  }

  private dispose(): void {
    cancelAnimationFrame(this.frameId);
    this.resizeObs?.disconnect();
    this.controls?.dispose();
    this.scene?.traverse(obj => {
      const mesh = obj as THREE.Mesh;
      mesh.geometry?.dispose?.();
      const mat = mesh.material as THREE.Material | THREE.Material[] | undefined;
      if (Array.isArray(mat)) {
        mat.forEach(m => m.dispose());
      } else {
        mat?.dispose?.();
      }
    });
    this.renderer?.dispose();
  }
}
```

- [ ] **Step 2: Create the styles**

```scss
:host { display: block; width: 100%; height: 100%; }
.hb3d-canvas { display: block; width: 100%; height: 100%; }
```

- [ ] **Step 3: Verify it compiles**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS. Three.js now appears in the lazy `heartbreak-hill` chunk; a bundle-size budget WARNING for that chunk is acceptable (warnings don't fail the build — confirmed by prior builds). If the build ERRORS on a budget, raise the relevant `bundle`/lazy budget threshold in `frontend/angular.json` minimally (do not change behaviour), and report it.

> If the build errors with `Could not resolve "three/addons/controls/OrbitControls.js"`, use the alternative import path `three/examples/jsm/controls/OrbitControls.js` (older Three layout) and report which one worked.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.ts frontend/src/app/components/heartbreak-hill/heartbreak-3d/heartbreak-3d.scss
git commit -m "Add Heartbreak3d WebGL terrain component"
```

---

## Task 4: Mount 3D in the hero with the 2D fallback

**Files:**
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts`
- Modify: `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html`

- [ ] **Step 1: Extend the component class**

In `heartbreak-hill.ts`:

1. Add imports at the top:
```ts
import { Heartbreak3d } from './heartbreak-3d/heartbreak-3d';
import { isWebglAvailable } from './heartbreak-3d/heartbreak-3d.util';
```
2. Add `Heartbreak3d` to the `@Component` `imports` array (after `TranslateModule`).
3. Add these members to the class (near the other signals):
```ts
  /** Whether to attempt the WebGL hero (falls back to 2D on failure). */
  use3d = signal(isWebglAvailable());

  /** Parsed route polyline points, shared by the 2D profile and the 3D terrain. */
  polylinePoints = computed<[number, number, number][] | null>(() => {
    const c = this.challenge();
    if (!c?.polylineJson) {
      return null;
    }
    try {
      return JSON.parse(c.polylineJson) as [number, number, number][];
    } catch {
      return null;
    }
  });
```
4. (Optional cleanup) the existing `profile` computed may now derive from `polylinePoints()` instead of re-parsing; if you change it, keep its output identical. This is not required — leave `profile` as-is if simpler.

- [ ] **Step 2: Mount the 3D component in the hero**

In `heartbreak-hill.html`, replace the `<div class="hero-bg"> … </div>` block (the SVG block) with a version that shows 3D when enabled and the existing SVG otherwise:

```html
      <div class="hero-bg">
        @if (use3d()) {
          <app-heartbreak-3d [polyline]="polylinePoints()" (fallback)="use3d.set(false)"></app-heartbreak-3d>
        } @else {
          <svg [attr.viewBox]="'0 0 ' + profileWidth + ' ' + profileHeight" preserveAspectRatio="xMidYMax slice">
            @if (profile(); as p) {
              <path [attr.d]="p.area" class="hero-area"></path>
              <path [attr.d]="p.line" class="hero-line"></path>
            } @else {
              <path class="hero-area" d="M0,360 C300,330 600,250 900,160 C1050,120 1150,100 1200,92 L1200,360 Z"></path>
              <path class="hero-line" d="M0,330 C300,300 600,220 900,130 C1050,90 1150,72 1200,64"></path>
            }
          </svg>
        }
        <div class="hero-veil"></div>
      </div>
```

(The `.hero-bg` is already `position:absolute; inset:0`; the 3D canvas fills it via the child's `:host { width:100%; height:100% }`. The `.hero-veil` gradient stays on top for text legibility.)

- [ ] **Step 3: Build gate**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS (strict templates pass; `app-heartbreak-3d` resolves; budget warning acceptable).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts frontend/src/app/components/heartbreak-hill/heartbreak-hill.html
git commit -m "Use 3D WebGL hero on Heartbreak Hill page with 2D fallback"
```

---

## Task 5: Tests, changelog

- [ ] **Step 1: Run the full frontend test suite (Vitest)**

Run: `cd frontend && npm test -- --watch=false`
Expected: the new `heartbreak-3d.util.spec.ts` passes alongside the existing Heartbreak Hill specs. (The pre-existing broken `app.spec.ts` is tracked separately and is not part of this work — note it if it still fails, do not fix it here.)

- [ ] **Step 2: Best-effort visual smoke (optional, honest)**

If the full stack can be brought up (backend with `PACR_AI_API_KEY` dummy + MariaDB + `ng serve`), open `http://localhost:4200/heartbreak-hill` anonymously and confirm the 3D hero renders and auto-orbits (and that forcing `use3d=false` shows the 2D profile). If the stack/Chromium can't run here, report "not run" with the reason — the build + unit tests are the gates.

- [ ] **Step 3: Changelog**

Add a third bullet under the existing `[Unreleased] / ### Added` Heartbreak Hill block in `CHANGELOG.md` (version stays `0.50.0`, no bump):

```markdown
- Heartbreak Hill 3D hero: real-time WebGL terrain (Three.js) lofted from the climb's elevation profile, with a glowing route, an auto-orbiting camera and a climbing marker; automatic 2D fallback when WebGL is unavailable.
```

- [ ] **Step 4: Commit**

```bash
git add CHANGELOG.md
git commit -m "Document Heartbreak Hill 3D hero in changelog"
```

---

## Self-Review

**Spec coverage** (spec §7 "3D-Terrain (Phase 2)"):
- Real WebGL terrain via Three.js, encapsulated lazy component — Tasks 1/3. ✓
- Glowing route on the slope + climbing marker + auto-orbit — Task 3. ✓
- Automatic 2D fallback when WebGL unavailable / init fails — Tasks 3 (`fallback` output, `isWebglAvailable`) + 4 (`use3d` branch). ✓
- Terrain from the climb's data (procedural from the elevation profile per the planning decision; baked-DEM upgrade slot left open via the helper) — Task 2. ✓

**Deferred (correct, not in this plan):** baked real-DEM heightmap (needs real coordinates + offline pipeline — a data step); the interactive multi-ghost "duel" (pick a pro, scrub a timeline) from the spec's Phase 3 stretch — only a single climbing marker is built here; share/badge image generation.

**Type consistency:** `buildTerrainGrid(polyline, segX, segZ)` returns `TerrainGrid` consumed by the component; `heights` length `(segX+1)*(segZ+1)` matches `PlaneGeometry(width, depth, segX, segZ)` row-major vertex order; `routeXYZ` is `number[][]` converted to `THREE.Vector3` in the component. `isWebglAvailable()` shared by helper test, component, and parent. `polyline: [number,number,number][] | null` input matches `polylinePoints()` output in the parent.

**Risk notes:** (1) Task 1 needs network for `npm install` — BLOCKED if offline. (2) Three.js enlarges the lazy chunk → budget WARNING (not error) expected; only touch `angular.json` budgets if it errors. (3) All WebGL/DOM access is inside `ngAfterViewInit` (browser-only) — no top-level `window` access, so any build-time prerender stays safe. (4) jsdom has no WebGL, so the component itself is build-verified, not unit-tested; the testable math is isolated in the pure helper.

**Placeholder scan:** the synthetic ridge for an absent polyline is intentional graceful behaviour (real polyline seeded later), not a TODO. No `TODO`/`TBD` in code.
