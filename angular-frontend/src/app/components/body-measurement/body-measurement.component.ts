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
import { BodyMeasurement, BloodPressure } from '../../models/competition.model';

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

  // ─── Körpermessungen ──────────────────────────────────────────────────────
  measurements: BodyMeasurement[] = [];
  loading = true;
  saving = false;
  /** null → form hidden, -1 → new entry, N → editing existing ID N */
  editingId: number | null = null;
  form!: FormGroup;

  // ─── Blutdruck ────────────────────────────────────────────────────────────
  bloodPressures: BloodPressure[] = [];
  bpLoading = true;
  bpSaving = false;
  editingBpId: number | null = null;
  bpForm!: FormGroup;

  constructor(
    private apiService: ApiService,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.buildBpForm();
    this.loadMeasurements();
    this.loadBloodPressures();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Körpermessungen: Form ────────────────────────────────────────────────

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

  loadMeasurements(): void {
    this.loading = true;
    this.apiService.getBodyMeasurements()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.measurements = data; this.loading = false; },
        error: () => {
          this.loading = false;
          this.snackBar.open('Fehler beim Laden der Messungen', 'Schließen', { duration: 3000 });
        }
      });
  }

  openNewForm(): void {
    this.editingBpId = null;
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
    this.editingBpId = null;
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

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const raw = this.form.getRawValue();
    const measuredAt: string = this.toIsoDate(raw.measuredAt);

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
        this.snackBar.open(isNew ? 'Messung gespeichert' : 'Messung aktualisiert', 'Schließen', { duration: 3000 });
        this.loadMeasurements();
      },
      error: () => {
        this.saving = false;
        this.snackBar.open('Fehler beim Speichern', 'Schließen', { duration: 3000 });
      }
    });
  }

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
        error: () => this.snackBar.open('Fehler beim Löschen', 'Schließen', { duration: 3000 })
      });
  }

  isEditing(measurement: BodyMeasurement): boolean {
    return this.editingId === measurement.id;
  }

  // ─── Blutdruck: Form ──────────────────────────────────────────────────────

  private buildBpForm(): void {
    this.bpForm = this.fb.group({
      measuredAt:        [this.todayIso(), Validators.required],
      systolicPressure:  [null, [Validators.required, Validators.min(0), Validators.max(300)]],
      diastolicPressure: [null, [Validators.required, Validators.min(0), Validators.max(200)]],
      pulseAtMeasurement:[null, [Validators.min(0), Validators.max(300)]],
      notes:             [null],
    });
  }

  loadBloodPressures(): void {
    this.bpLoading = true;
    this.apiService.getBloodPressures()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.bloodPressures = data; this.bpLoading = false; },
        error: () => {
          this.bpLoading = false;
          this.snackBar.open('Fehler beim Laden der Blutdruckwerte', 'Schließen', { duration: 3000 });
        }
      });
  }

  openNewBpForm(): void {
    this.editingId = null;
    this.editingBpId = -1;
    this.bpForm.reset({
      measuredAt:         this.todayIso(),
      systolicPressure:   null,
      diastolicPressure:  null,
      pulseAtMeasurement: null,
      notes:              null,
    });
  }

  openEditBpForm(bp: BloodPressure): void {
    this.editingId = null;
    this.editingBpId = bp.id ?? -1;
    this.bpForm.setValue({
      measuredAt:         bp.measuredAt,
      systolicPressure:   bp.systolicPressure,
      diastolicPressure:  bp.diastolicPressure,
      pulseAtMeasurement: bp.pulseAtMeasurement ?? null,
      notes:              bp.notes              ?? null,
    });
  }

  cancelBpForm(): void {
    this.editingBpId = null;
    this.bpForm.reset();
  }

  saveBp(): void {
    if (this.bpForm.invalid) { this.bpForm.markAllAsTouched(); return; }

    const raw = this.bpForm.getRawValue();
    const measuredAt: string = this.toIsoDate(raw.measuredAt);

    const payload: BloodPressure = {
      measuredAt,
      systolicPressure:   Number(raw.systolicPressure),
      diastolicPressure:  Number(raw.diastolicPressure),
      pulseAtMeasurement: this.toNumber(raw.pulseAtMeasurement),
      notes:              raw.notes || undefined,
    };

    this.bpSaving = true;
    const isNew = this.editingBpId === -1;
    const request$ = isNew
      ? this.apiService.createBloodPressure(payload)
      : this.apiService.updateBloodPressure(this.editingBpId!, payload);

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.bpSaving = false;
        this.editingBpId = null;
        this.snackBar.open(isNew ? 'Blutdruckwert gespeichert' : 'Blutdruckwert aktualisiert', 'Schließen', { duration: 3000 });
        this.loadBloodPressures();
      },
      error: () => {
        this.bpSaving = false;
        this.snackBar.open('Fehler beim Speichern', 'Schließen', { duration: 3000 });
      }
    });
  }

  deleteBp(bp: BloodPressure): void {
    if (!bp.id) return;
    if (!confirm(`Blutdruckwert vom ${this.formatDate(bp.measuredAt)} wirklich löschen?`)) return;
    this.apiService.deleteBloodPressure(bp.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.snackBar.open('Blutdruckwert gelöscht', 'Schließen', { duration: 3000 });
          this.loadBloodPressures();
        },
        error: () => this.snackBar.open('Fehler beim Löschen', 'Schließen', { duration: 3000 })
      });
  }

  isEditingBp(bp: BloodPressure): boolean {
    return this.editingBpId === bp.id;
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

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
}
