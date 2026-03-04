import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';

import { ApiService } from '../../services/api.service';
import { Training, TrainingDescription } from '../../models/competition.model';

export interface CreateTrainingDialogData {
  date: string;       // YYYY-MM-DD
  training?: Training; // wenn gesetzt → Edit-Modus
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
}
