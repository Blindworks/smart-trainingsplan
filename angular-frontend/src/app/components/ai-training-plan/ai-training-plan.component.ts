import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AiTrainingPlanService, AITrainingPlanDTO } from '../../services/ai-training-plan.service';
import { AuthService } from '../../services/auth.service';
import { catchError, of } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-ai-training-plan',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './ai-training-plan.component.html',
  styleUrls: ['./ai-training-plan.component.scss']
})
export class AiTrainingPlanComponent {
  weekStartDate: string = this.getMonday();
  loadPlanId: string = '';

  plan: AITrainingPlanDTO | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private aiService: AiTrainingPlanService,
    private authService: AuthService
  ) {}

  generate(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      this.error = 'Not logged in.';
      return;
    }
    this.loading = true;
    this.error = null;
    this.plan = null;

    this.aiService.generatePlan({
      userId: String(userId),
      weekStart: this.weekStartDate
    }).pipe(
      catchError(err => {
        this.error = this.buildErrorMessage(err, 'Failed to generate plan.');
        return of(null);
      })
    ).subscribe(result => {
      this.loading = false;
      if (result) this.plan = result;
    });
  }

  loadById(): void {
    if (!this.loadPlanId.trim()) return;
    this.loading = true;
    this.error = null;
    this.plan = null;

    this.aiService.getPlan(this.loadPlanId.trim()).pipe(
      catchError(err => {
        this.error = this.buildErrorMessage(err, 'Plan not found.');
        return of(null);
      })
    ).subscribe(result => {
      this.loading = false;
      if (result) this.plan = result;
    });
  }

  workoutIcon(type: string): string {
    switch (type?.toUpperCase()) {
      case 'RUN': return 'directions_run';
      case 'SWIM': return 'pool';
      case 'BIKE': case 'CYCLE': return 'directions_bike';
      case 'REST': return 'hotel';
      case 'STRENGTH': return 'fitness_center';
      default: return 'sports';
    }
  }

  trackDate(_: number, day: { date: string }): string {
    return day.date;
  }

  trackIndex(i: number): number {
    return i;
  }

  private getMonday(): string {
    const d = new Date();
    const day = d.getDay();
    const diff = (day === 0 ? -6 : 1 - day);
    d.setDate(d.getDate() + diff);
    return d.toISOString().split('T')[0];
  }

  private buildErrorMessage(err: unknown, fallback: string): string {
    if (!(err instanceof HttpErrorResponse)) {
      return fallback;
    }

    let detail = '';
    if (typeof err.error === 'string') {
      detail = err.error;
    } else if (err.error?.message) {
      detail = err.error.message;
    } else if (err.error?.error) {
      detail = err.error.error;
    }

    const statusPart = err.status ? `HTTP ${err.status}` : 'HTTP error';
    return detail ? `${statusPart}: ${detail}` : `${statusPart}: ${fallback}`;
  }
}
