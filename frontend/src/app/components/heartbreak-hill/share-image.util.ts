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
