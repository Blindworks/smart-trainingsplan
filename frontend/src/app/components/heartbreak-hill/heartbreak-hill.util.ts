/** Formats a gap (seconds behind the leader) as "+m:ss"; 0 or null → em dash. */
export function formatGap(seconds: number | null): string {
  if (seconds === null || seconds === 0) {
    return '—';
  }
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `+${m}:${s < 10 ? '0' + s : s}`;
}

/**
 * Maps an upload failure (HTTP status + optional backend `reason`) to an i18n key suffix
 * under `HEARTBREAK_HILL.*`. Rate limits and duplicate-file rejections get their own message;
 * everything else falls back to the generic upload error.
 */
export function uploadErrorKey(status: number, reason?: string | null): string {
  if (status === 429) {
    return 'ERROR_RATE_LIMIT';
  }
  if (status === 422 && reason === 'duplicate_file') {
    return 'ERROR_DUPLICATE_FILE';
  }
  return 'ERROR_UPLOAD';
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
