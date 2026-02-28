import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

import { ApiService } from '../../services/api.service';
import { Competition } from '../../models/competition.model';

@Component({
  selector: 'app-competition-list',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './competition-list.component.html',
  styleUrl: './competition-list.component.scss'
})
export class CompetitionListComponent implements OnInit {
  competitions: Competition[] = [];
  loading = false;
  editingRankingId: number | null = null;
  rankingInput = '';

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCompetitions();
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

  register(competition: Competition): void {
    if (!competition.id) return;
    this.apiService.registerForCompetition(competition.id).subscribe({
      next: () => {
        this.snackBar.open('Erfolgreich angemeldet!', 'Schließen', { duration: 3000 });
        this.loadCompetitions();
      },
      error: () => {
        this.snackBar.open('Fehler bei der Anmeldung', 'Schließen', { duration: 3000 });
      }
    });
  }

  unregister(competition: Competition): void {
    if (!competition.id) return;
    this.apiService.unregisterFromCompetition(competition.id).subscribe({
      next: () => {
        this.snackBar.open('Abmeldung erfolgreich', 'Schließen', { duration: 3000 });
        this.loadCompetitions();
      },
      error: () => {
        this.snackBar.open('Fehler bei der Abmeldung', 'Schließen', { duration: 3000 });
      }
    });
  }

  startEditRanking(competition: Competition): void {
    this.editingRankingId = competition.id!;
    this.rankingInput = competition.ranking || '';
  }

  cancelEditRanking(): void {
    this.editingRankingId = null;
    this.rankingInput = '';
  }

  saveRanking(competition: Competition): void {
    if (!competition.id) return;
    this.apiService.updateCompetitionRegistration(competition.id, this.rankingInput).subscribe({
      next: () => {
        competition.ranking = this.rankingInput;
        this.editingRankingId = null;
        this.rankingInput = '';
        this.snackBar.open('Ranking gespeichert', 'Schließen', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Fehler beim Speichern des Rankings', 'Schließen', { duration: 3000 });
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

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'Kein Datum';
    return new Date(dateString).toLocaleDateString('de-DE');
  }

  getDaysUntil(dateString: string | undefined): number | null {
    if (!dateString) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const target = new Date(dateString);
    target.setHours(0, 0, 0, 0);
    return Math.round((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }
}
