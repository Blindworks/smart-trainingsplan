export interface DashboardLoadStatus {
  acwr: number;
  flag: 'BLUE' | 'GREEN' | 'ORANGE' | 'RED';
}

export interface DashboardTrendPoint {
  date: string;
  strain21: number;
}

export interface DashboardEfTrendPoint {
  date: string;
  ef: number;
}

export interface DashboardDriftTrendPoint {
  date: string;
  driftPct: number;
}

export interface DashboardLastRun {
  date: string;
  strain21: number;
  driftPct: number;
  z4Min: number;
  z5Min: number;
  coachBullets: string[];
}

export interface DashboardDto {
  strain21: number;
  readinessScore: number;
  readinessRecommendation: string;
  loadStatus: DashboardLoadStatus;
  loadTrend: DashboardTrendPoint[];
  efTrend: DashboardEfTrendPoint[];
  driftTrend: DashboardDriftTrendPoint[];
  lastRun: DashboardLastRun;
}
