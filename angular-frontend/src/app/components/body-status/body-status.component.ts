import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { BodyMetric } from '../../models/competition.model';

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

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadMetrics();
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
