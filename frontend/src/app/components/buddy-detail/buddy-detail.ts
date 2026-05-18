import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { BuddyRunService, BuddyRunDto, BuddySuggestionDto } from '../../services/buddy-run.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-buddy-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './buddy-detail.html',
  styleUrl: './buddy-detail.scss'
})
export class BuddyDetail implements OnInit, OnDestroy {
  private readonly buddyService = inject(BuddyRunService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  run = signal<BuddyRunDto | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  suggestions = signal<BuddySuggestionDto[]>([]);
  busy = signal(false);

  avatarUrls = signal<Record<number, string>>({});
  private readonly avatarLoading = new Set<number>();

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

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) { this.error.set('not-found'); this.loading.set(false); return; }
    this.load(id);
  }

  private load(id: number): void {
    this.buddyService.get(id).subscribe({
      next: r => {
        this.run.set(r);
        this.loading.set(false);
        r.participants.forEach(p => { if (p.profileImageFilename) this.loadAvatar(p.userId); });
        if (this.isCreator()) this.loadSuggestions(id);
      },
      error: () => { this.error.set('error'); this.loading.set(false); }
    });
  }

  private loadSuggestions(id: number): void {
    this.buddyService.suggestions(id).subscribe({
      next: s => {
        this.suggestions.set(s);
        s.forEach(x => { if (x.profileImageFilename) this.loadAvatar(x.userId); });
      },
      error: () => { /* no-op */ }
    });
  }

  isCreator(): boolean {
    const me = this.userService.currentUser();
    const r = this.run();
    return !!(me && r && r.creatorId === me.id);
  }

  myParticipation() {
    const me = this.userService.currentUser();
    const r = this.run();
    if (!me || !r) return null;
    return r.participants.find(p => p.userId === me.id) ?? null;
  }

  join(): void {
    const r = this.run(); if (!r) return;
    this.busy.set(true);
    this.buddyService.join(r.id).subscribe({
      next: () => { this.busy.set(false); this.load(r.id); },
      error: () => this.busy.set(false)
    });
  }

  withdraw(): void {
    const r = this.run(); if (!r) return;
    this.busy.set(true);
    this.buddyService.withdraw(r.id).subscribe({
      next: () => { this.busy.set(false); this.load(r.id); },
      error: () => this.busy.set(false)
    });
  }

  respond(accept: boolean): void {
    const r = this.run(); if (!r) return;
    this.busy.set(true);
    this.buddyService.respond(r.id, accept).subscribe({
      next: () => { this.busy.set(false); this.load(r.id); },
      error: () => this.busy.set(false)
    });
  }

  invite(userId: number): void {
    const r = this.run(); if (!r) return;
    this.busy.set(true);
    this.buddyService.invite(r.id, userId).subscribe({
      next: () => { this.busy.set(false); this.load(r.id); this.loadSuggestions(r.id); },
      error: () => this.busy.set(false)
    });
  }

  cancel(): void {
    const r = this.run(); if (!r) return;
    if (!confirm('Diesen Buddy-Run wirklich absagen?')) return;
    this.busy.set(true);
    this.buddyService.cancel(r.id).subscribe({
      next: () => this.router.navigate(['/buddy']),
      error: () => this.busy.set(false)
    });
  }

  formatPace(secPerKm: number | null): string {
    if (secPerKm == null) return '–';
    const m = Math.floor(secPerKm / 60);
    const s = secPerKm % 60;
    return `${m}:${s.toString().padStart(2, '0')}/km`;
  }

  formatDate(s: string): string {
    try { return new Date(s).toLocaleString('de-DE', { dateStyle: 'full', timeStyle: 'short' }); }
    catch { return s; }
  }
}
