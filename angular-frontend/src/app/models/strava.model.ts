export interface StravaStatus {
  connected: boolean;
  athleteName?: string;
  athleteCity?: string;
  profileMedium?: string;
}

export interface StravaActivity {
  id: number;
  name: string;
  type: string;
  startDate: string;
  distanceMeters: number;
  movingTimeSeconds: number;
  totalElevationGain: number;
  averageSpeed: number;
  averageHeartrate?: number;
  averageWatts?: number;
}
