export type ActivityType = 'RIDE' | 'RUN';
export type Gender = 'MALE' | 'FEMALE' | 'DIVERS';
export type LeaderboardScope = 'OVERALL' | 'MEN' | 'WOMEN' | 'MOST_ATTEMPTS';

/** Ironman 5-year age-group keys (must match backend AgeGroup.getKey()). */
export const AGE_GROUPS: string[] = [
  'U18', '18-24', '25-29', '30-34', '35-39', '40-44',
  '45-49', '50-54', '55-59', '60-64', '65-69', '70+'
];

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
  gender: string | null;
  ageGroup: string | null;
  attemptCount: number;
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
  avgSpeedKmh: number | null;
  avgPaceSecondsPerKm: number | null;
  status: string;
}

export interface EffortTrack {
  effortId: number;
  activityType: ActivityType;
  trackJson: string;              // JSON: [[lat,lng,ele,relSec], ...]
}
