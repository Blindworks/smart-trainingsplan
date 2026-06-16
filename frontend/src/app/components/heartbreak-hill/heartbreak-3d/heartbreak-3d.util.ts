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
