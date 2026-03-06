import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { ApiService } from '../../services/api.service';
import { TrainingPlan, COMPETITION_TYPES } from '../../models/competition.model';
import { PaceInputComponent } from '../pace-input/pace-input.component';

interface TrainingRow {
  weekNumber: number | null;
  dayOfWeek: string;
  name: string;
  trainingType: string;
  intensityLevel: string;
  durationMinutes: number | null;
  workPace: string;
  workTimeText: string;
  workTimeSeconds: number | null;
  workDistanceKm: number | null;
  workDistanceMeters: number | null;
  recoveryPace: string;
  recoveryTimeText: string;
  recoveryTimeSeconds: number | null;
  recoveryDistanceKm: number | null;
  recoveryDistanceMeters: number | null;
}

interface PaceValidationResult {
  valid: boolean;
  error?: string;
}

type PaceSegment = 'work' | 'recovery';
type PaceChangedField = 'pace' | 'time' | 'distance';

@Component({
  selector: 'app-admin-create-training-plan-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PaceInputComponent
  ],
  templateUrl: './admin-create-training-plan-dialog.component.html',
  styleUrl: './admin-create-training-plan-dialog.component.scss'
})
export class AdminCreateTrainingPlanDialogComponent {

  planName = '';
  planDescription = '';
  planTargetTime = '';
  planPrerequisites = '';
  planCompetitionType = '';

  readonly competitionTypes = COMPETITION_TYPES;

  trainingRows: TrainingRow[] = [];
  saving = false;

  readonly days = [
    { value: 'MONDAY', label: 'Montag' },
    { value: 'TUESDAY', label: 'Dienstag' },
    { value: 'WEDNESDAY', label: 'Mittwoch' },
    { value: 'THURSDAY', label: 'Donnerstag' },
    { value: 'FRIDAY', label: 'Freitag' },
    { value: 'SATURDAY', label: 'Samstag' },
    { value: 'SUNDAY', label: 'Sonntag' },
  ];

  readonly types = [
    { value: 'endurance', label: 'Ausdauer' },
    { value: 'speed', label: 'Speed' },
    { value: 'interval', label: 'Intervall' },
    { value: 'fartlek', label: 'Fahrtspiel' },
    { value: 'strength', label: 'Kraft' },
    { value: 'recovery', label: 'Regeneration' },
    { value: 'race', label: 'Wettkampf' },
    { value: 'swimming', label: 'Schwimmen' },
    { value: 'cycling', label: 'Radfahren' },
    { value: 'general', label: 'Allgemein' },
  ];

  readonly intensities = [
    { value: 'low', label: 'Niedrig' },
    { value: 'medium', label: 'Mittel' },
    { value: 'high', label: 'Hoch' },
    { value: 'recovery', label: 'Regeneration' },
    { value: 'rest', label: 'Ruhe' },
  ];

