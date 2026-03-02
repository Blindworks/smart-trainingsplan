import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatRippleModule } from '@angular/material/core';
import { forkJoin } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { Competition, TrainingPlanDto } from '../../models/competition.model';

export interface AssignPlanDialogData {
  plan: TrainingPlanDto;
  preSelectedCompetition?: Competition;
}

@Component({
  selector: 'app-assign-plan-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatRippleModule
  ],
  templateUrl: './assign-plan-dialog.component.html',
  styleUrl: './assign-plan-dialog.component.scss'
})
export class AssignPlanDialogComponent implements OnInit {
  plan: TrainingPlanDto;
  competitions: Competition[] = [];
  selectedIds = new Set<number>();
  loading = false;
  assigning = false;

  constructor(
    private dialogRef: MatDialogRef<AssignPlanDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssignPlanDialogData,
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {
    this.plan = data.plan;
  }

  ngOnInit(): void {
    this.loadCompetitions();
  }

  loadCompetitions(): void {
    this.loading = true;
    this.apiService.getAllCompetitions().subscribe({
      next: (competitions) => {
        this.competitions = competitions;
        if (this.data.preSelectedCompetition?.id) {
          this.selectedIds.add(this.data.preSelectedCompetition.id);
        }
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden der Wettkaempfe', 'Schliessen', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  get filteredCompetitions(): Competition[] {
    if (!this.plan.competitionType) return this.competitions;
    return this.competitions.filter(c => !c.type || c.type === this.plan.competitionType);
  }

  get isFiltered(): boolean {
    return !!this.plan.competitionType;
  }

  toggle(competition: Competition): void {
    if (competition.id === undefined) return;
    if (this.selectedIds.has(competition.id)) {
      this.selectedIds.delete(competition.id);
    } else {
      this.selectedIds.add(competition.id);
    }
  }

  isSelected(competition: Competition): boolean {
    return competition.id !== undefined && this.selectedIds.has(competition.id);
  }

  assign(): void {
    if (this.selectedIds.size === 0) return;
    this.assigning = true;

    const calls = Array.from(this.selectedIds).map(compId =>
      this.apiService.assignPlanToCompetition(this.plan.id, compId)
    );

    forkJoin(calls).subscribe({
      next: () => {
        const count = this.selectedIds.size;
        const msg = count === 1
          ? 'Plan zugewiesen'
          : `Plan ${count} Competitions zugewiesen`;
        this.snackBar.open(msg, 'Schliessen', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (error: HttpErrorResponse) => {
        const backendMessage = error.error?.error;
        const message = backendMessage
          ? `Fehler beim Zuweisen des Plans: ${backendMessage}`
          : 'Fehler beim Zuweisen des Plans';
        this.snackBar.open(message, 'Schliessen', { duration: 5000 });
        this.assigning = false;
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }

  get selectedCount(): number {
    return this.selectedIds.size;
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return '';
    return new Date(dateString).toLocaleDateString('de-DE', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
