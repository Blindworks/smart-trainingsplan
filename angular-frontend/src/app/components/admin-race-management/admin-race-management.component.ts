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
import { Competition } from '../../models/competition.model';
import { AdminRaceEditDialogComponent } from '../admin-race-edit-dialog/admin-race-edit-dialog.component';

@Component({
  selector: 'app-admin-race-management',
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
  templateUrl: './admin-race-management.component.html',
  styleUrl: './admin-race-management.component.scss'
})
export class AdminRaceManagementComponent implements OnInit {
  races: Competition[] = [];
  loading = true;

  constructor(
    private apiService: ApiService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadRaces();
  }

  loadRaces(): void {
    this.loading = true;
    this.apiService.getAllCompetitions()
      .pipe(catchError(() => of([])))
      .subscribe(races => {
        this.races = races;
        this.loading = false;
      });
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(AdminRaceEditDialogComponent, {
      width: '560px',
      maxWidth: '96vw',
      data: { race: null }
    });
    dialogRef.afterClosed().subscribe((created?: Competition) => {
      if (created) {
        this.loadRaces();
        this.snackBar.open('Race erstellt', 'Schließen', { duration: 3000 });
      }
    });
  }

  openEditDialog(race: Competition): void {
    const dialogRef = this.dialog.open(AdminRaceEditDialogComponent, {
      width: '560px',
      maxWidth: '96vw',
      data: { race }
    });
    dialogRef.afterClosed().subscribe((updated?: Competition) => {
      if (updated) {
        this.loadRaces();
        this.snackBar.open('Race aktualisiert', 'Schließen', { duration: 3000 });
      }
    });
  }

  deleteRace(race: Competition): void {
    if (!race.id || !confirm(`Race "${race.name}" wirklich löschen?`)) return;
    this.apiService.deleteCompetition(race.id).subscribe({
      next: () => {
        this.races = this.races.filter(r => r.id !== race.id);
        this.snackBar.open('Race gelöscht', 'Schließen', { duration: 3000 });
      },
      error: () => this.snackBar.open('Fehler beim Löschen', 'Schließen', { duration: 3000 })
    });
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '–';
    return new Date(dateStr).toLocaleDateString('de-DE');
  }
}
