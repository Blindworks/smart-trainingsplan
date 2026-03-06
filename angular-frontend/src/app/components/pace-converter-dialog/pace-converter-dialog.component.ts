import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSliderModule } from '@angular/material/slider';
import { PaceInputComponent } from '../pace-input/pace-input.component';

@Component({
  selector: 'app-pace-converter-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSliderModule,
    PaceInputComponent
  ],
  templateUrl: './pace-converter-dialog.component.html',
  styleUrl: './pace-converter-dialog.component.scss'
})
export class PaceConverterDialogComponent {
  distanceKm: number | null = 10;
  timeHours: number | null = 0;
  timeMinutes: number | null = 50;
  timeSeconds: number | null = 0;
  paceMinutes: number | null = 5;
  paceSeconds: number | null = 0;
  paceInput = '05:00';
  speedKmh: number | null = 12;
  paceSliderSeconds = 300;
  distanceSliderKm = 10;
  speedSliderKmh = 12;
  errorMessage = '';

  constructor(private dialogRef: MatDialogRef<PaceConverterDialogComponent>) {}

  close(): void {
    this.dialogRef.close();
  }

  calculateFromPaceDistance(): void {
    this.onPaceChange();
  }

  calculateFromDistanceTime(): void {
    this.onTimeChange();
  }

  calculateFromTimeSpeed(): void {
    this.onSpeedChange();
  }

  calculateFromPaceTime(): void {
    this.onPaceChange();
  }

  onPaceChange(): void {
    const paceSec = this.toPaceSeconds(this.paceMinutes, this.paceSeconds);
    if (paceSec <= 0) {
      return;
    }

    this.errorMessage = '';
    this.speedKmh = this.round(3600 / paceSec, 2);
    this.paceSliderSeconds = Math.round(paceSec);
    this.speedSliderKmh = this.speedKmh ?? 0;

    const distance = Number(this.distanceKm) || 0;
    if (distance > 0) {
      this.setTimeFromSeconds(Math.round(distance * paceSec));
    }
  }

  onSpeedChange(): void {
    const speed = Number(this.speedKmh) || 0;
    const distance = Number(this.distanceKm) || 0;
    if (speed <= 0) {
      return;
    }

    this.errorMessage = '';
    const paceSec = 3600 / speed;
    this.setPaceFromSeconds(paceSec);
    this.speedSliderKmh = speed;

    if (distance > 0) {
      this.setTimeFromSeconds(Math.round((distance / speed) * 3600));
    }
  }

  onDistanceChange(): void {
    const distance = Number(this.distanceKm) || 0;
    if (distance <= 0) {
      return;
    }

    const paceSec = this.resolveCurrentPaceSeconds();
    if (paceSec <= 0) {
      return;
    }

    this.errorMessage = '';
    this.distanceSliderKm = distance;
    this.setTimeFromSeconds(Math.round(distance * paceSec));
  }

  onTimeChange(): void {
    const totalSeconds = this.toTotalSeconds(this.timeHours, this.timeMinutes, this.timeSeconds);
    const distance = Number(this.distanceKm) || 0;
    if (totalSeconds <= 0 || distance <= 0) {
      return;
    }

    this.errorMessage = '';
    const paceSec = totalSeconds / distance;
    this.setPaceFromSeconds(paceSec);
    this.speedKmh = this.round(3600 / paceSec, 2);
    this.speedSliderKmh = this.speedKmh ?? 0;
  }

  onDistanceSliderChange(value: number): void {
    this.distanceKm = this.round(value, 2);
    this.onDistanceChange();
  }

  onSpeedSliderChange(value: number): void {
    this.speedKmh = this.round(value, 2);
    this.onSpeedChange();
  }

  onPaceSliderChange(value: number): void {
    const clamped = Math.max(120, Math.min(720, Math.round(value)));
    this.setPaceFromSeconds(clamped);
    this.onPaceChange();
  }

  formatSliderPace(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  private toTotalSeconds(hours: number | null, minutes: number | null, seconds: number | null): number {
    const h = Math.max(0, Number(hours) || 0);
    const m = Math.max(0, Number(minutes) || 0);
    const s = Math.max(0, Number(seconds) || 0);
    return Math.round(h * 3600 + m * 60 + s);
  }

  private setTimeFromSeconds(totalSeconds: number): void {
    const safe = Math.max(0, Math.round(totalSeconds));
    this.timeHours = Math.floor(safe / 3600);
    this.timeMinutes = Math.floor((safe % 3600) / 60);
    this.timeSeconds = safe % 60;
  }

  private toPaceSeconds(minutes: number | null, seconds: number | null): number {
    const m = Math.max(0, Number(minutes) || 0);
    const s = Math.max(0, Number(seconds) || 0);
    return m * 60 + s;
  }

  private setPaceFromSeconds(paceSeconds: number): void {
    const rounded = Math.max(1, Math.round(paceSeconds));
    this.paceMinutes = Math.floor(rounded / 60);
    this.paceSeconds = rounded % 60;
    this.paceSliderSeconds = rounded;
    this.paceInput = this.formatPace(this.paceMinutes, this.paceSeconds);
  }

  private round(value: number, decimals: number): number {
    const factor = Math.pow(10, decimals);
    return Math.round(value * factor) / factor;
  }

  onPaceInputChanged(value: string): void {
    this.paceInput = value ?? '';
    const parsed = this.parsePace(this.paceInput);

    if (!parsed) {
      this.paceMinutes = null;
      this.paceSeconds = null;
      return;
    }

    this.paceMinutes = parsed.minutes;
    this.paceSeconds = parsed.seconds;
    this.onPaceChange();
  }

  private parsePace(value: string): { minutes: number; seconds: number } | null {
    const trimmed = value.trim();
    if (!trimmed) return null;

    const parts = trimmed.split(':');
    if (parts.length !== 2) return null;

    const minutes = Number(parts[0]);
    const seconds = Number(parts[1]);

    if (!Number.isInteger(minutes) || minutes < 0) return null;
    if (!Number.isInteger(seconds) || seconds < 0 || seconds > 59) return null;

    return { minutes, seconds };
  }

  private formatPace(minutes: number | null, seconds: number | null): string {
    const safeMinutes = Math.max(0, Number(minutes) || 0);
    const safeSeconds = Math.max(0, Number(seconds) || 0);
    return `${String(safeMinutes).padStart(2, '0')}:${String(safeSeconds).padStart(2, '0')}`;
  }

  private resolveCurrentPaceSeconds(): number {
    const fromPace = this.toPaceSeconds(this.paceMinutes, this.paceSeconds);
    if (fromPace > 0) {
      return fromPace;
    }

    const speed = Number(this.speedKmh) || 0;
    if (speed > 0) {
      return 3600 / speed;
    }

    return 0;
  }
}
