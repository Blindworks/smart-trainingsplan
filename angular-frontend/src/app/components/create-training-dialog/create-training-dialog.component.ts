import { Component, DestroyRef, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, EMPTY, finalize, map, startWith, switchMap, tap } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Training, TrainingDescription, TrainingImpactRequest, TrainingImpactResponse } from '../../models/competition.model';

export interface CreateTrainingDialogData {
  date: string;       // YYYY-MM-DD
  training?: Training; // wenn gesetzt -> Edit-Modus
}

@Component({
  selector: 'app-create-training-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatExpansionModule
  ],
  templateUrl: './create-training-dialog.component.html',
  styleUrl: './create-training-dialog.component.scss'
})
export class CreateTrainingDialogComponent {
  form: FormGroup;
  saving = false;
  isEditMode: boolean;
  impactPreview: TrainingImpactResponse | null = null;
  impactLoading = false;
  impactError: string | null = null;

  private readonly destroyRef = inject(DestroyRef);

  trainingTypes = [
    { value: 'endurance', label: 'Ausdauer' },
    { value: 'speed', label: 'Schnelligkeit' },
    { value: 'interval', label: 'Intervall' },
    { value: 'fartlek', label: 'Fahrtspiel' },
    { value: 'strength', label: 'Kraft' },
    { value: 'recovery', label: 'Regeneration' },
    { value: 'swimming', label: 'Schwimmen' },
    { value: 'cycling', label: 'Radfahren' },
    { value: 'race', label: 'Wettkampf' },
    { value: 'general', label: 'Allgemein' }
  ];

  intensityLevels = [
    { value: 'low', label: 'Niedrig' },
    { value: 'medium', label: 'Mittel' },
    { value: 'high', label: 'Hoch' },
    { value: 'recovery', label: 'Regeneration' },
    { value: 'rest', label: 'Ruhetag' }
  ];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CreateTrainingDialogComponent>,
    private apiService: ApiService,
    private authService: AuthService,
    @Inject(MAT_DIALOG_DATA) public data: CreateTrainingDialogData
  ) {
    this.isEditMode = !!data.training;
    const t = data.training;
    const desc = t?.trainingDescription;

    this.form = this.fb.group({
      name:                 [t?.name ?? '',                              Validators.required],
      trainingType:         [t?.trainingType ?? 'endurance',            Validators.required],
      intensityLevel:       [t?.intensityLevel ?? 'medium',             Validators.required],
      durationMinutes:      [t?.durationMinutes ?? null,                [Validators.min(1), Validators.max(999)]],
      // TrainingDescription fields
      descName:                [desc?.name ?? ''],
      detailedInstructions:    [desc?.detailedInstructions ?? ''],
      warmupInstructions:      [desc?.warmupInstructions ?? ''],
      cooldownInstructions:    [desc?.cooldownInstructions ?? ''],
      equipment:               [desc?.equipment ?? ''],
      tips:                    [desc?.tips ?? ''],
      difficultyLevel:         [desc?.difficultyLevel ?? '']
    });

    this.setupImpactPreview();
  }

  formatDate(dateString: string): string {
    const [year, month, day] = dateString.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return date.toLocaleDateString('de-DE', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  hasDescriptionDetails(): boolean {
    const v = this.form.value;
    return !!(v.detailedInstructions?.trim() || v.warmupInstructions?.trim() ||
              v.cooldownInstructions?.trim() || v.equipment?.trim() ||
              v.tips?.trim() || v.difficultyLevel?.trim());
  }

  formatInjuryRisk(risk?: 'LOW' | 'MEDIUM' | 'HIGH'): string {
    switch (risk) {
      case 'LOW':
        return 'Low';
      case 'MEDIUM':
        return 'Medium';
      case 'HIGH':
        return 'High';
      default:
        return '-';
    }
  }

  onSave(): void {
    if (this.form.invalid || this.saving) return;

    this.saving = true;
    const v = this.form.value;
    const trim = (s: string) => s?.trim() || undefined;

    const descName             = trim(v.descName);
    const detailedInstructions = trim(v.detailedInstructions);
    const warmupInstructions   = trim(v.warmupInstructions);
    const cooldownInstructions = trim(v.cooldownInstructions);
    const equipment            = trim(v.equipment);
    const tips                 = trim(v.tips);
    const difficultyLevel      = trim(v.difficultyLevel);

    const hasDesc = descName || detailedInstructions || warmupInstructions ||
                    cooldownInstructions || equipment || tips || difficultyLevel;

    const trainingDescription: TrainingDescription | undefined = hasDesc
      ? { name: descName ?? '', detailedInstructions, warmupInstructions,
          cooldownInstructions, equipment, tips, difficultyLevel }
      : undefined;

    const training: Training = {
      name:             v.name.trim(),
      trainingDate:     this.data.date,
      trainingType:     v.trainingType,
      intensityLevel:   v.intensityLevel,
      durationMinutes:  v.durationMinutes || undefined,
      isCompleted:      this.data.training?.isCompleted ?? false,
      trainingDescription
    };

    const request$ = this.isEditMode
      ? this.apiService.updateTraining(this.data.training!.id!, training)
      : this.apiService.createTraining(training);

    request$.subscribe({
      next: (result) => this.dialogRef.close(result),
      error: () => { this.saving = false; }
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  private setupImpactPreview(): void {
    this.form.valueChanges.pipe(
      startWith(this.form.value),
      debounceTime(350),
      map(() => this.buildImpactRequest()),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      switchMap((request) => {
        if (!request) {
          this.impactPreview = null;
          this.impactError = null;
          this.impactLoading = false;
          return EMPTY;
        }

        this.impactLoading = true;
        return this.apiService.getTrainingImpact(request).pipe(
          tap((preview) => {
            this.impactPreview = preview;
            this.impactError = null;
          }),
          catchError(() => {
            this.impactPreview = null;
            this.impactError = 'Preview currently unavailable';
            return EMPTY;
          }),
          finalize(() => {
            this.impactLoading = false;
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  private buildImpactRequest(): TrainingImpactRequest | null {
    const currentUserId = this.authService.getCurrentUserId();
    const durationValue = Number(this.form.get('durationMinutes')?.value);

    if (!currentUserId || !Number.isFinite(durationValue) || durationValue < 1) {
      return null;
    }

    const inputName = (this.form.get('name')?.value ?? '').trim();
    const intensity = String(this.form.get('intensityLevel')?.value ?? 'medium');
    const zone = this.resolvePreviewZone(intensity);

    const baseName = inputName || 'Workout';
    const activityName = /\bZ[1-5]\b/i.test(baseName)
      ? baseName
      : `${baseName} ${zone}`;

    return {
      userId: String(currentUserId),
      workout: {
        date: this.data.date,
        activityName,
        distanceKm: null,
        durationMinutes: Math.round(durationValue),
        averagePaceSecondsPerKm: null,
        averageHeartRate: null
      }
    };
  }

  private resolvePreviewZone(intensity: string): string {
    switch (intensity) {
      case 'high':
        return 'Z4';
      case 'medium':
        return 'Z3';
      case 'low':
        return 'Z2';
      case 'recovery':
      case 'rest':
        return 'Z1';
      default:
        return 'Z2';
    }
  }
}
