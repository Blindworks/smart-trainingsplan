import { Component, OnDestroy, OnInit } from '@angular/core';
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
import { MatSelectModule } from '@angular/material/select';
import { Subject, forkJoin, of } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';

import { ApiService } from '../../services/api.service';
import { Competition, PaceZones, User } from '../../models/competition.model';
import { StravaActivity, StravaStatus } from '../../models/strava.model';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { I18nService } from '../../services/i18n.service';

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
    MatSelectModule,
    DatePipe,
    TranslatePipe
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private profileImageObjectUrl: string | null = null;

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
  editHrRest: number | null = null;
  editGender = '';
  profileImageUrl: string | null = null;
  profileImageUploading = false;
  profileImageMessage = '';

  paceZones: PaceZones | null = null;
  paceZoneLoading = false;
  paceZoneSaving = false;
  paceZoneEditMode = false;
  paceRefDistanceM = 10000;
  paceRefLabel = '10K';
  paceRefHours = 0;
  paceRefMinutes = 45;
  paceRefSeconds = 0;

  readonly PRESET_DISTANCES = [
    { label: '1 Mile (1.6 km)', distanceM: 1609 },
    { label: '5K', distanceM: 5000 },
    { label: '10K', distanceM: 10000 },
    { label: 'Half Marathon', distanceM: 21097 },
    { label: 'Marathon', distanceM: 42195 }
  ];

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar,
    private i18nService: I18nService
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadStravaStatus();
    this.loadUser();
    this.loadPaceZones();
  }

  ngOnDestroy(): void {
    this.revokeProfileImageObjectUrl();
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
          this.showSnack('profile.messages.stravaConnectError');
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
          this.showSnack('profile.messages.stravaDisconnected');
        },
        error: () => {
          this.showSnack('profile.messages.stravaDisconnectError');
          this.stravaLoading = false;
        }
      });
  }

  formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) {
      return this.i18nService.t('profile.durationHours', { h, m });
    }
    return this.i18nService.t('profile.durationMinutes', { m });
  }

  formatPace(speedMs: number): string {
    if (!speedMs || speedMs === 0) {
      return '-';
    }
    const paceSecPerKm = 1000 / speedMs;
    const m = Math.floor(paceSecPerKm / 60);
    const s = Math.floor(paceSecPerKm % 60);
    return `${m}:${s.toString().padStart(2, '0')} /km`;
  }

  formatDistance(meters: number): string {
    return `${(meters / 1000).toFixed(1)} km`;
  }

  get completionRate(): number {
    if (this.totalTrainings === 0) {
      return 0;
    }
    return Math.round((this.completedTrainings / this.totalTrainings) * 100);
  }

  getInitials(name?: string): string {
    const resolved = this.user?.username || name;
    if (!resolved) {
      return 'AT';
    }
    return resolved
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  loadUser(): void {
    this.apiService.getMe()
      .pipe(takeUntil(this.destroy$), catchError(() => of(null)))
      .subscribe(user => {
        if (user) {
          this.user = user;
          this.loadProfileImage();
        }
      });
  }

  loadProfileImage(): void {
    if (!this.user?.id) {
      this.revokeProfileImageObjectUrl();
      this.profileImageUrl = null;
      return;
    }

    this.apiService.getProfileImage(this.user.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: blob => {
          this.revokeProfileImageObjectUrl();
          this.profileImageObjectUrl = URL.createObjectURL(blob);
          this.profileImageUrl = this.profileImageObjectUrl;
          this.profileImageMessage = '';
        },
        error: (error) => {
          this.revokeProfileImageObjectUrl();
          this.profileImageUrl = null;
          this.profileImageMessage = error?.status === 404
            ? this.i18nService.t('profile.imageNotUploaded')
            : this.i18nService.t('profile.imageLoadError');
        }
      });
  }

  onProfileImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.user?.id) {
      return;
    }

    this.profileImageUploading = true;
    this.apiService.uploadProfileImage(this.user.id, file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.profileImageUploading = false;
          this.profileImageMessage = '';
          this.showSnack('profile.messages.imageUpdated');
          this.loadUser();
        },
        error: () => {
          this.profileImageUploading = false;
          this.showSnack('profile.messages.imageUploadError');
        }
      });

    input.value = '';
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
    this.editHrRest = this.user?.hrRest ?? null;
    this.editGender = this.user?.gender ?? '';
    this.editMode = true;
  }

  cancelEdit(): void {
    this.editMode = false;
  }

  saveUser(): void {
    if (!this.user?.id) {
      return;
    }

    this.saving = true;
    this.apiService.updateUser(this.user.id, {
      username: this.editUsername,
      email: this.editEmail,
      firstName: this.editFirstName || undefined,
      lastName: this.editLastName || undefined,
      dateOfBirth: this.editDateOfBirth || undefined,
      heightCm: this.editHeightCm,
      weightKg: this.editWeightKg,
      maxHeartRate: this.editMaxHeartRate,
      hrRest: this.editHrRest,
      gender: this.editGender || null
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: updated => {
          this.user = updated;
          this.editMode = false;
          this.saving = false;
          this.showSnack('profile.messages.profileSaved');
        },
        error: () => {
          this.showSnack('profile.messages.profileSaveError');
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

  getUserStatusLabel(status?: User['status']): string {
    switch (status) {
      case 'EMAIL_VERIFICATION_PENDING':
        return this.i18nService.t('profile.statusEmailPending');
      case 'ADMIN_APPROVAL_PENDING':
        return this.i18nService.t('profile.statusAdminPending');
      case 'BLOCKED':
        return this.i18nService.t('profile.statusBlocked');
      case 'INACTIVE':
        return this.i18nService.t('profile.statusInactive');
      case 'ACTIVE':
        return this.i18nService.t('profile.statusActive');
      default:
        return this.i18nService.t('profile.statusUnknown');
    }
  }

  loadPaceZones(): void {
    this.paceZoneLoading = true;
    this.apiService.getPaceZones()
      .pipe(takeUntil(this.destroy$), catchError(() => of(null)))
      .subscribe(zones => {
        this.paceZones = zones;
        if (zones) {
          this.prefillPaceZoneForm(zones);
        }
        this.paceZoneLoading = false;
      });
  }

  private prefillPaceZoneForm(zones: PaceZones): void {
    const preset = this.PRESET_DISTANCES.find(p => p.distanceM === zones.referenceDistanceM);
    if (preset) {
      this.paceRefDistanceM = preset.distanceM;
      this.paceRefLabel = preset.label;
    } else if (zones.referenceDistanceM) {
      this.paceRefDistanceM = zones.referenceDistanceM;
      this.paceRefLabel = zones.referenceLabel ?? `${(zones.referenceDistanceM / 1000).toFixed(1)} km`;
    }
    if (zones.referenceTimeSeconds) {
      const t = zones.referenceTimeSeconds;
      this.paceRefHours = Math.floor(t / 3600);
      this.paceRefMinutes = Math.floor((t % 3600) / 60);
      this.paceRefSeconds = t % 60;
    }
  }

  onPaceDistanceChange(distanceM: number): void {
    this.paceRefDistanceM = distanceM;
    const preset = this.PRESET_DISTANCES.find(p => p.distanceM === distanceM);
    this.paceRefLabel = preset ? preset.label : `${(distanceM / 1000).toFixed(1)} km`;
  }

  savePaceZones(): void {
    const totalSeconds = (this.paceRefHours || 0) * 3600
      + (this.paceRefMinutes || 0) * 60
      + (this.paceRefSeconds || 0);

    if (!this.paceRefDistanceM || totalSeconds <= 0) {
      this.showSnack('profile.messages.paceMissingInput');
      return;
    }

    this.paceZoneSaving = true;
    this.apiService.setPaceZoneReference(this.paceRefDistanceM, totalSeconds, this.paceRefLabel)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: zones => {
          this.paceZones = zones;
          this.paceZoneEditMode = false;
          this.paceZoneSaving = false;
          this.showSnack('profile.messages.paceSaved');
        },
        error: () => {
          this.showSnack('profile.messages.paceSaveError');
          this.paceZoneSaving = false;
        }
      });
  }

  formatPaceFromSeconds(secPerKm: number | null): string {
    if (secPerKm == null) {
      return '8';
    }
    const m = Math.floor(secPerKm / 60);
    const s = secPerKm % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  getPaceZoneColor(zone: number): string {
    const colors: Record<number, string> = {
      1: '#64B5F6',
      2: '#4CAF50',
      3: '#FFC107',
      4: '#FF7043',
      5: '#E53935',
      6: '#9C27B0'
    };
    return colors[zone] ?? '#888';
  }

  private revokeProfileImageObjectUrl(): void {
    if (this.profileImageObjectUrl) {
      URL.revokeObjectURL(this.profileImageObjectUrl);
      this.profileImageObjectUrl = null;
    }
  }

  private showSnack(messageKey: string, duration = 3000): void {
    this.snackBar.open(this.i18nService.t(messageKey), this.i18nService.t('common.close'), { duration });
  }
}
