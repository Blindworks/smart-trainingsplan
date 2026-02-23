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

interface DecouplingChartPoint {
  dateLabel: string;
  activityName: string;
  decouplingPct: number;
  barPct: number;   // visual bar height (0–100), based on |decouplingPct| / 15
  color: string;
  tooltip: string;
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

  decouplingChart: DecouplingChartPoint[] = [];
  decouplingChartStats = { avg: 0, best: 0, sessions: 0 };
  decouplingHoveredIndex: number | null = null;

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
    this.apiService.getDecouplingHistory(20).pipe(
      catchError(() => of([]))
    ).subscribe((points: DecouplingHistoryPoint[]) => {
      this.buildDecouplingChart(points);
    });
  }

  private buildDecouplingChart(points: DecouplingHistoryPoint[]): void {
    const MAX_PCT = 15;
    this.decouplingChart = points.map(p => {
      const pct = p.decouplingPct;
      const [, month, day] = p.date.split('-');
      const dateLabel = `${day}.${month}`;
      return {
        dateLabel,
        activityName: p.activityName ?? p.sport ?? 'Aktivität',
        decouplingPct: pct,
        barPct: Math.max((Math.abs(pct) / MAX_PCT) * 100, 2),
        color: this.getDecouplingBarColor(pct),
        tooltip: `${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%`
      };
    });

    if (points.length === 0) {
      this.decouplingChartStats = { avg: 0, best: 0, sessions: 0 };
      return;
    }
    const sum  = points.reduce((s, p) => s + p.decouplingPct, 0);
    const best = Math.min(...points.map(p => p.decouplingPct));
    this.decouplingChartStats = {
      avg:      Math.round(sum / points.length * 10) / 10,
      best:     Math.round(best * 10) / 10,
      sessions: points.length
    };
  }

  get decouplingChartHasData(): boolean {
    return this.decouplingChart.length > 0;
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
}
