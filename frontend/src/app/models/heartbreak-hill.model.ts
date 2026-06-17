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
  avgSpeedKmh: number | null;
  avgPaceSecondsPerKm: number | null;
  status: string;
}

export interface EffortTrack {
  effortId: number;
  activityType: ActivityType;
  trackJson: string;              // JSON: [[lat,lng,ele,relSec], ...]
}
