import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ApiService } from '../../services/api.service';
import { Competition, TrainingPlanDto } from '../../models/competition.model';
import { AssignPlanDialogComponent } from '../assign-plan-dialog/assign-plan-dialog.component';

@Component({
  selector: 'app-competition-list',
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './competition-list.component.html',
  styleUrl: './competition-list.component.scss'
})
export class CompetitionListComponent implements OnInit {
  competitions: Competition[] = [];
  templatePlans: TrainingPlanDto[] = [];
  loading = false;
  templatesLoading = false;

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadCompetitions();
    this.loadTemplatePlans();
  }

  loadCompetitions(): void {
    this.loading = true;
    this.apiService.getAllCompetitions().subscribe({
      next: (competitions) => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        this.competitions = competitions.filter(c => {
          const dateStr = c.date || c.targetDate;
          return dateStr ? new Date(dateStr) >= today : true;
        });
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden der Wettkämpfe', 'Schließen', { duration: 3000 });
        this.loading = false;
      }
    });
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

  deleteCompetition(id: number): void {
    if (confirm('Möchten Sie diesen Wettkampf wirklich löschen?')) {
      this.apiService.deleteCompetition(id).subscribe({
        next: () => {
          this.snackBar.open('Wettkampf gelöscht', 'Schließen', { duration: 3000 });
          this.loadCompetitions();
        },
        error: () => {
          this.snackBar.open('Fehler beim Löschen', 'Schließen', { duration: 3000 });
        }
      });
    }
  }

  openAssignDialog(plan: TrainingPlanDto, preSelectedCompetition?: Competition): void {
    const dialogRef = this.dialog.open(AssignPlanDialogComponent, {
      data: { plan, preSelectedCompetition },
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

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'Kein Datum';
    return new Date(dateString).toLocaleDateString('de-DE');
  }
}
