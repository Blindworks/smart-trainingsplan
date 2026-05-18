import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { BuddyRunService, UserBuddyPreferencesDto } from '../../services/buddy-run.service';

const WEEKDAYS = ['MO', 'DI', 'MI', 'DO', 'FR', 'SA', 'SO'];

@Component({
  selector: 'app-buddy-preferences',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule],
  templateUrl: './buddy-preferences.html',
  styleUrl: './buddy-preferences.scss'
})
export class BuddyPreferences implements OnInit {
  private readonly buddyService = inject(BuddyRunService);

  prefs = signal<UserBuddyPreferencesDto | null>(null);
  loading = signal(true);
  saving = signal(false);
  saved = signal(false);

  weekdays = WEEKDAYS;
  selectedDays: Record<string, boolean> = {};

  /** Free-form mm:ss input bound to the form. Empty string clears the buddy pace. */
  paceInput = signal<string>('');
  /** Tolerance signal mirroring prefs().paceTolerancePercent so the range is reactive on slider changes. */
  toleranceInput = signal<number>(15);
  paceError = signal<string | null>(null);

  /** Reactive min/max pace window derived from buddy pace + tolerance. */
  paceRange = computed<{ min: string; max: string } | null>(() => {
    const parsed = this.parsePace(this.paceInput());
    if (parsed == null) return null;
    const tol = Math.max(0, Math.min(100, this.toleranceInput()));
    const delta = Math.round(parsed * (tol / 100));
    return {
      min: this.formatPace(parsed - delta),
      max: this.formatPace(parsed + delta)
    };
  });

  ngOnInit(): void {
    this.buddyService.getPreferences().subscribe({
      next: p => {
        this.prefs.set(p);
        const days = (p.availableWeekdays || '').split(',').map(d => d.trim().toUpperCase());
        WEEKDAYS.forEach(d => this.selectedDays[d] = days.includes(d));
        this.paceInput.set(this.formatPace(p.targetPaceSecPerKm));
        this.toleranceInput.set(p.paceTolerancePercent);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  toggleDay(d: string): void {
    this.selectedDays[d] = !this.selectedDays[d];
  }

  formatPace(sec: number | null | undefined): string {
    if (sec == null) return '';
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  /** Parse "mm:ss" / "m:ss" to seconds. Returns null on empty, undefined on invalid. */
  parsePace(input: string): number | null | undefined {
    const trimmed = (input ?? '').trim();
    if (trimmed === '') return null;
    const m = trimmed.match(/^(\d{1,2}):([0-5]\d)$/);
    if (!m) return undefined;
    const sec = parseInt(m[1], 10) * 60 + parseInt(m[2], 10);
    if (sec < 120 || sec > 720) return undefined; // 2:00 – 12:00 min/km
    return sec;
  }

  save(): void {
    const p = this.prefs(); if (!p) return;
    this.paceError.set(null);

    const parsed = this.parsePace(this.paceInput());
    if (parsed === undefined) {
      this.paceError.set('INVALID');
      return;
    }

    this.saving.set(true);
    this.saved.set(false);
    const updated: UserBuddyPreferencesDto = {
      ...p,
      paceTolerancePercent: this.toleranceInput(),
      availableWeekdays: WEEKDAYS.filter(d => this.selectedDays[d]).join(',') || null,
      targetPaceSecPerKm: parsed
    };
    this.buddyService.updatePreferences(updated).subscribe({
      next: r => {
        this.prefs.set(r);
        this.saving.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 2500);
      },
      error: () => this.saving.set(false)
    });
  }
}
