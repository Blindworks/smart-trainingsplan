import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
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
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
  ],
  templateUrl: './strava-activity-dialog.component.html',
  styleUrl: './strava-activity-dialog.component.scss'
})
export class StravaActivityDialogComponent implements OnInit {
  completed: CompletedTraining;
  vo2Max: number | null = null;
  vo2MaxHRCorrected: number | null = null;
  vo2MaxLoading = false;
  activityMetrics: ActivityMetrics | null = null;
  metricsComputing = false;
  metricsComputeError: string | null = null;

  trainingTypes: string[] = [];
  selectedTrainingType: string | null = null;
  trainingTypeSaving = false;
  trainingTypeSaved = false;

  constructor(
    private dialogRef: MatDialogRef<StravaActivityDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CompletedTrainingDialogData,
    private apiService: ApiService
  ) {
    this.completed = data.completed;
  }

  ngOnInit(): void {
    this.selectedTrainingType = this.completed.trainingType ?? null;
    this.apiService.getCompletedTrainingTypes().pipe(
      catchError(() => of([]))
    ).subscribe(types => {
      this.trainingTypes = types;
    });

    if (this.isRunning() && this.completed.distanceKm && this.completed.durationSeconds) {
      this.vo2MaxLoading = true;
      this.apiService.getVo2MaxEstimate(
        this.completed.distanceKm,
        this.completed.durationSeconds,
        this.completed.sport ?? '',
        this.completed.averageHeartRate
      ).pipe(
        catchError(() => of(null))
      ).subscribe(result => {
        this.vo2Max = result?.vo2max ?? null;
        this.vo2MaxHRCorrected = result?.vo2maxHRCorrected ?? null;
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

  saveTrainingType(): void {
    if (!this.completed.id) return;
    this.trainingTypeSaving = true;
    this.trainingTypeSaved = false;
    this.apiService.updateCompletedTrainingType(this.completed.id, this.selectedTrainingType).pipe(
      catchError(() => of(null))
    ).subscribe(updated => {
      this.trainingTypeSaving = false;
      if (updated) {
        this.completed.trainingType = updated.trainingType;
        this.trainingTypeSaved = true;
        setTimeout(() => this.trainingTypeSaved = false, 2000);
      }
    });
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

  formatStartTime(dateTimeString: string): string {
    // Avoid timezone conversion: show the wall-clock time encoded in the ISO string.
    const isoMatch = /^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})/.exec(dateTimeString);
    if (isoMatch) {
      return `${isoMatch[2]}:${isoMatch[3]}`;
    }
    const date = new Date(dateTimeString);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
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

  getTRIMPColor(trimp: number): string {
    if (trimp >= 150) return '#ef5350'; // rot — sehr hoch
    if (trimp >= 100) return '#ff7043'; // orange — hoch
    if (trimp >= 50)  return '#ffca28'; // gelb — moderat
    return '#66bb6a';                   // grün — leicht
  }

  getTRIMPCategory(trimp: number): string {
    if (trimp >= 150) return 'Sehr hoch';
    if (trimp >= 100) return 'Hoch';
    if (trimp >= 50)  return 'Moderat';
    return 'Leicht';
  }

  getTRIMPGaugeWidth(trimp: number): number {
    return Math.min((trimp / 200) * 100, 100);
  }

  /** Color for aerobic decoupling value. */
  getDecouplingColor(pct: number): string {
    if (pct > 8)  return '#ef5350'; // red   — starker Drift
    if (pct > 5)  return '#ff7043'; // orange — mäßiger Drift
    if (pct > 3)  return '#ffca28'; // yellow — leichter Drift
    if (pct < 0)  return '#42a5f5'; // blue  — negativ (gut)
    return '#66bb6a';               // green  — effizient (0–3 %)
  }

  /** Category label for aerobic decoupling value. */
  getDecouplingCategory(pct: number): string {
    if (pct > 8)  return 'Starker Drift';
    if (pct > 5)  return 'Mäßiger Drift';
    if (pct > 3)  return 'Leichter Drift';
    if (pct < 0)  return 'Negativ (gut)';
    return 'Effizient';
  }

  /**
   * Left anchor (%) for the centered gauge fill.
   * Track spans −10 % … +10 %; centre mark is at 50 %.
   * Positive values extend right from centre; negative values extend left.
   */
  getDecouplingGaugeLeft(pct: number): number {
    const clamped = Math.max(-10, Math.min(10, pct));
    return clamped >= 0 ? 50 : 50 + (clamped / 10) * 50;
  }

  /** Width (%) of the gauge fill — one side of centre = max 50 %. */
  getDecouplingGaugeWidth(pct: number): number {
    return Math.abs(Math.max(-10, Math.min(10, pct)) / 10) * 50;
  }

  getEfFormatted(ef: number | undefined): string {
    if (ef == null) return '—';
    return ef.toFixed(4);
  }

  getEfColor(ef: number): string {
    if (ef >= 0.030) return '#4caf50';
    if (ef >= 0.024) return '#8bc34a';
    if (ef >= 0.018) return '#ffca28';
    if (ef >= 0.013) return '#ff7043';
    return '#ef5350';
  }

  getEfCategory(ef: number): string {
    if (ef >= 0.030) return 'Sehr effizient';
    if (ef >= 0.024) return 'Effizient';
    if (ef >= 0.018) return 'Durchschnittlich';
    if (ef >= 0.013) return 'Unterdurchschnittlich';
    return 'Wenig effizient';
  }

  /** Human-readable explanation for an ineligible decoupling result. */
  getDecouplingIneligibleText(reason: string | undefined): string {
    switch (reason) {
      case 'TOO_SHORT':           return 'Aktivität zu kurz – mindestens 20 Min. erforderlich.';
      case 'HR_COVERAGE_TOO_LOW': return 'Zu wenig HR-Daten – mindestens 70 % erforderlich.';
      case 'SPEED_DATA_MISSING':  return 'Kein Geschwindigkeits-Stream verfügbar.';
      default:                    return 'Decoupling konnte nicht berechnet werden.';
    }
  }
}
