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
import { BloodPressure } from '../../models/competition.model';

@Component({
  selector: 'app-blood-pressure',
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
  templateUrl: './blood-pressure.component.html',
  styleUrl: './blood-pressure.component.scss'
})
export class BloodPressureComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  measurements: BloodPressure[] = [];
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
      measuredAt:          [this.todayIso(), Validators.required],
      systolicPressure:    [null, [Validators.required, Validators.min(0), Validators.max(300)]],
      diastolicPressure:   [null, [Validators.required, Validators.min(0), Validators.max(200)]],
      pulseAtMeasurement:  [null, [Validators.min(0), Validators.max(300)]],
      notes:               [null],
    });
  }

  private todayIso(): string {
    return this.toIsoDate(new Date());
  }

  private toIsoDate(value: string | Date): string {
    if (value instanceof Date) {
      const year = value.getFullYear();
      const month = String(value.getMonth() + 1).padStart(2, '0');
      const day = String(value.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
    return value;
  }

  // ─── Data loading ────────────────────────────────────────────────────────────

  loadMeasurements(): void {
    this.loading = true;
    this.apiService.getBloodPressures()
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
      measuredAt:         this.todayIso(),
      systolicPressure:   null,
      diastolicPressure:  null,
      pulseAtMeasurement: null,
      notes:              null,
    });
  }

  openEditForm(measurement: BloodPressure): void {
    this.editingId = measurement.id ?? -1;
    this.form.setValue({
      measuredAt:         measurement.measuredAt,
      systolicPressure:   measurement.systolicPressure,
      diastolicPressure:  measurement.diastolicPressure,
      pulseAtMeasurement: measurement.pulseAtMeasurement  ?? null,
      notes:              measurement.notes               ?? null,
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
    const measuredAt: string = this.toIsoDate(raw.measuredAt);

    const payload: BloodPressure = {
      measuredAt,
      systolicPressure:   Number(raw.systolicPressure),
      diastolicPressure:  Number(raw.diastolicPressure),
      pulseAtMeasurement: this.toNumber(raw.pulseAtMeasurement),
      notes:              raw.notes || undefined,
    };

    this.saving = true;
    const isNew = this.editingId === -1;

    const request$ = isNew
      ? this.apiService.createBloodPressure(payload)
      : this.apiService.updateBloodPressure(this.editingId!, payload);

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

  deleteMeasurement(measurement: BloodPressure): void {
    if (!measurement.id) return;
    if (!confirm(`Messung vom ${this.formatDate(measurement.measuredAt)} wirklich löschen?`)) return;

    this.apiService.deleteBloodPressure(measurement.id)
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

  isEditing(measurement: BloodPressure): boolean {
    return this.editingId === measurement.id;
  }
}
