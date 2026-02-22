import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { Competition, User } from '../../models/competition.model';
import { StravaStatus, StravaActivity } from '../../models/strava.model';

@Component({
  selector: 'app-profile',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    DatePipe
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  loading = true;
  stravaLoading = false;
  activitiesLoading = false;

  competitions: Competition[] = [];
  totalTrainings = 0;
  completedTrainings = 0;

  stravaStatus: StravaStatus | null = null;
  stravaActivities: StravaActivity[] = [];

  user: User | null = null;
  editMode = false;
  saving = false;
  editUsername = '';
  editEmail = '';
  editFirstName = '';
  editLastName = '';
  editDateOfBirth = '';
  editHeightCm: number | null = null;
  editWeightKg: number | null = null;
  editMaxHeartRate: number | null = null;

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadStravaStatus();
    this.loadUser();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadStats(): void {
    this.loading = true;
    forkJoin({
      competitions: this.apiService.getAllCompetitions().pipe(catchError(() => of([]))),
      trainings: this.apiService.getAllTrainings().pipe(catchError(() => of([])))
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ competitions, trainings }) => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        this.competitions = competitions.filter(c => {
          const dateStr = c.date || c.targetDate;
          return dateStr ? new Date(dateStr) >= today : true;
        });
        this.totalTrainings = trainings.length;
        this.completedTrainings = trainings.filter(t => t.isCompleted || t.completed).length;
        this.loading = false;
      });
  }

  loadStravaStatus(): void {
    this.apiService.getStravaStatus()
      .pipe(takeUntil(this.destroy$), catchError(() => of(null)))
      .subscribe(status => {
        this.stravaStatus = status;
        if (status?.connected) {
          this.loadStravaActivities();
        }
      });
  }

  loadStravaActivities(): void {
    this.activitiesLoading = true;
    const endDate = new Date().toISOString().split('T')[0];
    const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

    this.apiService.getStravaActivities(startDate, endDate)
      .pipe(takeUntil(this.destroy$), catchError(() => of([])))
      .subscribe(activities => {
        this.stravaActivities = activities.slice(0, 5);
        this.activitiesLoading = false;
      });
  }

  connectStrava(): void {
    this.stravaLoading = true;
    this.apiService.getStravaAuthUrl()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ url }) => {
          window.location.href = url;
        },
        error: () => {
          this.snackBar.open('Strava-Verbindung fehlgeschlagen', 'Schließen', { duration: 3000 });
          this.stravaLoading = false;
        }
      });
  }

  disconnectStrava(): void {
    this.stravaLoading = true;
    this.apiService.disconnectStrava()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.stravaStatus = null;
          this.stravaActivities = [];
          this.stravaLoading = false;
          this.snackBar.open('Strava getrennt', 'Schließen', { duration: 3000 });
        },
        error: () => {
          this.snackBar.open('Fehler beim Trennen', 'Schließen', { duration: 3000 });
          this.stravaLoading = false;
        }
      });
  }

  formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return h > 0 ? `${h}h ${m}min` : `${m}min`;
  }

  formatPace(speedMs: number): string {
    if (!speedMs || speedMs === 0) return '–';
    const paceSecPerKm = 1000 / speedMs;
    const m = Math.floor(paceSecPerKm / 60);
    const s = Math.floor(paceSecPerKm % 60);
    return `${m}:${s.toString().padStart(2, '0')} /km`;
  }

  formatDistance(meters: number): string {
    return (meters / 1000).toFixed(1) + ' km';
  }

  get completionRate(): number {
    if (this.totalTrainings === 0) return 0;
    return Math.round((this.completedTrainings / this.totalTrainings) * 100);
  }

  getInitials(name?: string): string {
    const resolved = this.user?.username || name;
    if (!resolved) return 'AT';
    return resolved.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  loadUser(): void {
    this.apiService.getUsers()
      .pipe(takeUntil(this.destroy$), catchError(() => of([])))
      .subscribe(users => {
        if (users.length > 0) this.user = users[0];
      });
  }

  startEdit(): void {
    this.editUsername = this.user?.username ?? '';
    this.editEmail = this.user?.email ?? '';
    this.editFirstName = this.user?.firstName ?? '';
    this.editLastName = this.user?.lastName ?? '';
    this.editDateOfBirth = this.user?.dateOfBirth ?? '';
    this.editHeightCm = this.user?.heightCm ?? null;
    this.editWeightKg = this.user?.weightKg ?? null;
    this.editMaxHeartRate = this.user?.maxHeartRate ?? null;
    this.editMode = true;
  }

  cancelEdit(): void {
    this.editMode = false;
  }

  saveUser(): void {
    if (!this.user?.id) return;
    this.saving = true;
    this.apiService.updateUser(this.user.id, {
      username: this.editUsername,
      email: this.editEmail,
      firstName: this.editFirstName || undefined,
      lastName: this.editLastName || undefined,
      dateOfBirth: this.editDateOfBirth || undefined,
      heightCm: this.editHeightCm,
      weightKg: this.editWeightKg,
      maxHeartRate: this.editMaxHeartRate
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: updated => {
          this.user = updated;
          this.editMode = false;
          this.saving = false;
          this.snackBar.open('Profil gespeichert', 'Schließen', { duration: 3000 });
        },
        error: () => {
          this.snackBar.open('Fehler beim Speichern', 'Schließen', { duration: 3000 });
          this.saving = false;
        }
      });
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      Run: 'directions_run',
      Ride: 'directions_bike',
      Swim: 'pool',
      Walk: 'directions_walk',
      Hike: 'hiking',
      WeightTraining: 'fitness_center'
    };
    return icons[type] ?? 'sports';
  }
}
