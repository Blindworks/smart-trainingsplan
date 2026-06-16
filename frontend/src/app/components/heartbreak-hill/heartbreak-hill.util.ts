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
