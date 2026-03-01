import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

import { ApiService } from '../../services/api.service';
import { Competition } from '../../models/competition.model';

export interface RegistrationDialogData {
  competition: Competition;
  isEditing: boolean;
}

const DISTANCES_KM: Record<string, number> = {
  '5K': 5,
  '10K': 10,
  'Halbmarathon': 21.0975,
  'Marathon': 42.195,
  '50K': 50,
  '100K': 100,
};

function timeFormatValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const pattern = /^\d{1,2}:\d{2}:\d{2}$/;
  if (!pattern.test(control.value)) {
    return { invalidTimeFormat: true };
  }
  const parts = (control.value as string).split(':');
  const minutes = parseInt(parts[1]);
  const seconds = parseInt(parts[2]);
  if (minutes > 59 || seconds > 59) {
    return { invalidTimeFormat: true };
  }
  return null;
}

@Component({
  selector: 'app-registration-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  templateUrl: './registration-dialog.component.html',
  styleUrl: './registration-dialog.component.scss',
})
export class RegistrationDialogComponent implements OnInit {
  form!: FormGroup;
  saving = false;
  errorMessage = '';
  calculatedPace = '';

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private dialogRef: MatDialogRef<RegistrationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RegistrationDialogData,
  ) {}

  ngOnInit(): void {
    const c = this.data.competition;
    this.form = this.fb.group({
      targetTime: [c.targetTime ?? '', [timeFormatValidator]],
      registeredWithOrganizer: [c.registeredWithOrganizer ?? false],
      ranking: [c.ranking ?? ''],
    });

    this.form.get('targetTime')!.valueChanges.subscribe(() => this.updatePace());
    if (c.targetTime) this.updatePace();
  }

  get distanceKm(): number | null {
    const type = this.data.competition.type;
    return type ? (DISTANCES_KM[type] ?? null) : null;
  }

  get hasKnownDistance(): boolean {
    return this.distanceKm !== null;
  }

  private updatePace(): void {
    const raw = this.form.get('targetTime')!.value as string;
    const distance = this.distanceKm;
    if (!raw || !distance || this.form.get('targetTime')!.invalid) {
      this.calculatedPace = '';
      return;
    }
    const parts = raw.split(':');
    const hours = parseInt(parts[0]) || 0;
    const minutes = parseInt(parts[1]) || 0;
    const seconds = parseInt(parts[2]) || 0;
    const totalSeconds = hours * 3600 + minutes * 60 + seconds;
    if (totalSeconds <= 0) { this.calculatedPace = ''; return; }
    const paceSeconds = totalSeconds / distance;
    const paceMin = Math.floor(paceSeconds / 60);
    const paceSec = Math.round(paceSeconds % 60);
    this.calculatedPace = `${paceMin}:${paceSec.toString().padStart(2, '0')} min/km`;
  }

  onCancel(): void {
    if (!this.saving) this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving) return;
    this.saving = true;
    this.errorMessage = '';

    const v = this.form.getRawValue();
    const payload = {
      targetTime: v.targetTime || undefined,
      registeredWithOrganizer: v.registeredWithOrganizer,
      ranking: v.ranking || undefined,
    };

    const competitionId = this.data.competition.id!;
    const op = this.data.isEditing
      ? this.apiService.updateCompetitionRegistration(competitionId, payload)
      : this.apiService.registerForCompetition(competitionId, payload);

    op.subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.errorMessage = 'Fehler beim Speichern der Anmeldung.';
        this.saving = false;
      },
    });
  }
}
