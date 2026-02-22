import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { ApiService } from '../../services/api.service';
import { BodyMeasurement } from '../../models/competition.model';

@Component({
  selector: 'app-body-measurement',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  templateUrl: './body-measurement.component.html',
  styleUrl: './body-measurement.component.scss'
})
export class BodyMeasurementComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  measurements: BodyMeasurement[] = [];
  loading = true;
  saving = false;

  /** null  → form hidden, -1 → new entry, N → editing existing ID N */
  editingId: number | null = null;

  form!: FormGroup;

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadMeasurements();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Form ───────────────────────────────────────────────────────────────────

  private buildForm(): void {
    this.form = this.fb.group({
      measuredAt:       [this.todayIso(), Validators.required],
      weightKg:         [null, [Validators.min(0)]],
      fatPercentage:    [null, [Validators.min(0), Validators.max(100)]],
      waterPercentage:  [null, [Validators.min(0), Validators.max(100)]],
      muscleMassKg:     [null, [Validators.min(0)]],
      boneMassKg:       [null, [Validators.min(0)]],
      visceralFatLevel: [null, [Validators.min(0)]],
      metabolicAge:     [null, [Validators.min(0)]],
      bmi:              [null, [Validators.min(0)]],
      notes:            [null],
    });
  }

  private todayIso(): string {
    return new Date().toISOString().split('T')[0];
  }

  // ─── Data loading ────────────────────────────────────────────────────────────

  loadMeasurements(): void {
    this.loading = true;
    this.apiService.getBodyMeasurements()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.measurements = data;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.snackBar.open('Fehler beim Laden der Messungen', 'Schließen', { duration: 3000 });
        }
      });
  }

  // ─── Form open / close ───────────────────────────────────────────────────────

  openNewForm(): void {
    this.editingId = -1;
    this.form.reset({
      measuredAt:       this.todayIso(),
      weightKg:         null,
      fatPercentage:    null,
      waterPercentage:  null,
      muscleMassKg:     null,
      boneMassKg:       null,
      visceralFatLevel: null,
      metabolicAge:     null,
      bmi:              null,
      notes:            null,
    });
  }

  openEditForm(measurement: BodyMeasurement): void {
    this.editingId = measurement.id ?? -1;
    this.form.setValue({
      measuredAt:       measurement.measuredAt,
      weightKg:         measurement.weightKg         ?? null,
      fatPercentage:    measurement.fatPercentage    ?? null,
      waterPercentage:  measurement.waterPercentage  ?? null,
      muscleMassKg:     measurement.muscleMassKg     ?? null,
      boneMassKg:       measurement.boneMassKg       ?? null,
      visceralFatLevel: measurement.visceralFatLevel ?? null,
      metabolicAge:     measurement.metabolicAge     ?? null,
      bmi:              measurement.bmi              ?? null,
      notes:            measurement.notes            ?? null,
    });
  }

  cancelForm(): void {
    this.editingId = null;
    this.form.reset();
  }

  // ─── Save ────────────────────────────────────────────────────────────────────

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();

    // Convert Date object from datepicker to YYYY-MM-DD string
    const measuredAt: string = raw.measuredAt instanceof Date
      ? raw.measuredAt.toISOString().split('T')[0]
      : raw.measuredAt;

    const payload: BodyMeasurement = {
      measuredAt,
      weightKg:         this.toNumber(raw.weightKg),
      fatPercentage:    this.toNumber(raw.fatPercentage),
      waterPercentage:  this.toNumber(raw.waterPercentage),
      muscleMassKg:     this.toNumber(raw.muscleMassKg),
      boneMassKg:       this.toNumber(raw.boneMassKg),
      visceralFatLevel: this.toNumber(raw.visceralFatLevel),
      metabolicAge:     this.toNumber(raw.metabolicAge),
      bmi:              this.toNumber(raw.bmi),
      notes:            raw.notes || undefined,
    };

    this.saving = true;
    const isNew = this.editingId === -1;

    const request$ = isNew
      ? this.apiService.createBodyMeasurement(payload)
      : this.apiService.updateBodyMeasurement(this.editingId!, payload);

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.editingId = null;
        this.snackBar.open(
          isNew ? 'Messung gespeichert' : 'Messung aktualisiert',
          'Schließen',
          { duration: 3000 }
        );
        this.loadMeasurements();
      },
      error: () => {
        this.saving = false;
        this.snackBar.open('Fehler beim Speichern', 'Schließen', { duration: 3000 });
      }
    });
  }

  // ─── Delete ──────────────────────────────────────────────────────────────────

  deleteMeasurement(measurement: BodyMeasurement): void {
    if (!measurement.id) return;
    if (!confirm(`Messung vom ${this.formatDate(measurement.measuredAt)} wirklich löschen?`)) return;

    this.apiService.deleteBodyMeasurement(measurement.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.snackBar.open('Messung gelöscht', 'Schließen', { duration: 3000 });
          this.loadMeasurements();
        },
        error: () => {
          this.snackBar.open('Fehler beim Löschen', 'Schließen', { duration: 3000 });
        }
      });
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private toNumber(value: unknown): number | undefined {
    if (value === null || value === undefined || value === '') return undefined;
    const n = Number(value);
    return isNaN(n) ? undefined : n;
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const [year, month, day] = dateString.split('-');
    return `${day}.${month}.${year}`;
  }

  isEditing(measurement: BodyMeasurement): boolean {
    return this.editingId === measurement.id;
  }
}
