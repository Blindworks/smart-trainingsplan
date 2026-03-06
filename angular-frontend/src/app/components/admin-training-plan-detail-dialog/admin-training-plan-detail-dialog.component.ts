import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, forkJoin, of } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { TrainingPlan, Training, COMPETITION_TYPES } from '../../models/competition.model';
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

export interface TrainingPlanDetailDialogData {
  plan: TrainingPlan;
  startInEditMode?: boolean;
}

@Component({
  selector: 'app-admin-training-plan-detail-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    PaceInputComponent
  ],
  templateUrl: './admin-training-plan-detail-dialog.component.html',
  styleUrl: './admin-training-plan-detail-dialog.component.scss'
})
export class AdminTrainingPlanDetailDialogComponent implements OnInit {
  trainings: Training[] = [];
  loading = true;
  editMode = false;
  saving = false;

  editName = '';
  editDescription = '';
  editTargetTime = '';
  editPrerequisites = '';
  editCompetitionType = '';

  newRows: TrainingRow[] = [];
  savingNewTrainings = false;

  editingId: number | null = null;
  editRow: TrainingRow | null = null;
  savingEdit = false;

  readonly competitionTypes = COMPETITION_TYPES;

  readonly days = [
    { value: 'MONDAY',    label: 'Montag' },
    { value: 'TUESDAY',   label: 'Dienstag' },
    { value: 'WEDNESDAY', label: 'Mittwoch' },
    { value: 'THURSDAY',  label: 'Donnerstag' },
    { value: 'FRIDAY',    label: 'Freitag' },
    { value: 'SATURDAY',  label: 'Samstag' },
    { value: 'SUNDAY',    label: 'Sonntag' },
  ];

  readonly types = [
    { value: 'endurance',  label: 'Ausdauer' },
    { value: 'speed',      label: 'Speed' },
    { value: 'interval',   label: 'Intervall' },
    { value: 'fartlek',    label: 'Fahrtspiel' },
    { value: 'strength',   label: 'Kraft' },
    { value: 'recovery',   label: 'Regeneration' },
    { value: 'race',       label: 'Wettkampf' },
    { value: 'swimming',   label: 'Schwimmen' },
    { value: 'cycling',    label: 'Radfahren' },
    { value: 'general',    label: 'Allgemein' },
  ];

  readonly intensities = [
    { value: 'low',      label: 'Niedrig' },
    { value: 'medium',   label: 'Mittel' },
    { value: 'high',     label: 'Hoch' },
    { value: 'recovery', label: 'Regeneration' },
    { value: 'rest',     label: 'Ruhe' },
  ];

  constructor(
    public dialogRef: MatDialogRef<AdminTrainingPlanDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TrainingPlanDetailDialogData,
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.editMode = this.data.startInEditMode ?? false;
    this.resetEditFields();
    if (this.data.plan.id) {
      this.apiService.getTrainingsByPlan(this.data.plan.id)
        .pipe(catchError(() => of([])))
        .subscribe(trainings => {
          this.trainings = trainings.sort((a, b) =>
            (a.weekNumber ?? 0) - (b.weekNumber ?? 0)
          );
          this.loading = false;
        });
    }
  }

  resetEditFields(): void {
    this.editName = this.data.plan.name ?? '';
    this.editDescription = this.data.plan.description ?? '';
    this.editTargetTime = this.data.plan.targetTime ?? '';
    this.editPrerequisites = this.data.plan.prerequisites ?? '';
    this.editCompetitionType = this.data.plan.competitionType ?? '';
  }

  toggleEdit(): void {
    if (this.editMode) {
      this.resetEditFields();
    }
    this.editMode = !this.editMode;
  }

  saveMetadata(): void {
    if (!this.data.plan.id || !this.editName.trim()) return;
    this.saving = true;
    this.apiService.patchTrainingPlanMetadata(this.data.plan.id, {
      name: this.editName.trim(),
      description: this.editDescription.trim(),
      targetTime: this.editTargetTime.trim(),
      prerequisites: this.editPrerequisites.trim(),
      competitionType: this.editCompetitionType
    }).subscribe({
      next: updated => {
        this.data.plan.name = updated.name;
        this.data.plan.description = updated.description;
        this.data.plan.targetTime = updated.targetTime;
        this.data.plan.prerequisites = updated.prerequisites;
        this.data.plan.competitionType = updated.competitionType;
        this.editMode = false;
        this.saving = false;
        this.snackBar.open('Trainingsplan gespeichert', 'Schließen', { duration: 3000 });
        if (this.data.startInEditMode) {
          this.dialogRef.close(this.data.plan);
        }
      },
      error: () => {
        this.saving = false;
        this.snackBar.open('Fehler beim Speichern', 'Schließen', { duration: 3000 });
      }
    });
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '–';
    return new Date(dateStr).toLocaleDateString('de-DE');
  }

  intensityLabel(level?: string): string {
    const map: Record<string, string> = {
      high: 'Hoch', medium: 'Mittel', low: 'Niedrig', recovery: 'Regeneration', rest: 'Ruhe'
    };
    return level ? (map[level] ?? level) : '–';
  }

  typeLabel(type?: string): string {
    const map: Record<string, string> = {
      speed: 'Speed', endurance: 'Ausdauer', strength: 'Kraft', race: 'Wettkampf',
      interval: 'Intervall', recovery: 'Regeneration', swimming: 'Schwimmen',
      cycling: 'Radfahren', general: 'Allgemein', fartlek: 'Fahrtspiel'
    };
    return type ? (map[type] ?? type) : '–';
  }

  hasValidNewRow(): boolean {
    return this.newRows.some(r => r.name.trim().length > 0);
  }

