import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { BodyMetric, DailyMetrics, DecouplingHistoryPoint, Vo2MaxHistoryPoint, CurrentRaceTimePredictions } from '../../models/competition.model';
import { catchError, of } from 'rxjs';

interface BarChartPoint {
  date: string;
  dayLabel: string;
  label: string;
  showLabel: boolean;
  value: number | null;
  pct: number;
  color: string;
  tooltip: string;
}

interface ChartStats {
  avg: number;
  max: number;
  sessions: number;
}

interface RaceTimeChartPoint {
  x: number;
  y: number;
  date: string;
  label: string;
  timeFormatted: string;
  timeSeconds: number;
  showLabel: boolean;
  vo2max: number;
}

@Component({
  selector: 'app-body-status',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './body-status.component.html',
  styleUrl: './body-status.component.scss'
})
export class BodyStatusComponent implements OnInit {
  loading = true;
  recalculating = false;
  profileWarningMessage = '';
  profileMissingFields: string[] = [];
  private profileValidationBlocked = false;
  metrics: BodyMetric[] = [];
  todayStrain: number | null = null;
  todayStrainDate: string | null = null;

  strainChart: BarChartPoint[] = [];
  strainChartStats: ChartStats = { avg: 0, max: 0, sessions: 0 };
  strainHoveredIndex: number | null = null;

  trimpChart: BarChartPoint[] = [];
  trimpChartStats: ChartStats = { avg: 0, max: 0, sessions: 0 };
  trimpHoveredIndex: number | null = null;

  decouplingChart: BarChartPoint[] = [];
  decouplingChartStats = { avg: 0, best: 0, sessions: 0 };
  decouplingHoveredIndex: number | null = null;

  efChart: BarChartPoint[] = [];
  efChartStats: ChartStats = { avg: 0, max: 0, sessions: 0 };
  efHoveredIndex: number | null = null;

  // ACWR card state
  todayAcwr: number | null = null;
  todayAcwrFlag: string | null = null;
  todayAcwrDate: string | null = null;
  todayAcute7: number | null = null;
  todayAcwrChronic28: number | null = null;

  // ACWR chart
  acwrChart: BarChartPoint[] = [];
  acwrChartStats = { avg: 0, sessions: 0 };
  acwrHoveredIndex: number | null = null;

  // Readiness card state
  todayReadiness: number | null = null;
  todayRecommendation: string | null = null;
  todayReadinessReasons: string[] = [];
  todayReadinessDate: string | null = null;

  // Coach card state
  todayCoachTitle: string | null = null;
  todayCoachBullets: string[] = [];

  // Readiness chart
  readinessChart: BarChartPoint[] = [];
  readinessChartStats = { avg: 0, sessions: 0 };
  readinessHoveredIndex: number | null = null;

  // Context-aware (multi-factor) race predictions
  currentRacePredictions: CurrentRaceTimePredictions | null = null;

