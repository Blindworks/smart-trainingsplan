import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { HeartbreakHillService } from '../../services/heartbreak-hill.service';
import {
  ActivityType, SegmentChallenge, LeaderboardEntry, EffortResult
} from '../../models/heartbreak-hill.model';
import { buildElevationProfile, ElevationProfile, formatGap } from './heartbreak-hill.util';

@Component({
  selector: 'app-heartbreak-hill',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './heartbreak-hill.html',
  styleUrl: './heartbreak-hill.scss'
})
export class HeartbreakHill implements OnInit {
  private readonly service = inject(HeartbreakHillService);
  private readonly router = inject(Router);

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
  submitting = signal(false);
  uploadError = signal<string | null>(null);
  result = signal<EffortResult | null>(null);

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

  ngOnInit(): void {
    this.service.getChallenge().subscribe({
      next: c => {
        this.challenge.set(c);
        this.loading.set(false);
        this.loadLeaderboard();
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
    this.result.set(null);
    this.uploadError.set(null);
    this.loadLeaderboard();
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
    this.service.submitEffort(this.activeTab(), this.displayName().trim(), file).subscribe({
      next: res => {
        this.result.set(res);
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
}
