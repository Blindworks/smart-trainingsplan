import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { TrainingStatsDto, TrainingStatsBucket } from '../../models/competition.model';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    TranslatePipe
  ],
  templateUrl: './statistics.component.html',
  styleUrls: ['./statistics.component.scss']
})
export class StatisticsComponent implements OnInit {
  period = 'month';
  selectedTrainingType = '';
  selectedSport = '';
  trainingTypes: string[] = [];
  sports: string[] = [];
  stats: TrainingStatsDto | null = null;
  loading = false;
  maxDistance = 0;
  hoveredIndex: number | null = null;

  readonly periods = [
    { value: 'day',   labelKey: 'statistics.day' },
    { value: 'week',  labelKey: 'statistics.week' },
    { value: 'month', labelKey: 'statistics.month' },
    { value: 'year',  labelKey: 'statistics.year' },
    { value: 'all',   labelKey: 'statistics.all' },
  ];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getTrainingTypesUsed().pipe(catchError(() => of([]))).subscribe(t => this.trainingTypes = t);
    this.api.getSportsUsed().pipe(catchError(() => of([]))).subscribe(s => this.sports = s);
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.api.getTrainingStats(
      this.period,
      this.selectedTrainingType || undefined,
      this.selectedSport || undefined
    ).pipe(catchError(() => of(null))).subscribe(data => {
      this.stats = data;
      this.maxDistance = data && data.buckets.length > 0
        ? Math.max(...data.buckets.map(b => b.distanceKm), 0.1)
        : 0.1;
      this.loading = false;
    });
  }

  onPeriodChange(p: string): void {
    this.period = p;
    this.loadStats();
  }

  onFilterChange(): void {
    this.loadStats();
  }

  formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}h ${m}m`;
    return `${m}m`;
  }

  barHeight(bucket: TrainingStatsBucket): number {
    if (this.maxDistance === 0) return 0;
    return Math.round((bucket.distanceKm / this.maxDistance) * 100);
  }

  get hasData(): boolean {
    return this.stats !== null && this.stats.buckets.length > 0;
  }

  get hasAnyActivity(): boolean {
    return this.stats !== null && this.stats.totalActivityCount > 0;
  }

  trackByIndex(index: number): number {
    return index;
  }
}
