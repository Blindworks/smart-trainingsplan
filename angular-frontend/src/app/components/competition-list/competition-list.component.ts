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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ApiService } from '../../services/api.service';
import { Competition } from '../../models/competition.model';
import { RegistrationDialogComponent } from '../registration-dialog/registration-dialog.component';
import { SelectPlanDialogComponent } from '../select-plan-dialog/select-plan-dialog.component';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../services/i18n.service';

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
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    TranslatePipe
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
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private i18nService: I18nService
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
        this.showSnack('competitions.messages.loadError');
        this.loading = false;
      }
    });
  }

  openRegistrationDialog(competition: Competition): void {
    const ref = this.dialog.open(RegistrationDialogComponent, {
      data: { competition, isEditing: false },
      disableClose: true,
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.showSnack('competitions.messages.registered');
        this.loadCompetitions();
      }
    });
  }

  openEditRegistrationDialog(competition: Competition): void {
    const ref = this.dialog.open(RegistrationDialogComponent, {
      data: { competition, isEditing: true },
      disableClose: true,
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.showSnack('competitions.messages.registrationUpdated');
        this.loadCompetitions();
      }
    });
  }

  unregister(competition: Competition): void {
    if (!competition.id) {
      return;
    }

    this.apiService.unregisterFromCompetition(competition.id).subscribe({
      next: () => {
        this.showSnack('competitions.messages.unregisterSuccess');
        this.loadCompetitions();
      },
      error: () => {
        this.showSnack('competitions.messages.unregisterError');
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

  openSelectPlanDialog(competition: Competition): void {
    const ref = this.dialog.open(SelectPlanDialogComponent, {
      data: { competition },
      disableClose: true,
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.showSnack('competitions.messages.planLinked');
        this.loadCompetitions();
      }
    });
  }

  saveRanking(competition: Competition): void {
    if (!competition.id) {
      return;
    }

    this.apiService.updateCompetitionRegistration(competition.id, { ranking: this.rankingInput }).subscribe({
      next: () => {
        competition.ranking = this.rankingInput;
        this.editingRankingId = null;
        this.rankingInput = '';
        this.showSnack('competitions.messages.rankingSaved', 2000);
      },
      error: () => {
        this.showSnack('competitions.messages.rankingSaveError');
      }
    });
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) {
      return this.i18nService.t('competitions.noDate');
    }

    const locale = this.i18nService.getLanguage() === 'de' ? 'de-DE' : 'en-US';
    return new Date(dateString).toLocaleDateString(locale);
  }

  getDaysUntil(dateString: string | undefined): number | null {
    if (!dateString) {
      return null;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const target = new Date(dateString);
    target.setHours(0, 0, 0, 0);
    return Math.round((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }

  private showSnack(messageKey: string, duration = 3000): void {
    this.snackBar.open(this.i18nService.t(messageKey), this.i18nService.t('common.close'), { duration });
  }
}
