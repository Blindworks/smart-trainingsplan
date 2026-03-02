import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRippleModule } from '@angular/material/core';

import { ApiService } from '../../services/api.service';
import { Competition, TrainingPlanDto } from '../../models/competition.model';

export interface SelectPlanDialogData {
  competition: Competition;
}

@Component({
  selector: 'app-select-plan-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatRippleModule,
  ],
  templateUrl: './select-plan-dialog.component.html',
  styleUrl: './select-plan-dialog.component.scss',
})
export class SelectPlanDialogComponent implements OnInit {
  plans: TrainingPlanDto[] = [];
  loading = true;
  assigning = false;
  selectedPlanId: number | null = null;
  errorMessage = '';

  constructor(
    private apiService: ApiService,
    private dialogRef: MatDialogRef<SelectPlanDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SelectPlanDialogData,
  ) {}

  ngOnInit(): void {
    this.apiService.getTemplatePlans().subscribe({
      next: (plans) => {
        this.plans = plans;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Fehler beim Laden der Trainingspläne.';
        this.loading = false;
      },
    });
  }

  get filteredPlans(): TrainingPlanDto[] {
    if (!this.data.competition.type) return this.plans;
    return this.plans.filter(p => !p.competitionType || p.competitionType === this.data.competition.type);
  }

  get isFiltered(): boolean {
    return !!this.data.competition.type;
  }

  select(id: number): void {
    this.selectedPlanId = id;
  }

  isSelected(id: number): boolean {
    return this.selectedPlanId === id;
  }

  onAssign(): void {
    if (!this.selectedPlanId || this.assigning || !this.data.competition.id) return;
    this.assigning = true;
    this.errorMessage = '';
    this.apiService.assignPlanToCompetition(this.selectedPlanId, this.data.competition.id).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => {
        const backendMsg = err?.error?.error;
        this.errorMessage = backendMsg ? `Fehler: ${backendMsg}` : 'Fehler beim Verknüpfen des Plans.';
        this.assigning = false;
      },
    });
  }

  onCancel(): void {
    if (!this.assigning) this.dialogRef.close();
  }
}