  startEdit(t: Training): void {
    this.editingId = t.id!;
    this.editRow = {
      weekNumber: t.weekNumber ?? null,
      dayOfWeek: t.dayOfWeek ?? 'MONDAY',
      name: t.name,
      trainingType: t.trainingType,
      intensityLevel: t.intensityLevel,
      durationMinutes: t.durationMinutes ?? null,
      workPace: t.workPace ?? '',
      workTimeText: this.formatSecondsToDuration(t.workTimeSeconds ?? null),
      workTimeSeconds: t.workTimeSeconds ?? null,
      workDistanceKm: this.metersToKm(t.workDistanceMeters ?? null),
      workDistanceMeters: t.workDistanceMeters ?? null,
      recoveryPace: t.recoveryPace ?? '',
      recoveryTimeText: this.formatSecondsToDuration(t.recoveryTimeSeconds ?? null),
      recoveryTimeSeconds: t.recoveryTimeSeconds ?? null,
      recoveryDistanceKm: this.metersToKm(t.recoveryDistanceMeters ?? null),
      recoveryDistanceMeters: t.recoveryDistanceMeters ?? null,
    };
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editRow = null;
  }

  saveEdit(): void {
    if (!this.editingId || !this.editRow) return;
    const original = this.trainings.find(t => t.id === this.editingId);
    if (!original) return;

    const r = this.editRow;
    const validation = this.normalizeAndValidateRow(r);
    if (!validation.valid) {
      this.snackBar.open(validation.error ?? 'Ungültige Pace-Daten', 'Schließen', { duration: 3500 });
      return;
    }

    const updated: Training = {
      ...original,
      name: r.name.trim(),
      trainingType: r.trainingType as any,
      intensityLevel: r.intensityLevel as any,
      weekNumber: r.weekNumber ?? undefined,
      dayOfWeek: r.dayOfWeek as any,
      durationMinutes: r.durationMinutes ?? undefined,
      workPace: r.workPace?.trim() || undefined,
      workTimeSeconds: r.workTimeSeconds ?? undefined,
      workDistanceMeters: r.workDistanceKm != null ? Math.round(r.workDistanceKm * 1000) : undefined,
      recoveryPace: r.recoveryPace?.trim() || undefined,
      recoveryTimeSeconds: r.recoveryTimeSeconds ?? undefined,
      recoveryDistanceMeters: r.recoveryDistanceKm != null ? Math.round(r.recoveryDistanceKm * 1000) : undefined,
    };

    this.savingEdit = true;
    this.apiService.updateTraining(this.editingId, updated).subscribe({
      next: saved => {
        const idx = this.trainings.findIndex(t => t.id === this.editingId);
        if (idx >= 0) this.trainings[idx] = saved;
        this.cancelEdit();
        this.savingEdit = false;
        this.snackBar.open('Training gespeichert', 'Schließen', { duration: 2000 });
      },
      error: () => {
        this.savingEdit = false;
        this.snackBar.open('Fehler beim Speichern', 'Schließen', { duration: 3000 });
      }
    });
  }

  addRow(): void {
    this.newRows.push({
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

  removeNewRow(index: number): void {
    this.newRows.splice(index, 1);
  }

  saveNewTrainings(): void {
    const valid = this.newRows.filter(r => r.name.trim());
    if (!valid.length || !this.data.plan.id) return;

    for (const row of valid) {
      const validation = this.normalizeAndValidateRow(row);
      if (!validation.valid) {
        this.snackBar.open(validation.error ?? 'Ungültige Pace-Daten', 'Schließen', { duration: 3500 });
        return;
      }
    }

    this.savingNewTrainings = true;

    const calls = valid.map(r => this.apiService.createTraining({
      name: r.name.trim(),
      trainingType: r.trainingType as any,
      intensityLevel: r.intensityLevel as any,
      weekNumber: r.weekNumber ?? undefined,
      dayOfWeek: r.dayOfWeek as any,
      durationMinutes: r.durationMinutes ?? undefined,
      workPace: r.workPace?.trim() || undefined,
      workTimeSeconds: r.workTimeSeconds ?? undefined,
      workDistanceMeters: r.workDistanceKm != null ? Math.round(r.workDistanceKm * 1000) : undefined,
      recoveryPace: r.recoveryPace?.trim() || undefined,
      recoveryTimeSeconds: r.recoveryTimeSeconds ?? undefined,
      recoveryDistanceMeters: r.recoveryDistanceKm != null ? Math.round(r.recoveryDistanceKm * 1000) : undefined,
      isCompleted: false
    }, this.data.plan.id));

    forkJoin(calls).subscribe({
      next: created => {
        this.trainings = [...this.trainings, ...created].sort((a, b) =>
          (a.weekNumber ?? 0) - (b.weekNumber ?? 0)
        );
        this.newRows = [];
        this.savingNewTrainings = false;
        this.snackBar.open(`${created.length} Training(s) hinzugefügt`, 'Schließen', { duration: 3000 });
      },
      error: () => {
        this.savingNewTrainings = false;
        this.snackBar.open('Fehler beim Hinzufügen', 'Schließen', { duration: 3000 });
      }
    });
  }

  deleteTraining(training: Training): void {
    if (!training.id) return;
    this.apiService.deleteTraining(training.id).subscribe({
      next: () => {
        this.trainings = this.trainings.filter(t => t.id !== training.id);
        this.snackBar.open('Training gelöscht', 'Schließen', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Fehler beim Löschen', 'Schließen', { duration: 3000 });
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

  private metersToKm(value: number | null): number | null {
    if (value == null) return null;
    return value / 1000;
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
