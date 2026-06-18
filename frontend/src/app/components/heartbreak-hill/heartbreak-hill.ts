import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { HeartbreakHillService } from '../../services/heartbreak-hill.service';
import {
  ActivityType, SegmentChallenge, LeaderboardEntry, EffortResult, Gender
} from '../../models/heartbreak-hill.model';
import { buildElevationProfile, ElevationProfile, formatGap } from './heartbreak-hill.util';
import {
  SHARE_W, SHARE_H, ShareTemplate, ShareImageData, ShareLabels,
  formatTempo, shareFileName, drawShareImage, loadImage
} from './share-image.util';
import { Heartbreak3d } from './heartbreak-3d/heartbreak-3d';
import { isWebglAvailable } from './heartbreak-3d/heartbreak-3d.util';
import { loadStoredEffort, saveStoredEffort, clearStoredEffort } from './heartbreak-storage.util';

@Component({
  selector: 'app-heartbreak-hill',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, Heartbreak3d],
  templateUrl: './heartbreak-hill.html',
  styleUrl: './heartbreak-hill.scss'
})
export class HeartbreakHill implements OnInit {
  private readonly service = inject(HeartbreakHillService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  // viewBox dims for the hero elevation profile
  readonly profileWidth = 1200;
  readonly profileHeight = 360;

  /** Exposed so the template can format the leaderboard gap column. */
  readonly formatGap = formatGap;

  loading = signal(true);
  loadError = signal(false);
  challenge = signal<SegmentChallenge | null>(null);

  activeTab = signal<ActivityType>('RIDE');
  leaderboard = signal<LeaderboardEntry[]>([]);
  leaderboardLoading = signal(false);

  // upload state
  selectedFile = signal<File | null>(null);
  displayName = signal('');
  gender = signal<Gender | ''>('');
  birthYear = signal<number | null>(null);
  submitting = signal(false);
  uploadError = signal<string | null>(null);
  result = signal<EffortResult | null>(null);

  /** True when the shown result was restored from localStorage (not a fresh upload). */
  restored = signal(false);
  restoredAt = signal<string | null>(null);

  /** SVG elevation profile derived from the challenge polyline, or null if absent. */
  profile = computed<ElevationProfile | null>(() => {
    const c = this.challenge();
    if (!c?.polylineJson) {
      return null;
    }
    try {
      const pts = JSON.parse(c.polylineJson) as [number, number, number][];
      return buildElevationProfile(pts, this.profileWidth, this.profileHeight);
    } catch {
      return null;
    }
  });

  /** Selectable birth years: referenceYear-5 down to referenceYear-100 (matches backend validation). */
  birthYearOptions = computed<number[]>(() => {
    const c = this.challenge();
    const ref = c?.eventDate ? new Date(c.eventDate).getFullYear() : new Date().getFullYear();
    const years: number[] = [];
    for (let y = ref - 5; y >= ref - 100; y--) {
      years.push(y);
    }
    return years;
  });

  /** Whether to attempt the WebGL hero (falls back to 2D on failure). */
  use3d = signal(isWebglAvailable());

  /** Parsed route polyline points, shared by the 2D profile and the 3D terrain. */
  polylinePoints = computed<[number, number, number][] | null>(() => {
    const c = this.challenge();
    if (!c?.polylineJson) {
      return null;
    }
    try {
      return JSON.parse(c.polylineJson) as [number, number, number][];
    } catch {
      return null;
    }
  });

  ngOnInit(): void {
    this.service.getChallenge().subscribe({
      next: c => {
        this.challenge.set(c);
        this.loading.set(false);
        this.loadLeaderboard();
        this.restoreForCurrentTab();
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      }
    });
  }

  selectTab(type: ActivityType): void {
    if (this.activeTab() === type) {
      return;
    }
    this.activeTab.set(type);
    this.uploadError.set(null);
    this.loadLeaderboard();
    this.restoreForCurrentTab();
  }

  /** Restores the remembered effort (Snapshot) for the active tab, or clears the panel. */
  private restoreForCurrentTab(): void {
    const stored = loadStoredEffort(this.activeTab());
    if (stored) {
      this.result.set(stored.result);
      this.displayName.set(stored.displayName);
      this.restored.set(true);
      this.restoredAt.set(stored.savedAt);
      this.shareTemplate.set('A');
      void this.renderShare();
    } else {
      this.result.set(null);
      this.sharePreviewUrl.set(null);
      this.restored.set(false);
      this.restoredAt.set(null);
    }
  }

  /** Forgets the remembered effort for the active tab and closes the result panel. */
  discardStored(): void {
    clearStoredEffort(this.activeTab());
    this.result.set(null);
    this.sharePreviewUrl.set(null);
    this.restored.set(false);
    this.restoredAt.set(null);
  }

  private loadLeaderboard(): void {
    this.leaderboardLoading.set(true);
    this.service.getLeaderboard(this.activeTab()).subscribe({
      next: entries => {
        this.leaderboard.set(entries);
        this.leaderboardLoading.set(false);
      },
      error: () => {
        this.leaderboard.set([]);
        this.leaderboardLoading.set(false);
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
  }

  onFileDropped(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0] ?? null;
    if (file) {
      this.selectedFile.set(file);
    }
  }

  canSubmit = computed(() =>
    !!this.selectedFile() && this.displayName().trim().length > 0 && !this.submitting());

  submit(): void {
    const file = this.selectedFile();
    if (!file || !this.canSubmit()) {
      return;
    }
    this.submitting.set(true);
    this.uploadError.set(null);
    const g = this.gender() || null;
    this.service.submitEffort(this.activeTab(), this.displayName().trim(), file, g, this.birthYear()).subscribe({
      next: res => {
        this.result.set(res);
        saveStoredEffort(this.activeTab(), res, this.displayName().trim());
        this.restored.set(false);
        this.restoredAt.set(null);
        this.shareTemplate.set('A');
        void this.renderShare();
        this.submitting.set(false);
        this.loadLeaderboard();          // refresh so the new entry shows
      },
      error: (err: HttpErrorResponse) => {
        this.uploadError.set(err.status === 429 ? 'ERROR_RATE_LIMIT' : 'ERROR_UPLOAD');
        this.submitting.set(false);
      }
    });
  }

  /** Highlights the just-submitted effort row in the leaderboard. */
  isMyEffort(entry: LeaderboardEntry): boolean {
    return this.result()?.effortId === entry.effortId;
  }

  /** i18n key for a reference entry's badge, honest about the category. */
  referenceBadgeKey(entry: LeaderboardEntry): string {
    return (entry.category === 'PRO_MEN' || entry.category === 'PRO_WOMEN')
      ? 'HEARTBREAK_HILL.BADGE_PRO'
      : 'HEARTBREAK_HILL.BADGE_REFERENCE';
  }

  goToSignup(): void {
    this.router.navigate(['/signup']);
  }

  // --- share image ---
  readonly shareTemplate = signal<ShareTemplate>('A');
  readonly sharePreviewUrl = signal<string | null>(null);
  readonly canNativeShare = signal<boolean>(
    typeof navigator !== 'undefined' && 'canShare' in navigator);

  private shareCanvas: HTMLCanvasElement | null = null;
  private logoPromise: Promise<HTMLImageElement | null> | null = null;

  private locale(): string {
    return this.translate.currentLang === 'en' ? 'en-GB' : 'de-DE';
  }

  selectTemplate(t: ShareTemplate): void {
    if (this.shareTemplate() === t) {
      return;
    }
    this.shareTemplate.set(t);
    void this.renderShare();
  }

  /** Builds the off-screen 1080×1920 canvas and publishes a preview data URL. */
  private async renderShare(): Promise<void> {
    const r = this.result();
    if (!r) {
      return;
    }
    await document.fonts.ready;
    if (!this.logoPromise) {
      this.logoPromise = loadImage('assets/logo/PACR_logo_dark_text_transparent_cut.png')
        .catch(() => null);
    }
    const logo = await this.logoPromise;

    const canvas = document.createElement('canvas');
    canvas.width = SHARE_W;
    canvas.height = SHARE_H;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }

    const type = this.activeTab();
    const data: ShareImageData = {
      segmentName: this.challenge()?.name ?? 'Heartbreak Hill',
      activityType: type,
      elevation: this.polylinePoints(),
      tempo: formatTempo(type, r.avgSpeedKmh, r.avgPaceSecondsPerKm, this.locale()),
      time: r.elapsedFormatted,
      rank: r.rank,
      totalCount: r.totalCount
    };
    const labels: ShareLabels = {
      tempo: this.translate.instant(
        type === 'RIDE' ? 'HEARTBREAK_HILL.SHARE_LBL_TEMPO' : 'HEARTBREAK_HILL.SHARE_LBL_PACE'),
      time: this.translate.instant('HEARTBREAK_HILL.SHARE_LBL_TIME'),
      rank: this.translate.instant('HEARTBREAK_HILL.SHARE_LBL_RANK'),
      of: this.translate.instant('HEARTBREAK_HILL.SHARE_LBL_OF')
    };

    drawShareImage(ctx, data, this.shareTemplate(), labels, logo);
    this.shareCanvas = canvas;
    this.sharePreviewUrl.set(canvas.toDataURL('image/png'));
  }

  downloadShare(): void {
    const canvas = this.shareCanvas;
    const r = this.result();
    if (!canvas || !r) {
      return;
    }
    canvas.toBlob(blob => {
      if (!blob) {
        return;
      }
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = shareFileName(r.rank, this.activeTab());
      a.click();
      URL.revokeObjectURL(url);
    }, 'image/png');
  }

  shareImage(): void {
    const canvas = this.shareCanvas;
    const r = this.result();
    if (!canvas || !r) {
      return;
    }
    canvas.toBlob(blob => {
      if (!blob) {
        return;
      }
      const file = new File([blob], shareFileName(r.rank, this.activeTab()), { type: 'image/png' });
      const nav = navigator as Navigator & {
        canShare?: (d: { files: File[] }) => boolean;
        share?: (d: { files: File[] }) => Promise<void>;
      };
      if (nav.canShare?.({ files: [file] }) && nav.share) {
        nav.share({ files: [file] }).catch(() => { /* user cancelled */ });
      }
    }, 'image/png');
  }
}
