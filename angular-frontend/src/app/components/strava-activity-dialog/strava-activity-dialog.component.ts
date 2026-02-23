import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivityMetrics, CompletedTraining } from '../../models/competition.model';
import { ApiService } from '../../services/api.service';
import { catchError, of } from 'rxjs';

export interface CompletedTrainingDialogData {
  completed: CompletedTraining;
}

@Component({
  selector: 'app-strava-activity-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './strava-activity-dialog.component.html',
  styleUrl: './strava-activity-dialog.component.scss'
})
export class StravaActivityDialogComponent implements OnInit {
  completed: CompletedTraining;
  vo2Max: number | null = null;
  vo2MaxLoading = false;
  activityMetrics: ActivityMetrics | null = null;
  metricsComputing = false;
  metricsComputeError: string | null = null;

  constructor(
    private dialogRef: MatDialogRef<StravaActivityDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CompletedTrainingDialogData,
    private apiService: ApiService
  ) {
    this.completed = data.completed;
  }

  ngOnInit(): void {
    if (this.isRunning() && this.completed.distanceKm && this.completed.durationSeconds) {
      this.vo2MaxLoading = true;
      this.apiService.getVo2MaxEstimate(
        this.completed.distanceKm,
        this.completed.durationSeconds,
        this.completed.sport ?? ''
      ).pipe(
        catchError(() => of(null))
      ).subscribe(result => {
        this.vo2Max = result?.vo2max ?? null;
        this.vo2MaxLoading = false;
      });
    }

    if (this.completed.id) {
      this.apiService.getActivityMetrics(this.completed.id).pipe(
        catchError(() => of(null))
      ).subscribe(metrics => {
        this.activityMetrics = metrics;
      });
    }
  }

  /** Returns the total zone minutes for calculating bar widths. */
  getTotalZoneMin(): number {
    if (!this.activityMetrics) return 0;
    const m = this.activityMetrics;
    return (m.z1Min ?? 0) + (m.z2Min ?? 0) + (m.z3Min ?? 0) + (m.z4Min ?? 0) + (m.z5Min ?? 0);
  }

  /** Width percentage for a given zone's bar (0–100). */
  getZoneBarWidth(zoneMin: number | undefined): number {
    const total = this.getTotalZoneMin();
    if (!total || !zoneMin) return 0;
    return (zoneMin / total) * 100;
  }

  computeStravaMetrics(): void {
    if (!this.completed.id) return;
    this.metricsComputing = true;
    this.metricsComputeError = null;
    this.apiService.computeStravaMetrics(this.completed.id).pipe(
      catchError(err => {
        this.metricsComputeError = 'Berechnung fehlgeschlagen. Kein HR-Stream verfügbar?';
        this.metricsComputing = false;
        return of(null);
      })
    ).subscribe(metrics => {
      this.activityMetrics = metrics;
      this.metricsComputing = false;
    });
  }

  formatZoneMin(minutes: number | undefined): string {
    if (!minutes || minutes < 0.01) return '0 min';
    if (minutes < 1) return `${Math.round(minutes * 60)} s`;
    const h = Math.floor(minutes / 60);
    const m = Math.round(minutes % 60);
    return h > 0 ? `${h}h ${m}min` : `${m} min`;
  }

  onClose(): void {
    this.dialogRef.close();
  }

  isRunning(): boolean {
    return (this.completed.sport ?? '').toLowerCase().includes('run');
  }

  isStrava(): boolean {
    return this.completed.source === 'STRAVA';
  }

  getDisplayName(): string {
    return this.completed.activityName ?? this.formatSportName(this.completed.sport);
  }

  formatSportName(sport: string | undefined): string {
    if (!sport) return 'Training';
    const map: { [key: string]: string } = {
      Run: 'Laufen', Ride: 'Radfahren', Swim: 'Schwimmen',
      Walk: 'Gehen', Hike: 'Wandern', WeightTraining: 'Krafttraining',
      Yoga: 'Yoga', Workout: 'Training', VirtualRide: 'Virtuelles Radfahren',
      TrailRun: 'Trailrunning', NordicSki: 'Skilanglauf', AlpineSki: 'Skifahren',
      Rowing: 'Rudern', running: 'Laufen', cycling: 'Radfahren',
      swimming: 'Schwimmen', generic: 'Training'
    };
    return map[sport] ?? (sport.charAt(0).toUpperCase() + sport.slice(1));
  }

  getSportIcon(sport: string | undefined): string {
    const s = (sport ?? '').toLowerCase();
    if (s.includes('run')) return 'directions_run';
    if (s.includes('rid') || s.includes('cycl')) return 'directions_bike';
    if (s.includes('swim')) return 'pool';
    if (s.includes('walk') || s.includes('hike')) return 'hiking';
    if (s.includes('weight') || s.includes('strength')) return 'fitness_center';
    if (s.includes('yoga')) return 'self_improvement';
    if (s.includes('row')) return 'rowing';
    return 'sports';
  }

  formatDate(dateString: string): string {
    const [year, month, day] = dateString.split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString('de-DE', {
      weekday: 'long', day: '2-digit', month: 'long', year: 'numeric'
    });
  }

  formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return h > 0 ? `${h}h ${m}min` : `${m}min`;
  }

  formatPace(secondsPerKm: number): string {
    const min = Math.floor(secondsPerKm / 60);
    const sec = Math.round(secondsPerKm % 60).toString().padStart(2, '0');
    return `${min}:${sec} min/km`;
  }

  formatSpeed(kmh: number): string {
    return `${kmh.toFixed(1)} km/h`;
  }

  getVo2MaxCategory(vo2max: number): string {
    if (vo2max >= 60) return 'Exzellent';
    if (vo2max >= 52) return 'Sehr gut';
    if (vo2max >= 44) return 'Gut';
    if (vo2max >= 36) return 'Durchschnittlich';
    return 'Verbesserungswürdig';
  }

  getVo2MaxColor(vo2max: number): string {
    if (vo2max >= 60) return '#4caf50';
    if (vo2max >= 52) return '#8bc34a';
    if (vo2max >= 44) return '#ff9800';
    if (vo2max >= 36) return '#ff5722';
    return '#f44336';
  }

  getStrainColor(strain21: number): string {
    if (strain21 >= 15) return '#ef5350'; // red
    if (strain21 >= 10) return '#ff7043'; // orange
    if (strain21 >= 6)  return '#ffca28'; // yellow
    return '#66bb6a';                      // green
  }

  getStrainGaugeWidth(strain21: number): number {
    return Math.min((strain21 / 21) * 100, 100);
  }
}
