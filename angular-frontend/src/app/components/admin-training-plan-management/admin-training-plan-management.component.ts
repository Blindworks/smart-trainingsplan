import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { TrainingPlan } from '../../models/competition.model';
import { AdminTrainingPlanDetailDialogComponent } from '../admin-training-plan-detail-dialog/admin-training-plan-detail-dialog.component';

@Component({
  selector: 'app-admin-training-plan-management',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatDialogModule,
    MatSnackBarModule,
    RouterLink
  ],
  templateUrl: './admin-training-plan-management.component.html',
  styleUrl: './admin-training-plan-management.component.scss'
})
export class AdminTrainingPlanManagementComponent implements OnInit {
  plans: TrainingPlan[] = [];
  loading = true;

  constructor(
    private apiService: ApiService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    this.loading = true;
    this.apiService.getAllTrainingPlans()
      .pipe(catchError(() => of([])))
      .subscribe(plans => {
        this.plans = plans;
        this.loading = false;
      });
  }

  openDetail(plan: TrainingPlan): void {
    this.dialog.open(AdminTrainingPlanDetailDialogComponent, {
      width: '860px',
      maxWidth: '96vw',
      data: { plan }
    });
  }

  openEdit(plan: TrainingPlan): void {
    const ref = this.dialog.open(AdminTrainingPlanDetailDialogComponent, {
      width: '860px',
      maxWidth: '96vw',
      data: { plan, startInEditMode: true }
    });
    ref.afterClosed().subscribe(updated => {
      if (updated) {
        const idx = this.plans.findIndex(p => p.id === updated.id);
        if (idx !== -1) this.plans[idx] = { ...this.plans[idx], ...updated };
      }
    });
  }

  deletePlan(plan: TrainingPlan): void {
    if (!plan.id || !confirm(`Trainingsplan "${plan.name}" wirklich löschen?`)) return;
    this.apiService.deleteTrainingPlan(plan.id).subscribe({
      next: () => {
        this.plans = this.plans.filter(p => p.id !== plan.id);
        this.snackBar.open('Trainingsplan gelöscht', 'Schließen', { duration: 3000 });
      },
      error: () => this.snackBar.open('Fehler beim Löschen', 'Schließen', { duration: 3000 })
    });
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '–';
    return new Date(dateStr).toLocaleDateString('de-DE');
  }
}