  // Race time predictions (history chart)
  vo2maxHistory: Vo2MaxHistoryPoint[] = [];
  selectedDistance = '5km';
  /** Raw measurement dots — used for hover interaction */
  raceTimeChartPoints: RaceTimeChartPoint[] = [];
  /** Smoothed trend line path (moving average) */
  raceTimeLinePath = '';
  /** Fill area below the trend line */
  raceTimeFillPath = '';
  raceTimeHoveredIndex: number | null = null;
  raceTimeCurrentPrediction: string | null = null;
  raceTimeBestPrediction: string | null = null;
  raceTrendDirection: 'BESSER' | 'STABIL' | 'SCHLECHTER' | null = null;
  selectedTimeRange = '1Y';
  readonly distanceOptions = [
    { key: '1km',         label: '1 km',       color: '#42a5f5' },
    { key: '5km',         label: '5 km',        color: '#66bb6a' },
    { key: '10km',        label: '10 km',       color: '#ffca28' },
    { key: 'Halbmarathon',label: 'Halbm.',      color: '#ff7043' },
    { key: 'Marathon',    label: 'Marathon',    color: '#ef5350' },
  ];
  readonly timeRangeOptions = [
    { key: '1W', label: '1 W' },
    { key: '1M', label: '1 M' },
    { key: '6M', label: '6 M' },
    { key: '1Y', label: '1 J' },
  ];

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadMetrics();
    this.loadDailyHistory();
    this.loadDecouplingHistory();
    this.loadVo2MaxHistory();
    this.loadCurrentRacePredictions();
  }

  loadMetrics(): void {
    this.loading = true;
    this.apiService.getBodyMetrics().subscribe({
      next: (metrics) => {
        this.metrics = metrics;
        this.loading = false;
      },
      error: () => {
        this.metrics = [];
        this.loading = false;
      }
    });
  }

  private formatDateLocal(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  loadDailyHistory(): void {
    const today = new Date();
    const startDate = new Date(today);
    startDate.setDate(today.getDate() - 29);
    const start = this.formatDateLocal(startDate);
    const end = this.formatDateLocal(today);

    this.profileValidationBlocked = false;
    this.apiService.computeToday().pipe(
      catchError((error) => {
        const message = error?.error?.message;
        const missingFields = error?.error?.missingFields;
        if (Array.isArray(missingFields)) {
          this.profileValidationBlocked = true;
          this.profileMissingFields = missingFields;
          this.profileWarningMessage = message || 'Profil unvollstaendig fuer Metrik-Berechnungen.';
        } else if (message) {
          this.snackBar.open(message, 'Schliessen', { duration: 5000 });
        }
        return of(null);
      })
    ).subscribe(() => {
      this.apiService.getDailyMetrics(start, end).subscribe({
        next: (metrics: DailyMetrics[]) => {
          if (!this.profileValidationBlocked) {
            this.profileWarningMessage = '';
            this.profileMissingFields = [];
          }
          // Most recent day with strain data
          const sorted = metrics
            .filter(m => m.dailyStrain21 != null)
            .sort((a, b) => b.date.localeCompare(a.date));
          if (sorted.length > 0) {
            this.todayStrain = sorted[0].dailyStrain21!;
            this.todayStrainDate = sorted[0].date;
          }
          this.buildStrainChart(metrics, startDate);
          this.buildTrIMPChart(metrics, startDate);
          this.buildEfChart(metrics, startDate);
          // Most recent ACWR
          const sortedAcwr = metrics
            .filter(m => m.acwr != null)
            .sort((a, b) => b.date.localeCompare(a.date));
          if (sortedAcwr.length > 0) {
            const latest = sortedAcwr[0];
            this.todayAcwr = latest.acwr ?? null;
            this.todayAcwrFlag = latest.acwrFlag ?? null;
            this.todayAcwrDate = latest.date;
            this.todayAcute7 = latest.acute7 ?? null;
            this.todayAcwrChronic28 = latest.chronic28 ?? null;
          }
          this.buildAcwrChart(metrics, startDate);
          // Most recent readiness
          const sortedReadiness = metrics
            .filter(m => m.readinessScore != null)
            .sort((a, b) => b.date.localeCompare(a.date));
          if (sortedReadiness.length > 0) {
            const latest = sortedReadiness[0];
            this.todayReadiness = latest.readinessScore ?? null;
            this.todayRecommendation = latest.recommendation ?? null;
            this.todayReadinessDate = latest.date;
            try {
              this.todayReadinessReasons = latest.reasonsJson ? JSON.parse(latest.reasonsJson) : [];
            } catch {
              this.todayReadinessReasons = [];
            }
            this.todayCoachTitle = latest.coachTitle ?? null;
            try {
              this.todayCoachBullets = latest.coachBulletsJson ? JSON.parse(latest.coachBulletsJson) : [];
            } catch {
              this.todayCoachBullets = [];
            }
          }
          this.buildReadinessChart(metrics, startDate);
        },
        error: () => {}
      });
    });
  }

  private buildChart(
    metrics: DailyMetrics[],
    from: Date,
    getValue: (m: DailyMetrics) => number | undefined | null,
    yMax: number,
    getColor: (v: number) => string,
    tooltipLabel: string
  ): { points: BarChartPoint[]; stats: ChartStats } {
    const map = new Map<string, number>();
    let max = 0;
    let sum = 0;
    let count = 0;

    for (const m of metrics) {
      const v = getValue(m);
      if (v != null && v > 0) {
        const dateStr = typeof m.date === 'string' ? m.date : this.formatDateLocal(new Date(m.date));
        map.set(dateStr, v);
        if (v > max) max = v;
        sum += v;
        count++;
      }
    }

    const scale = Math.max(yMax, max);
    const points: BarChartPoint[] = [];
    const DAY_LABELS = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];

    for (let i = 0; i < 30; i++) {
      const d = new Date(from);
      d.setDate(from.getDate() + i);
      const dateStr = this.formatDateLocal(d);
      const value = map.get(dateStr) ?? null;
      const label = `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;
      const dayLabel = DAY_LABELS[d.getDay()];

      points.push({
        date: dateStr,
        dayLabel,
        label,
        showLabel: i % 7 === 0 || i === 29,
        value,
        pct: value != null ? Math.max((value / scale) * 100, 1) : 0,
        color: value != null ? getColor(value) : 'transparent',
        tooltip: value != null
          ? `${label}: ${tooltipLabel} ${value.toFixed(1)}`
          : `${label}: kein Training`
      });
    }

    return {
      points,
      stats: { avg: count > 0 ? Math.round(sum / count) : 0, max: Math.round(max), sessions: count }
    };
  }

  private buildStrainChart(metrics: DailyMetrics[], from: Date): void {
    const result = this.buildChart(
      metrics, from,
      m => m.dailyStrain21,
      21,
      v => this.getStrainColor(v),
      'Strain'
    );
    this.strainChart = result.points;
    this.strainChartStats = result.stats;
  }

  private buildTrIMPChart(metrics: DailyMetrics[], from: Date): void {
    const result = this.buildChart(
      metrics, from,
      m => m.dailyTrimp,
      200,
      v => this.getTRIMPBarColor(v),
      'TRIMP'
    );
    this.trimpChart = result.points;
    this.trimpChartStats = result.stats;
  }

  private buildEfChart(metrics: DailyMetrics[], from: Date): void {
    const result = this.buildChart(
      metrics, from,
      m => m.ef7 ?? null,
      0.04,
      v => this.getEfColor(v),
      'EF'
    );
    // Fix tooltip and stats precision for small EF values
    let sum = 0, max = 0, count = 0;
    result.points.forEach(p => {
      if (p.value !== null) {
        p.tooltip = p.value.toFixed(4);
        sum += p.value;
        if (p.value > max) max = p.value;
        count++;
      }
    });
    this.efChart = result.points;
    this.efChartStats = {
      avg: count > 0 ? Math.round(sum / count * 10000) / 10000 : 0,
      max: Math.round(max * 10000) / 10000,
      sessions: count
    };
  }

  get efChartHasData(): boolean {
    return this.efChartStats.sessions > 0;
  }

  getEfColor(ef: number): string {
    if (ef >= 0.030) return '#4caf50'; // sehr effizient
    if (ef >= 0.024) return '#8bc34a';
    if (ef >= 0.018) return '#ffca28';
    if (ef >= 0.013) return '#ff7043';
    return '#ef5350';                  // wenig effizient
  }

  get strainChartHasData(): boolean {
    return this.strainChartStats.sessions > 0;
  }

  get trimpChartHasData(): boolean {
    return this.trimpChartStats.sessions > 0;
  }

  getStrainColor(strain: number): string {
    if (strain >= 15) return '#ef5350';
    if (strain >= 10) return '#ff7043';
    if (strain >= 6)  return '#ffca28';
    return '#66bb6a';
  }

  getStrainCategory(strain: number): string {
    if (strain >= 15) return 'Sehr hoch';
    if (strain >= 10) return 'Hoch';
    if (strain >= 6)  return 'Moderat';
    return 'Leicht';
  }

  getTRIMPBarColor(trimp: number): string {
    if (trimp >= 150) return '#ef5350';
    if (trimp >= 100) return '#ff7043';
    if (trimp >= 50)  return '#ffca28';
    return '#66bb6a';
  }

  loadDecouplingHistory(): void {
    const today = new Date();
    const startDate = new Date(today);
    startDate.setDate(today.getDate() - 29);
    const start = this.formatDateLocal(startDate);
    const end = this.formatDateLocal(today);

    this.apiService.getDecouplingHistory(start, end).pipe(
      catchError(() => of([]))
    ).subscribe((points: DecouplingHistoryPoint[]) => {
      this.buildDecouplingChart(points, startDate);
    });
  }

  private buildDecouplingChart(points: DecouplingHistoryPoint[], from: Date): void {
    const MAX_PCT = 15;
    const DAY_LABELS = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];

    // Group by date (average if multiple activities on same day)
    const map = new Map<string, number[]>();
    for (const p of points) {
      const existing = map.get(p.date) ?? [];
      existing.push(p.decouplingPct);
      map.set(p.date, existing);
    }

    const chartPoints: BarChartPoint[] = [];
    let sum = 0, count = 0, best = Infinity;

    for (let i = 0; i < 30; i++) {
      const d = new Date(from);
      d.setDate(from.getDate() + i);
      const dateStr = this.formatDateLocal(d);
      const label = `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;
      const dayLabel = DAY_LABELS[d.getDay()];

      const values = map.get(dateStr);
      const value = values ? values.reduce((a, b) => a + b, 0) / values.length : null;

      if (value !== null) {
        sum += value;
        count++;
        if (value < best) best = value;
      }

      chartPoints.push({
        date: dateStr,
        dayLabel,
        label,
        showLabel: i % 7 === 0 || i === 29,
        value,
        pct: value !== null ? Math.max((Math.abs(value) / MAX_PCT) * 100, 1) : 0,
        color: value !== null ? this.getDecouplingBarColor(value) : 'transparent',
        tooltip: value !== null ? `${value >= 0 ? '+' : ''}${value.toFixed(1)}%` : ''
      });
    }

    this.decouplingChart = chartPoints;
    this.decouplingChartStats = {
      avg:      count > 0 ? Math.round(sum / count * 10) / 10 : 0,
      best:     best !== Infinity ? Math.round(best * 10) / 10 : 0,
      sessions: count
    };
  }

  get decouplingChartHasData(): boolean {
    return this.decouplingChartStats.sessions > 0;
  }

  getDecouplingBarColor(pct: number): string {
    if (pct < 0)  return '#42a5f5'; // blau  — negativ (gut)
    if (pct > 8)  return '#ef5350'; // rot   — starker Drift
    if (pct > 5)  return '#ff7043'; // orange
    if (pct > 3)  return '#ffca28'; // gelb
    return '#66bb6a';               // grün  — effizient
  }

  getDecouplingCategory(pct: number): string {
    if (pct < 0)  return 'Negativ (gut)';
    if (pct > 8)  return 'Starker Drift';
    if (pct > 5)  return 'Mäßiger Drift';
    if (pct > 3)  return 'Leichter Drift';
    return 'Effizient';
  }

  recalculate(): void {
    this.recalculating = true;
    this.apiService.recalculateBodyMetrics().subscribe({
      next: (result) => {
        this.recalculating = false;
        this.snackBar.open(
          `Neu berechnet aus ${result.activitiesProcessed} Aktivitäten`,
          'OK',
          { duration: 4000 }
        );
        this.loadMetrics();
        this.loadDailyHistory();
      },
      error: () => {
        this.recalculating = false;
        this.snackBar.open('Fehler bei der Neuberechnung', 'Schließen', { duration: 3000 });
      }
    });
  }

  getVo2MaxCategory(value: number): string {
    if (value >= 60) return 'Exzellent';
    if (value >= 52) return 'Sehr gut';
    if (value >= 44) return 'Gut';
    if (value >= 36) return 'Durchschnittlich';
    return 'Ausbaufähig';
  }

  getVo2MaxColor(value: number): string {
    if (value >= 60) return '#4caf50';
    if (value >= 52) return '#8bc34a';
    if (value >= 44) return '#ffb300';
    if (value >= 36) return '#ff7043';
    return '#ef5350';
  }

  getMetricColor(metric: BodyMetric): string {
    if (metric.metricType.startsWith('VO2MAX')) {
      return this.getVo2MaxColor(metric.value);
    }
    return 'var(--accent-blue)';
  }

  getMetricCategory(metric: BodyMetric): string {
    if (metric.metricType.startsWith('VO2MAX')) {
      return this.getVo2MaxCategory(metric.value);
    }
    return '';
  }

  getMetricIcon(metricType: string): string {
    switch (metricType) {
      case 'VO2MAX': return 'monitor_heart';
      case 'VO2MAX_HR_CORRECTED': return 'favorite';
      default: return 'analytics';
    }
  }

  getMetricIconColor(metricType: string): string {
    switch (metricType) {
      case 'VO2MAX': return 'rgba(76,175,80,0.16)';
      case 'VO2MAX_HR_CORRECTED': return 'rgba(229,57,53,0.14)';
      default: return 'rgba(45,123,255,0.12)';
    }
  }

  getMetricIconFgColor(metricType: string): string {
    switch (metricType) {
      case 'VO2MAX': return '#4caf50';
      case 'VO2MAX_HR_CORRECTED': return '#e53935';
      default: return 'var(--accent-blue)';
    }
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return '';
    return new Date(dateString).toLocaleDateString('de-DE');
  }

  get acwrChartHasData(): boolean {
    return this.acwrChartStats.sessions > 0;
  }

  get acwrMarkerPct(): number {
    if (this.todayAcwr == null) return 0;
    return Math.min((this.todayAcwr / 2.0) * 100, 97);
  }

  getAcwrFlagColor(flag: string | null): string {
    switch (flag) {
      case 'BLUE':   return '#42a5f5';
      case 'GREEN':  return '#66bb6a';
      case 'ORANGE': return '#ff7043';
      case 'RED':    return '#ef5350';
      default:       return 'var(--accent-blue)';
    }
  }

  getAcwrFlagIconBg(flag: string | null): string {
    switch (flag) {
      case 'BLUE':   return 'rgba(66,165,245,0.12)';
      case 'GREEN':  return 'rgba(102,187,106,0.12)';
      case 'ORANGE': return 'rgba(255,112,67,0.12)';
      case 'RED':    return 'rgba(239,83,80,0.12)';
      default:       return 'rgba(45,123,255,0.12)';
    }
  }

  getAcwrFlagLabel(flag: string | null): string {
    switch (flag) {
      case 'BLUE':   return 'Unterbelastung';
      case 'GREEN':  return 'Optimale Belastung';
      case 'ORANGE': return 'Erhöhte Belastung';
      case 'RED':    return 'Hohes Verletzungsrisiko';
      default:       return '';
    }
  }

  private buildAcwrChart(metrics: DailyMetrics[], from: Date): void {
    const ACWR_MAX = 2.0;
    const DAY_LABELS = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];

    const map = new Map<string, { acwr: number; flag: string }>();
    for (const m of metrics) {
      if (m.acwr != null && m.acwrFlag != null) {
        const dateStr = typeof m.date === 'string' ? m.date : this.formatDateLocal(new Date(m.date));
        map.set(dateStr, { acwr: m.acwr, flag: m.acwrFlag });
      }
    }

    let sum = 0, count = 0;
    const points: BarChartPoint[] = [];

    for (let i = 0; i < 30; i++) {
      const d = new Date(from);
      d.setDate(from.getDate() + i);
      const dateStr = this.formatDateLocal(d);
      const label = `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;
      const dayLabel = DAY_LABELS[d.getDay()];
      const entry = map.get(dateStr) ?? null;

      if (entry) { sum += entry.acwr; count++; }

      points.push({
        date: dateStr,
        dayLabel,
        label,
        showLabel: i % 7 === 0 || i === 29,
        value: entry ? entry.acwr : null,
        pct: entry ? Math.max((entry.acwr / ACWR_MAX) * 100, 1) : 0,
        color: entry ? this.getAcwrFlagColor(entry.flag) : 'transparent',
        tooltip: entry ? entry.acwr.toFixed(2) : ''
      });
    }

    this.acwrChart = points;
    this.acwrChartStats = {
      avg: count > 0 ? Math.round(sum / count * 100) / 100 : 0,
      sessions: count
    };
  }

  private buildReadinessChart(metrics: DailyMetrics[], from: Date): void {
    const SCORE_MAX = 100;
    const DAY_LABELS = ['So', 'Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa'];

    const map = new Map<string, { score: number; rec: string }>();
    for (const m of metrics) {
      if (m.readinessScore != null && m.recommendation != null && m.dailyStrain21 != null && m.dailyStrain21 > 0) {
        const dateStr = typeof m.date === 'string' ? m.date : this.formatDateLocal(new Date(m.date));
        map.set(dateStr, { score: m.readinessScore, rec: m.recommendation });
      }
    }

    let sum = 0, count = 0;
    const points: BarChartPoint[] = [];

    for (let i = 0; i < 30; i++) {
      const d = new Date(from);
      d.setDate(from.getDate() + i);
      const dateStr = this.formatDateLocal(d);
      const label = `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;
      const dayLabel = DAY_LABELS[d.getDay()];
      const entry = map.get(dateStr) ?? null;

      if (entry) { sum += entry.score; count++; }

      points.push({
        date: dateStr,
        dayLabel,
        label,
        showLabel: i % 7 === 0 || i === 29,
        value: entry ? entry.score : null,
        pct: entry ? Math.max((entry.score / SCORE_MAX) * 100, 2) : 0,
        color: entry ? this.getRecommendationColor(entry.rec) : 'transparent',
        tooltip: entry ? `${entry.score} – ${this.getRecommendationLabel(entry.rec)}` : ''
      });
    }

    this.readinessChart = points;
    this.readinessChartStats = {
      avg: count > 0 ? Math.round(sum / count) : 0,
      sessions: count
    };
  }

  get readinessChartHasData(): boolean {
    return this.readinessChartStats.sessions > 0;
  }

  getRecommendationColor(rec: string | null): string {
    switch (rec) {
      case 'HARD':     return '#4caf50';
      case 'MODERATE': return '#8bc34a';
      case 'EASY':     return '#ffca28';
      case 'REST':     return '#ef5350';
      default:         return 'var(--accent-blue)';
    }
  }

  getRecommendationLabel(rec: string | null): string {
    switch (rec) {
      case 'HARD':     return 'Hartes Training';
      case 'MODERATE': return 'Moderates Training';
      case 'EASY':     return 'Leichtes Training';
      case 'REST':     return 'Ruhetag';
      default:         return '';
    }
  }

  getRecommendationIconBg(rec: string | null): string {
    switch (rec) {
      case 'HARD':     return 'rgba(76,175,80,0.14)';
      case 'MODERATE': return 'rgba(139,195,74,0.14)';
      case 'EASY':     return 'rgba(255,202,40,0.14)';
      case 'REST':     return 'rgba(239,83,80,0.14)';
      default:         return 'rgba(45,123,255,0.12)';
    }
  }

  getRecommendationIcon(rec: string | null): string {
    switch (rec) {
      case 'HARD':     return 'bolt';
      case 'MODERATE': return 'directions_run';
      case 'EASY':     return 'self_improvement';
      case 'REST':     return 'hotel';
      default:         return 'psychology';
    }
  }

  recomputeReadiness(): void {
    this.recalculating = true;
    this.apiService.recomputeReadiness().subscribe({
      next: () => {
        this.profileWarningMessage = '';
        this.profileMissingFields = [];
        this.recalculating = false;
        this.snackBar.open('Readiness neu berechnet', 'OK', { duration: 4000 });
        this.loadDailyHistory();
      },
      error: (error) => {
        this.recalculating = false;
        const message = error?.error?.message;
        const missingFields = error?.error?.missingFields;
        if (Array.isArray(missingFields)) {
          this.profileMissingFields = missingFields;
          this.profileWarningMessage = message || 'Profil unvollstaendig fuer Metrik-Berechnungen.';
          return;
        }
        this.snackBar.open(message || 'Fehler bei der Readiness-Berechnung', 'Schliessen', { duration: 4000 });
      }
    });
  }

  get profileMissingFieldsLabel(): string {
    if (!this.profileMissingFields.length) {
      return '';
    }
    const labels = this.profileMissingFields.map((field) => this.getFieldLabel(field));
    return labels.join(', ');
  }

  private getFieldLabel(field: string): string {
    const map: Record<string, string> = {
      firstName: 'Vorname',
      lastName: 'Nachname',
      dateOfBirth: 'Geburtsdatum',
      heightCm: 'Groesse',
      weightKg: 'Gewicht',
      maxHeartRate: 'Maximale Herzfrequenz',
      hrRest: 'Ruhepuls',
      gender: 'Geschlecht'
    };
    return map[field] ?? field;
  }

  // ─── Context-aware Race Predictions ─────────────────────────────────────────

  loadCurrentRacePredictions(): void {
    this.apiService.getCurrentRacePredictions().pipe(
      catchError(() => of(null))
    ).subscribe(data => {
      this.currentRacePredictions = data;
    });
  }

  getAdjustmentColor(pct: number): string {
    if (pct === 0) return '#4caf50';
    if (pct <= 3)  return '#8bc34a';
    if (pct <= 8)  return '#ffca28';
    if (pct <= 15) return '#ff7043';
    return '#ef5350';
  }

  getConfidenceColor(c: string): string {
    switch (c) {
      case 'HOCH':   return '#4caf50';
      case 'MITTEL': return '#ffca28';
      default:       return '#ff7043';
    }
  }

  getConfidenceIcon(c: string): string {
    switch (c) {
      case 'HOCH':   return 'verified';
      case 'MITTEL': return 'info';
      default:       return 'warning';
    }
  }

  getPredictionDistanceColor(key: string): string {
    return this.distanceOptions.find(d => d.key === key)?.color ?? 'var(--accent-blue)';
  }

  // ─── Race Time Predictions (history) ────────────────────────────────────────

  loadVo2MaxHistory(): void {
    this.apiService.getVo2MaxHistory().pipe(
      catchError(() => of([]))
    ).subscribe((history: Vo2MaxHistoryPoint[]) => {
      this.vo2maxHistory = history;
      this.buildRaceTimeChart();
    });
  }

  selectDistance(key: string): void {
    this.selectedDistance = key;
    this.buildRaceTimeChart();
  }

  selectTimeRange(key: string): void {
    this.selectedTimeRange = key;
    this.buildRaceTimeChart();
  }

  get selectedDistanceColor(): string {
    return this.distanceOptions.find(d => d.key === this.selectedDistance)?.color ?? '#66bb6a';
  }

  get raceTimeChartHasData(): boolean {
    return this.raceTimeChartPoints.length >= 2;
  }

  get raceTimeSinglePoint(): boolean {
    return this.vo2maxHistory.length === 1 && this.raceTimeCurrentPrediction !== null;
  }

  getTrendIcon(): string {
    switch (this.raceTrendDirection) {
      case 'BESSER':      return 'trending_up';
      case 'SCHLECHTER':  return 'trending_down';
      case 'STABIL':      return 'trending_flat';
      default:            return '';
    }
  }

  getTrendColor(): string {
    switch (this.raceTrendDirection) {
      case 'BESSER':      return '#4caf50';
      case 'SCHLECHTER':  return '#ef5350';
      default:            return 'var(--text-secondary)';
    }
  }

  private buildRaceTimeChart(): void {
    const key = this.selectedDistance;
    const cutoff = new Date();
    switch (this.selectedTimeRange) {
      case '1W': cutoff.setDate(cutoff.getDate() - 7);           break;
      case '1M': cutoff.setMonth(cutoff.getMonth() - 1);         break;
      case '6M': cutoff.setMonth(cutoff.getMonth() - 6);         break;
      default:   cutoff.setFullYear(cutoff.getFullYear() - 1);   break; // 1Y
    }
    const cutoffStr = this.formatDateLocal(cutoff);
    const filtered = this.vo2maxHistory.filter(
      h => h.predictions[key] != null && h.date >= cutoffStr
    );

    const reset = () => {
      this.raceTimeChartPoints = [];
      this.raceTimeLinePath = '';
      this.raceTimeFillPath = '';
      this.raceTrendDirection = null;
    };

    if (filtered.length === 0) {
      reset();
      this.raceTimeCurrentPrediction = null;
      this.raceTimeBestPrediction = null;
      return;
    }

    const allSeconds = filtered.map(h => this.parseRaceTime(h.predictions[key]!));
    this.raceTimeCurrentPrediction = this.formatRaceTime(allSeconds[allSeconds.length - 1]);
    this.raceTimeBestPrediction    = this.formatRaceTime(Math.min(...allSeconds));

    if (filtered.length < 2) { reset(); return; }

    // ── SVG coordinate system: viewBox "0 0 400 90" ─────────────────────────
    const SVG_W = 400, SVG_H = 90;
    const padL = 5, padR = 5, padT = 6, padB = 24;
    const chartW = SVG_W - padL - padR;
    const chartH = SVG_H - padT - padB;
    const n = filtered.length;

    const minT = Math.min(...allSeconds);
    const maxT = Math.max(...allSeconds);
    const margin = (maxT - minT) * 0.12 || 30; // at least 30 s margin
    const dispMin = minT - margin;
    const dispMax = maxT + margin;
    const dispRange = dispMax - dispMin;

    const toY = (sec: number) =>
      Math.max(padT, Math.min(padT + chartH,
        padT + (1 - (sec - dispMin) / dispRange) * chartH));
    const toX = (i: number) =>
      padL + (n > 1 ? (i / (n - 1)) * chartW : chartW / 2);

    const stepInterval = Math.max(1, Math.floor(n / 7));

    // ── Raw dots (individual measurements, for hover) ────────────────────────
    this.raceTimeChartPoints = filtered.map((h, i) => {
      const sec   = allSeconds[i];
      const parts = h.date.split('-');
      const label = parts.length === 3
        ? `${parts[2]}.${parts[1]}.${parts[0].slice(2)}`
        : h.date;
      return {
        x: toX(i),
        y: toY(sec),
        date: h.date,
        label,
        timeFormatted: this.formatRaceTime(sec),
        timeSeconds: sec,
        showLabel: i === 0 || i === n - 1 || i % stepInterval === 0,
        vo2max: h.vo2max,
      };
    });

    // ── Smoothed trend line (centered moving average) ────────────────────────
    // Use a window that grows with data size, min 3 for any smoothing at all
    const windowSize = n < 3 ? 1 : Math.min(7, Math.max(3, Math.ceil(n / 5)));
    const trendSeconds = this.computeMovingAverage(allSeconds, windowSize);

    const trendPts = trendSeconds.map((sec, i) => ({ x: toX(i), y: toY(sec) }));

    this.raceTimeLinePath = trendPts
      .map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`)
      .join(' ');

    const bottom = padT + chartH;
    const last = trendPts[trendPts.length - 1];
    const first = trendPts[0];
    this.raceTimeFillPath =
      `${this.raceTimeLinePath} L${last.x.toFixed(1)},${bottom} L${first.x.toFixed(1)},${bottom} Z`;

    // ── Trend direction ──────────────────────────────────────────────────────
    // Compare mean of first third vs last third (in seconds; fewer = faster = better)
    if (n >= 4) {
      const third = Math.max(2, Math.floor(n / 3));
      const earlyAvg = allSeconds.slice(0, third).reduce((a, b) => a + b, 0) / third;
      const lateAvg  = allSeconds.slice(n - third).reduce((a, b) => a + b, 0) / third;
      const changePct = (lateAvg - earlyAvg) / earlyAvg;
      if      (changePct < -0.02) this.raceTrendDirection = 'BESSER';     // ≥ 2 % faster
      else if (changePct >  0.02) this.raceTrendDirection = 'SCHLECHTER'; // ≥ 2 % slower
      else                        this.raceTrendDirection = 'STABIL';
    } else {
      this.raceTrendDirection = null;
    }
  }

  /** Centered moving average with the given window size. */
  private computeMovingAverage(values: number[], windowSize: number): number[] {
    const half = Math.floor(windowSize / 2);
    return values.map((_, i) => {
      const start = Math.max(0, i - half);
      const end   = Math.min(values.length - 1, i + half);
      const slice = values.slice(start, end + 1);
      return slice.reduce((a, b) => a + b, 0) / slice.length;
    });
  }

  private parseRaceTime(timeStr: string): number {
    const parts = timeStr.split(':').map(Number);
    if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
    if (parts.length === 2) return parts[0] * 60 + parts[1];
    return 0;
  }

  formatRaceTime(totalSeconds: number): string {
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = Math.round(totalSeconds % 60);
    if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    return `${m}:${String(s).padStart(2, '0')}`;
  }
}
