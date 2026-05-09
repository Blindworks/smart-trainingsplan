import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  effect,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import * as L from 'leaflet';

import { RunClubService } from '../../services/run-club.service';
import { ThemeService } from '../../services/theme.service';
import { UserService } from '../../services/user.service';
import { apiBaseUrl } from '../../core/api-base';
import {
  RunClubDetail as RunClubDetailDto,
  RunClubMembership,
  RunClubPost,
  RunClubStats
} from '../../models/run-club.model';
import { RunClubPostComposer } from '../run-club-post-composer/run-club-post-composer';
import { RunClubFeedPost } from '../run-club-feed-post/run-club-feed-post';

type Tab = 'overview' | 'feed' | 'members' | 'events';

@Component({
  selector: 'app-run-club-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, RunClubPostComposer, RunClubFeedPost],
  templateUrl: './run-club-detail.html',
  styleUrl: './run-club-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RunClubDetail implements OnInit, AfterViewInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(RunClubService);
  private readonly userService = inject(UserService);
  private readonly themeService = inject(ThemeService);
  readonly translate = inject(TranslateService);

  private memberAvatarObjUrls = new Map<number, string>();
  memberAvatarMap = signal<Record<number, string>>({});

  @ViewChild('miniMap') miniMapRef?: ElementRef<HTMLDivElement>;

  club = signal<RunClubDetailDto | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  actionLoading = signal(false);
  activeTab = signal<Tab>('overview');
  menuOpen = signal(false);

  stats = signal<RunClubStats | null>(null);
  posts = signal<RunClubPost[]>([]);
  postsLoading = signal(false);
  members = signal<RunClubMembership[]>([]);
  pendingMembers = signal<RunClubMembership[]>([]);
  membersLoading = signal(false);
  events = signal<any[]>([]);
  eventsLoading = signal(false);

  private map: L.Map | null = null;
  private tileLayer: L.TileLayer | null = null;
  private currentTheme: 'light' | 'dark' = 'dark';

  constructor() {
    effect(() => {
      const t = this.themeService.resolved();
      if (this.map && t !== this.currentTheme) {
        this.currentTheme = t;
        this.refreshTile();
      }
    });
  }

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.router.navigate(['/run-clubs']);
      return;
    }
    this.load(slug);
  }

  ngAfterViewInit(): void {
    // map init triggered after club loads
  }

  ngOnDestroy(): void {
    this.destroyMap();
    for (const url of this.memberAvatarObjUrls.values()) {
      URL.revokeObjectURL(url);
    }
    this.memberAvatarObjUrls.clear();
  }

  private load(slug: string): void {
    this.loading.set(true);
    this.service.getBySlug(slug).subscribe({
      next: c => {
        this.club.set(c);
        this.loading.set(false);
        this.loadStats(c.id);
        setTimeout(() => this.tryInitMap(), 80);
      },
      error: () => {
        this.error.set(this.translate.instant('RUN_CLUBS.LOAD_ERROR'));
        this.loading.set(false);
      }
    });
  }

  private loadStats(id: number): void {
    this.service.getStats(id).subscribe({
      next: s => this.stats.set(s),
      error: () => {}
    });
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    const c = this.club();
    if (!c) return;
    if (tab === 'feed' && this.posts().length === 0) this.loadPosts();
    if (tab === 'members' && this.members().length === 0) this.loadMembers();
    if (tab === 'events' && this.events().length === 0) this.loadEvents();
    if (tab === 'overview') {
      setTimeout(() => this.tryInitMap(), 80);
    }
  }

  // ----- Actions -----

  isAdmin(): boolean {
    const c = this.club();
    return !!c && (c.canManage === true || c.myRole === 'ADMIN');
  }

  canPost(): boolean {
    const c = this.club();
    return !!c && c.isMember && c.myMembershipStatus === 'ACTIVE';
  }

  joinLabel(): string {
    const c = this.club();
    if (!c) return '';
    if (c.myMembershipStatus === 'ACTIVE') return this.translate.instant('RUN_CLUBS.JOINED');
    if (c.myMembershipStatus === 'PENDING') return this.translate.instant('RUN_CLUBS.REQUEST_PENDING');
    return this.translate.instant('RUN_CLUBS.JOIN');
  }

  primaryAction(): void {
    const c = this.club();
    if (!c || this.actionLoading()) return;

    if (c.myMembershipStatus === 'ACTIVE') {
      if (!confirm(this.translate.instant('RUN_CLUBS.CONFIRM_LEAVE'))) return;
      this.actionLoading.set(true);
      this.service.leave(c.id).subscribe({
        next: () => {
          this.actionLoading.set(false);
          this.club.set({ ...c, isMember: false, myRole: null, myMembershipStatus: 'LEFT', memberCount: Math.max(0, c.memberCount - 1) });
        },
        error: () => this.actionLoading.set(false)
      });
    } else if (c.myMembershipStatus === 'PENDING') {
      // no-op for now (could cancel request)
    } else {
      this.actionLoading.set(true);
      this.service.join(c.id).subscribe({
        next: m => {
          this.actionLoading.set(false);
          const status = m.status;
          this.club.set({
            ...c,
            isMember: status === 'ACTIVE',
            myRole: m.role,
            myMembershipStatus: status,
            memberCount: status === 'ACTIVE' ? c.memberCount + 1 : c.memberCount
          });
        },
        error: () => this.actionLoading.set(false)
      });
    }
  }

  toggleMenu(): void { this.menuOpen.update(v => !v); }
  closeMenu(): void { this.menuOpen.set(false); }

  goEdit(): void {
    const c = this.club();
    if (c) this.router.navigate(['/run-clubs', c.slug, 'edit']);
  }

  deleteClub(): void {
    const c = this.club();
    if (!c) return;
    if (!confirm(this.translate.instant('RUN_CLUBS.CONFIRM_DELETE'))) return;
    this.service.delete(c.id).subscribe({
      next: () => this.router.navigate(['/run-clubs'])
    });
  }

  // ----- Feed -----

  loadPosts(): void {
    const c = this.club();
    if (!c) return;
    this.postsLoading.set(true);
    this.service.getPosts(c.id).subscribe({
      next: ps => {
        this.posts.set(ps);
        this.postsLoading.set(false);
      },
      error: () => this.postsLoading.set(false)
    });
  }

  onPostCreated(post: RunClubPost): void {
    this.posts.set([post, ...this.posts()]);
  }

  onPostDeleted(postId: number): void {
    this.posts.set(this.posts().filter(p => p.id !== postId));
  }

  // ----- Members -----

  loadMembers(): void {
    const c = this.club();
    if (!c) return;
    this.membersLoading.set(true);
    this.service.getMembers(c.id, 'ACTIVE').subscribe({
      next: ms => {
        this.members.set(ms);
        this.membersLoading.set(false);
        this.fetchMemberAvatars(ms);
      },
      error: () => this.membersLoading.set(false)
    });
    if (this.isAdmin()) {
      this.service.getMembers(c.id, 'PENDING').subscribe({
        next: ms => {
          this.pendingMembers.set(ms);
          this.fetchMemberAvatars(ms);
        }
      });
    }
  }

  private fetchMemberAvatars(members: RunClubMembership[]): void {
    for (const m of members) {
      if (!m.hasProfileImage) continue;
      if (this.memberAvatarObjUrls.has(m.userId)) continue;
      this.userService.getProfileImage(m.userId).subscribe(blob => {
        if (!blob) return;
        const url = URL.createObjectURL(blob);
        this.memberAvatarObjUrls.set(m.userId, url);
        this.memberAvatarMap.set({ ...this.memberAvatarMap(), [m.userId]: url });
      });
    }
  }

  approveMember(m: RunClubMembership): void {
    this.service.approveMember(m.id).subscribe({
      next: () => {
        this.pendingMembers.set(this.pendingMembers().filter(x => x.id !== m.id));
        this.loadMembers();
      }
    });
  }

  rejectMember(m: RunClubMembership): void {
    if (!confirm('Reject this request?')) return;
    this.service.rejectMember(m.id).subscribe({
      next: () => this.pendingMembers.set(this.pendingMembers().filter(x => x.id !== m.id))
    });
  }

  promoteMember(m: RunClubMembership): void {
    this.service.promoteMember(m.id).subscribe({
      next: updated => {
        this.members.set(this.members().map(x => x.id === m.id ? updated : x));
      }
    });
  }

  demoteMember(m: RunClubMembership): void {
    this.service.demoteMember(m.id).subscribe({
      next: updated => {
        this.members.set(this.members().map(x => x.id === m.id ? updated : x));
      }
    });
  }

  kickMember(m: RunClubMembership): void {
    if (!confirm('Remove this member?')) return;
    this.service.kickMember(m.id).subscribe({
      next: () => this.members.set(this.members().filter(x => x.id !== m.id))
    });
  }

  // ----- Events -----

  loadEvents(): void {
    const c = this.club();
    if (!c) return;
    this.eventsLoading.set(true);
    this.service.getEvents(c.id).subscribe({
      next: es => {
        this.events.set(es);
        this.eventsLoading.set(false);
      },
      error: () => this.eventsLoading.set(false)
    });
  }

  // ----- Map -----

  private tryInitMap(): void {
    const c = this.club();
    if (!c || c.latitude == null || c.longitude == null) return;
    if (!this.miniMapRef) return;
    if (this.map) {
      this.map.invalidateSize();
      return;
    }
    this.currentTheme = this.themeService.resolved();
    this.map = L.map(this.miniMapRef.nativeElement, {
      center: [c.latitude, c.longitude],
      zoom: 14,
      zoomControl: false,
      scrollWheelZoom: false
    });
    this.tileLayer = this.createTile();
    this.tileLayer.addTo(this.map);

    L.marker([c.latitude, c.longitude], {
      icon: L.divIcon({
        className: '',
        html: `<div class="rc-pin"><span class="material-symbols-outlined">place</span></div>`,
        iconSize: [28, 36],
        iconAnchor: [14, 36]
      })
    }).addTo(this.map);

    setTimeout(() => this.map?.invalidateSize(), 100);
  }

  private createTile(): L.TileLayer {
    const isDark = this.currentTheme === 'dark';
    const url = isDark
      ? 'https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}{r}.png';
    return L.tileLayer(url, {
      attribution: '&copy; OpenStreetMap &copy; CARTO',
      subdomains: 'abcd',
      maxZoom: 19
    });
  }

  private refreshTile(): void {
    if (!this.map || !this.tileLayer) return;
    this.map.removeLayer(this.tileLayer);
    this.tileLayer = this.createTile();
    this.tileLayer.addTo(this.map);
  }

  private destroyMap(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
    this.tileLayer = null;
  }

  // ----- Helpers -----

  logoUrl(): string | null {
    const c = this.club();
    return c && c.hasLogo ? this.service.getLogoUrl(c.id, c.id) : null;
  }

  coverUrl(): string | null {
    const c = this.club();
    return c && c.hasCover ? this.service.getCoverUrl(c.id, c.id) : null;
  }

  initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]?.toUpperCase() ?? '').join('');
  }

  meetingDaysList(): string {
    const c = this.club();
    if (!c?.meetingDays?.length) return '';
    return c.meetingDays.map(d => this.translate.instant('RUN_CLUBS.DAY_FULL_' + d)).join(', ');
  }

  formatTime(time: string | null | undefined): string {
    if (!time) return '';
    // Backend returns LocalTime as "HH:mm" or "HH:mm:ss"
    const parts = time.split(':');
    if (parts.length < 2) return time;
    return `${parts[0]}:${parts[1]}`;
  }

  memberDisplayName(m: RunClubMembership): string {
    const first = (m.userFirstName ?? '').trim();
    const last = (m.userLastName ?? '').trim();
    const full = `${first} ${last}`.trim();
    return full || m.username || '—';
  }

  memberInitial(m: RunClubMembership): string {
    const name = this.memberDisplayName(m);
    return name.charAt(0).toUpperCase() || '?';
  }

  memberAvatarUrl(m: RunClubMembership): string | null {
    if (!m.hasProfileImage) return null;
    return this.memberAvatarMap()[m.userId] ?? null;
  }
}
