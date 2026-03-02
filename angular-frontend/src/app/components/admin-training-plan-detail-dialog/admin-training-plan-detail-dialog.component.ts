import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, of } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { TrainingPlan, Training } from '../../models/competition.model';

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
    MatSnackBarModule
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
      prerequisites: this.editPrerequisites.trim()
    }).subscribe({
      next: updated => {
        this.data.plan.name = updated.name;
        this.data.plan.description = updated.description;
        this.data.plan.targetTime = updated.targetTime;
        this.data.plan.prerequisites = updated.prerequisites;
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
}
