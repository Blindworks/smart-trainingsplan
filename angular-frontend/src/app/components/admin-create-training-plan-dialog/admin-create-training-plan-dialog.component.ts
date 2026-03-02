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

interface TrainingRow {
  weekNumber: number | null;
  dayOfWeek: string;
  name: string;
  trainingType: string;
  intensityLevel: string;
  durationMinutes: number | null;
}

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
    MatSnackBarModule
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
      durationMinutes: null
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
      competitionType: this.planCompetitionType || undefined
    };

    this.apiService.createTrainingPlan(plan).pipe(
      switchMap(created => {
        const validRows = this.trainingRows.filter(r => r.name.trim());
        if (validRows.length === 0) return of(created);

        const calls = validRows.map(r => this.apiService.createTraining({
          name: r.name.trim(),
          trainingType: r.trainingType as any,
          intensityLevel: r.intensityLevel as any,
          weekNumber: r.weekNumber ?? undefined,
          dayOfWeek: r.dayOfWeek as any,
          durationMinutes: r.durationMinutes ?? undefined,
          isCompleted: false
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
      error: () => {
        this.saving = false;
        this.snackBar.open('Fehler beim Erstellen', 'Schließen', { duration: 3000 });
      }
    });
  }
}
