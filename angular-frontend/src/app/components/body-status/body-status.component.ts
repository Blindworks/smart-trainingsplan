import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { BodyMetric, DailyMetrics } from '../../models/competition.model';

interface TrIMPChartPoint {
  date: string;
  label: string;
  showLabel: boolean;
  trimp: number | null;
  pct: number;
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

  trimpChart: TrIMPChartPoint[] = [];
  trimpChartStats = { avg: 0, max: 0, sessions: 0 };

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadMetrics();
    this.loadDailyHistory();
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
    startDate.setDate(today.getDate() - 29); // 30-day window
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
        // Build TRIMP chart
        this.buildTrIMPChart(metrics, startDate, today);
      },
      error: () => {}
    });
  }

  private buildTrIMPChart(metrics: DailyMetrics[], from: Date, to: Date): void {
    const map = new Map<string, number>();
    let max = 0;
    let sum = 0;
    let count = 0;

    for (const m of metrics) {
      if (m.dailyTrimp != null && m.dailyTrimp > 0) {
        map.set(m.date, m.dailyTrimp);
        if (m.dailyTrimp > max) max = m.dailyTrimp;
        sum += m.dailyTrimp;
        count++;
      }
    }

    const yMax = Math.max(200, max);
    const points: TrIMPChartPoint[] = [];

    for (let i = 0; i < 30; i++) {
      const d = new Date(from);
      d.setDate(from.getDate() + i);
      const dateStr = this.formatDateLocal(d);
      const trimp = map.get(dateStr) ?? null;
      const label = `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;

      points.push({
        date: dateStr,
        label,
        showLabel: i % 7 === 0 || i === 29,
        trimp,
        pct: trimp != null ? Math.max((trimp / yMax) * 100, 1) : 0,
        color: trimp != null ? this.getTRIMPBarColor(trimp) : 'transparent',
        tooltip: trimp != null ? `${label}: TRIMP ${trimp.toFixed(0)}` : `${label}: kein Training`
      });
    }

    this.trimpChart = points;
    this.trimpChartStats = {
      avg: count > 0 ? Math.round(sum / count) : 0,
      max: Math.round(max),
      sessions: count
    };
  }

  get trimpChartHasData(): boolean {
    return this.trimpChartStats.sessions > 0;
  }

  getTRIMPBarColor(trimp: number): string {
    if (trimp >= 150) return '#ef5350';
    if (trimp >= 100) return '#ff7043';
    if (trimp >= 50)  return '#ffca28';
    return '#66bb6a';
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
