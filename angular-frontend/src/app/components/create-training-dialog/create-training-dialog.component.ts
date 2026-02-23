import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { ApiService } from '../../services/api.service';
import { Training } from '../../models/competition.model';

export interface CreateTrainingDialogData {
  date: string; // YYYY-MM-DD
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
    MatSelectModule
  ],
  templateUrl: './create-training-dialog.component.html',
  styleUrl: './create-training-dialog.component.scss'
})
export class CreateTrainingDialogComponent {
  form: FormGroup;
  saving = false;

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
    this.form = this.fb.group({
      name: ['', Validators.required],
      trainingType: ['endurance', Validators.required],
      intensityLevel: ['medium', Validators.required],
      durationMinutes: [null, [Validators.min(1), Validators.max(999)]],
      description: ['']
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

  onSave(): void {
    if (this.form.invalid || this.saving) return;

    this.saving = true;
    const value = this.form.value;

    const descriptionText = value.description?.trim();
    const training: Training = {
      name: value.name.trim(),
      trainingDate: this.data.date,
      trainingType: value.trainingType,
      intensityLevel: value.intensityLevel,
      durationMinutes: value.durationMinutes || undefined,
      isCompleted: false,
      trainingDescription: descriptionText
        ? { name: descriptionText }
        : undefined
    };

    this.apiService.createTraining(training).subscribe({
      next: (created) => {
        this.dialogRef.close(created);
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
