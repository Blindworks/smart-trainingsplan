import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { catchError, of } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { TrainingPlan, Training } from '../../models/competition.model';

export interface TrainingPlanDetailDialogData {
  plan: TrainingPlan;
}

@Component({
  selector: 'app-admin-training-plan-detail-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './admin-training-plan-detail-dialog.component.html',
  styleUrl: './admin-training-plan-detail-dialog.component.scss'
})
export class AdminTrainingPlanDetailDialogComponent implements OnInit {
  trainings: Training[] = [];
  loading = true;

  constructor(
    public dialogRef: MatDialogRef<AdminTrainingPlanDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TrainingPlanDetailDialogData,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    if (this.data.plan.id) {
      this.apiService.getTrainingsByPlan(this.data.plan.id)
        .pipe(catchError(() => of([])))
        .subscribe(trainings => {
          this.trainings = trainings.sort((a, b) =>
            (a.trainingDate ?? '').localeCompare(b.trainingDate ?? '')
          );
          this.loading = false;
        });
    }
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
