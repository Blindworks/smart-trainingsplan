import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { BuddyRunService, BuddyRunDto, BuddySuggestionDto } from '../../services/buddy-run.service';
import { UserService } from '../../services/user.service';

type Tab = 'matches' | 'open' | 'upcoming' | 'mine';
type DistanceFilter = 'ANY' | 'SHORT' | 'MID' | 'LONG' | 'MARATHON';
type PaceFilter = 'ANY' | 'FAST' | 'MID' | 'EASY';
type TimeFilter = 'ANY' | 'EARLY' | 'MORNING' | 'EVENING';

@Component({
  selector: 'app-buddy',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule],
  templateUrl: './buddy.html',
  styleUrl: './buddy.scss'
})
export class Buddy implements OnInit, OnDestroy {
  private readonly buddyService = inject(BuddyRunService);
  private readonly userService = inject(UserService);

  avatarUrls = signal<Record<number, string>>({});
  private readonly avatarLoading = new Set<number>();

  activeTab = signal<Tab>('matches');
  loading = signal(true);
  error = signal<string | null>(null);
  runs = signal<BuddyRunDto[]>([]);
  matches = signal<BuddySuggestionDto[]>([]);
  optedIn = signal<boolean>(true);

  userLat = signal<number | null>(null);
  userLon = signal<number | null>(null);

  // Frontend-only filters (display state — can be wired to backend later)
  filterDistance = signal<DistanceFilter>('ANY');
  filterPace = signal<PaceFilter>('ANY');
  filterTime = signal<TimeFilter>('ANY');

  filteredMatches = computed(() => {
    const list = this.matches();
    const pace = this.filterPace();
    return list.filter(m => {
      if (pace !== 'ANY') {
        const p = m.userPaceSecPerKm;
        if (!p) return false;
        if (pace === 'FAST' && p > 270) return false; // < 4:30
        if (pace === 'MID' && (p < 270 || p > 330)) return false; // 4:30 - 5:30
        if (pace === 'EASY' && p < 330) return false; // > 5:30
      }
      return true;
    });
  });

  ngOnInit(): void {
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        pos => {
          this.userLat.set(pos.coords.latitude);
          this.userLon.set(pos.coords.longitude);
          this.load();
        },
        () => this.load()
      );
    } else {
      this.load();
    }
  }

  selectTab(t: Tab): void {
    this.activeTab.set(t);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const tab = this.activeTab();

    if (tab === 'matches') {
      this.buddyService.matches().subscribe({
        next: r => {
          this.optedIn.set(r.optedIn);
          this.matches.set(r.matches);
          this.loading.set(false);
          r.matches.forEach(m => {
            if (m.profileImageFilename) this.loadAvatar(m.userId);
          });
        },
        error: () => { this.error.set('error'); this.loading.set(false); }
      });
      return;
    }

    const obs = tab === 'open'
      ? this.buddyService.open(this.userLat() ?? undefined, this.userLon() ?? undefined, 25)
      : tab === 'upcoming'
        ? this.buddyService.upcoming()
        : this.buddyService.mine();

    obs.subscribe({
      next: (r) => { this.runs.set(r); this.loading.set(false); },
      error: () => { this.error.set('error'); this.loading.set(false); }
    });
  }

  formatPace(secPerKm: number | null): string {
    if (secPerKm == null) return '–';
    const m = Math.floor(secPerKm / 60);
    const s = secPerKm % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  formatDate(s: string): string {
    try { return new Date(s).toLocaleString('de-DE', { dateStyle: 'short', timeStyle: 'short' }); }
    catch { return s; }
  }

  visibilityIcon(v: string): string {
    switch (v) {
      case 'FRIENDS_ONLY': return 'group';
      case 'PUBLIC_NEARBY': return 'public';
      case 'PRIVATE_INVITE': return 'lock';
      default: return 'directions_run';
    }
  }

  avatarUrl(userId: number): string | null {
    return this.avatarUrls()[userId] ?? null;
  }

  private loadAvatar(userId: number): void {
    if (this.avatarUrls()[userId] || this.avatarLoading.has(userId)) return;
    this.avatarLoading.add(userId);
    this.userService.getProfileImage(userId).subscribe({
      next: blob => {
        if (blob) {
          const url = URL.createObjectURL(blob);
          this.avatarUrls.update(map => ({ ...map, [userId]: url }));
        }
        this.avatarLoading.delete(userId);
      },
      error: () => { this.avatarLoading.delete(userId); }
    });
  }

  ngOnDestroy(): void {
    Object.values(this.avatarUrls()).forEach(url => URL.revokeObjectURL(url));
  }

  tierForScore(score: number | null): string {
    if (score == null) return 'MATCH';
    if (score >= 0.9) return 'ELITE TIER';
    if (score >= 0.75) return 'PRO TIER';
    if (score >= 0.5) return 'ADVANCED';
    return 'CASUAL';
  }
}