  constructor(
    public dialogRef: MatDialogRef<AdminCreateTrainingPlanDialogComponent>,
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  addRow(): void {
    this.trainingRows.push({
      weekNumber: null,
      dayOfWeek: 'MONDAY',
      name: '',
      trainingType: 'endurance',
      intensityLevel: 'medium',
      durationMinutes: null,
      workPace: '',
      workTimeText: '',
      workTimeSeconds: null,
      workDistanceKm: null,
      workDistanceMeters: null,
      recoveryPace: '',
      recoveryTimeText: '',
      recoveryTimeSeconds: null,
      recoveryDistanceKm: null,
      recoveryDistanceMeters: null,
    });
  }

  removeRow(index: number): void {
    this.trainingRows.splice(index, 1);
  }

  save(): void {
    if (!this.planName.trim()) return;
    this.saving = true;

    const plan: TrainingPlan = {
      name: this.planName.trim(),
      description: this.planDescription.trim() || undefined,
      targetTime: this.planTargetTime.trim() || undefined,
      prerequisites: this.planPrerequisites.trim() || undefined,
      competitionType: this.planCompetitionType || undefined,
    };

    this.apiService.createTrainingPlan(plan).pipe(
      switchMap(created => {
        const validRows = this.trainingRows.filter(r => r.name.trim());
        if (validRows.length === 0) return of(created);

        for (const row of validRows) {
          const validation = this.normalizeAndValidateRow(row);
          if (!validation.valid) {
            throw new Error(validation.error ?? 'Ungültige Pace-Daten');
          }
        }

        const calls = validRows.map(r => this.apiService.createTraining({
          name: r.name.trim(),
          trainingType: r.trainingType as any,
          intensityLevel: r.intensityLevel as any,
          weekNumber: r.weekNumber ?? undefined,
          dayOfWeek: r.dayOfWeek as any,
          durationMinutes: r.durationMinutes ?? undefined,
          workPace: r.workPace || undefined,
          workTimeSeconds: r.workTimeSeconds ?? undefined,
          workDistanceMeters: r.workDistanceKm != null ? Math.round(r.workDistanceKm * 1000) : undefined,
          recoveryPace: r.recoveryPace || undefined,
          recoveryTimeSeconds: r.recoveryTimeSeconds ?? undefined,
          recoveryDistanceMeters: r.recoveryDistanceKm != null ? Math.round(r.recoveryDistanceKm * 1000) : undefined,
          isCompleted: false,
        }, created.id));

        return forkJoin(calls).pipe(switchMap(() => of(created)));
      })
    ).subscribe({
      next: created => {
        this.saving = false;
        const count = this.trainingRows.filter(r => r.name.trim()).length;
        const msg = count > 0
          ? `Trainingsplan mit ${count} Training(s) erstellt`
          : 'Trainingsplan erstellt';
        this.snackBar.open(msg, 'Schließen', { duration: 3000 });
        this.dialogRef.close(created);
      },
      error: (err) => {
        this.saving = false;
        const errorMessage = err?.message || 'Fehler beim Erstellen';
        this.snackBar.open(errorMessage, 'Schließen', { duration: 3500 });
      }
    });
  }

  onPaceFieldChanged(row: TrainingRow, segment: PaceSegment, changedField: PaceChangedField): void {
    this.recalculateDerivedPaceFields(row, segment, changedField);
  }

  private normalizeAndValidateRow(row: TrainingRow): PaceValidationResult {
    row.workPace = row.workPace?.trim() ?? '';
    row.recoveryPace = row.recoveryPace?.trim() ?? '';

    const workTimeSync = this.syncDurationFromText(row, 'work');
    if (!workTimeSync.valid) return workTimeSync;
    const recoveryTimeSync = this.syncDurationFromText(row, 'recovery');
    if (!recoveryTimeSync.valid) return recoveryTimeSync;

    if (row.workTimeSeconds != null && row.workTimeSeconds <= 0) {
      return { valid: false, error: 'work_time muss größer als 0 sein.' };
    }
    if (row.workDistanceKm != null && row.workDistanceKm <= 0) {
      return { valid: false, error: 'work_distance muss größer als 0 sein.' };
    }
    if (row.recoveryTimeSeconds != null && row.recoveryTimeSeconds <= 0) {
      return { valid: false, error: 'recovery_time muss größer als 0 sein.' };
    }
    if (row.recoveryDistanceKm != null && row.recoveryDistanceKm <= 0) {
      return { valid: false, error: 'recovery_distance muss größer als 0 sein.' };
    }

    if (!row.workPace) {
      return { valid: false, error: 'Bitte work_pace angeben.' };
    }

    const workPaceSeconds = this.parsePaceToSeconds(row.workPace);
    if (!workPaceSeconds) {
      return { valid: false, error: 'work_pace muss im Format MM:SS oder H:MM:SS sein.' };
    }

    if (row.workTimeSeconds == null && row.workDistanceKm == null) {
      return { valid: false, error: 'Bei work_pace muss work_time oder work_distance angegeben werden.' };
    }

    if (row.workTimeSeconds == null && row.workDistanceKm != null) {
      row.workTimeSeconds = Math.round(workPaceSeconds * row.workDistanceKm);
    } else if (row.workDistanceKm == null && row.workTimeSeconds != null) {
      row.workDistanceKm = row.workTimeSeconds / workPaceSeconds;
    }

    if (row.recoveryPace) {
      const recoveryPaceSeconds = this.parsePaceToSeconds(row.recoveryPace);
      if (!recoveryPaceSeconds) {
        return { valid: false, error: 'recovery_pace muss im Format MM:SS oder H:MM:SS sein.' };
      }

      if (row.recoveryTimeSeconds == null && row.recoveryDistanceKm == null) {
        return { valid: false, error: 'Bei recovery_pace muss recovery_time oder recovery_distance angegeben werden.' };
      }

      if (row.recoveryTimeSeconds == null && row.recoveryDistanceKm != null) {
        row.recoveryTimeSeconds = Math.round(recoveryPaceSeconds * row.recoveryDistanceKm);
      } else if (row.recoveryDistanceKm == null && row.recoveryTimeSeconds != null) {
        row.recoveryDistanceKm = row.recoveryTimeSeconds / recoveryPaceSeconds;
      }
    }

    this.recalculateTotalDuration(row);

    return { valid: true };
  }

  private recalculateDerivedPaceFields(row: TrainingRow, segment: PaceSegment, changedField: PaceChangedField): void {
    const paceValue = (segment === 'work' ? row.workPace : row.recoveryPace)?.trim() ?? '';
    if (segment === 'work') row.workPace = paceValue;
    else row.recoveryPace = paceValue;

    const paceSeconds = this.parsePaceToSeconds(paceValue);
    if (!paceSeconds) return;

    const timeText = (segment === 'work' ? row.workTimeText : row.recoveryTimeText)?.trim() ?? '';
    const parsedTime = timeText ? this.parseDurationToSeconds(timeText) : null;
    if (timeText && parsedTime == null) return;

    let time = parsedTime;
    let distance = segment === 'work' ? row.workDistanceKm : row.recoveryDistanceKm;

    if (time != null && time <= 0) return;
    if (distance != null && distance <= 0) return;

    if (changedField === 'time' && time != null) {
      distance = time / paceSeconds;
    } else if (changedField === 'distance' && distance != null) {
      time = Math.round(paceSeconds * distance);
    } else if (changedField === 'pace') {
      if (time != null) {
        distance = time / paceSeconds;
      } else if (distance != null) {
        time = Math.round(paceSeconds * distance);
      }
    }

    if (segment === 'work') {
      row.workTimeText = this.formatSecondsToDuration(time);
      row.workTimeSeconds = time;
      row.workDistanceKm = distance;
    } else {
      row.recoveryTimeText = this.formatSecondsToDuration(time);
      row.recoveryTimeSeconds = time;
      row.recoveryDistanceKm = distance;
    }

    this.recalculateTotalDuration(row);
  }

  private syncDurationFromText(row: TrainingRow, segment: PaceSegment): PaceValidationResult {
    const text = (segment === 'work' ? row.workTimeText : row.recoveryTimeText)?.trim() ?? '';
    const fieldName = segment === 'work' ? 'work_time' : 'recovery_time';

    if (!text) {
      if (segment === 'work') row.workTimeSeconds = null;
      else row.recoveryTimeSeconds = null;
      return { valid: true };
    }

    const seconds = this.parseDurationToSeconds(text);
    if (seconds == null) {
      return { valid: false, error: `${fieldName} muss im Format HH:mm:ss sein.` };
    }

    if (segment === 'work') {
      row.workTimeSeconds = seconds;
      row.workTimeText = this.formatSecondsToDuration(seconds);
    } else {
      row.recoveryTimeSeconds = seconds;
      row.recoveryTimeText = this.formatSecondsToDuration(seconds);
    }
    return { valid: true };
  }

  private parseDurationToSeconds(value: string): number | null {
    const match = /^(\d{2}):([0-5]\d):([0-5]\d)$/.exec(value.trim());
    if (!match) return null;
    const hours = Number(match[1]);
    const minutes = Number(match[2]);
    const seconds = Number(match[3]);
    return (hours * 3600) + (minutes * 60) + seconds;
  }

  private formatSecondsToDuration(value: number | null): string {
    if (value == null || value < 0) return '';
    const hours = Math.floor(value / 3600);
    const minutes = Math.floor((value % 3600) / 60);
    const seconds = value % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  private recalculateTotalDuration(row: TrainingRow): void {
    const work = row.workTimeSeconds ?? 0;
    const recovery = row.recoveryTimeSeconds ?? 0;
    const totalSeconds = work + recovery;
    row.durationMinutes = totalSeconds > 0 ? Math.max(1, Math.round(totalSeconds / 60)) : null;
  }

  private parsePaceToSeconds(value: string): number | null {
    const parts = value.split(':');
    if (parts.length !== 2 && parts.length !== 3) return null;

    const numeric = parts.map(p => Number(p));
    if (numeric.some(n => !Number.isInteger(n) || n < 0)) return null;

    let totalSeconds = 0;
    if (parts.length === 2) {
      const [minutes, seconds] = numeric;
      if (seconds >= 60) return null;
      totalSeconds = (minutes * 60) + seconds;
    } else {
      const [hours, minutes, seconds] = numeric;
      if (minutes >= 60 || seconds >= 60) return null;
      totalSeconds = (hours * 3600) + (minutes * 60) + seconds;
    }

    return totalSeconds > 0 ? totalSeconds : null;
  }
}
