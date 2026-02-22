import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ApiService } from '../../services/api.service';
import { TrainingPlanDto } from '../../models/competition.model';
import { AssignPlanDialogComponent } from '../assign-plan-dialog/assign-plan-dialog.component';

@Component({
  selector: 'app-plan-library',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './plan-library.component.html',
  styleUrl: './plan-library.component.scss'
})
export class PlanLibraryComponent implements OnInit {
  templatePlans: TrainingPlanDto[] = [];
  templatesLoading = false;

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadTemplatePlans();
  }

  loadTemplatePlans(): void {
    this.templatesLoading = true;
    this.apiService.getTemplatePlans().subscribe({
      next: (plans) => {
        this.templatePlans = plans;
        this.templatesLoading = false;
      },
      error: () => {
        this.templatesLoading = false;
      }
    });
  }

  openAssignDialog(plan: TrainingPlanDto): void {
    const dialogRef = this.dialog.open(AssignPlanDialogComponent, {
      data: { plan },
      panelClass: 'assign-plan-dialog-panel',
      autoFocus: false,
      restoreFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.snackBar.open('Plan erfolgreich zugewiesen', 'Schließen', { duration: 3000 });
      }
    });
  }
}
