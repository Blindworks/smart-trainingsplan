import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { BodyMetric, DailyMetrics, DecouplingHistoryPoint } from '../../models/competition.model';
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

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadMetrics();
    this.loadDailyHistory();
    this.loadDecouplingHistory();
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

    this.apiService.getDailyMetrics(start, end).subscribe({
      next: (metrics: DailyMetrics[]) => {
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
      if (m.readinessScore != null && m.recommendation != null) {
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
        this.recalculating = false;
        this.snackBar.open('Readiness neu berechnet', 'OK', { duration: 4000 });
        this.loadDailyHistory();
      },
      error: () => {
        this.recalculating = false;
        this.snackBar.open('Fehler bei der Readiness-Berechnung', 'Schließen', { duration: 3000 });
      }
    });
  }
}
