import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { RunClubService } from '../../../services/run-club.service';
import { RunClub, RunClubStatus } from '../../../models/run-club.model';

@Component({
  selector: 'app-admin-run-clubs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslateModule, DatePipe],
  templateUrl: './admin-run-clubs.html',
  styleUrl: './admin-run-clubs.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminRunClubs implements OnInit {
  private readonly service = inject(RunClubService);

  clubs = signal<RunClub[]>([]);
  loading = signal(false);
  activeStatus = signal<RunClubStatus>('PENDING');

  rejectDialogClubId = signal<number | null>(null);
  rejectReason = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.adminList(this.activeStatus()).subscribe({
      next: data => {
        this.clubs.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  setStatus(status: RunClubStatus): void {
    this.activeStatus.set(status);
    this.load();
  }

  approve(c: RunClub): void {
    this.service.adminApprove(c.id).subscribe({ next: () => this.load() });
  }

  openReject(c: RunClub): void {
    this.rejectDialogClubId.set(c.id);
    this.rejectReason.set('');
  }

  cancelReject(): void {
    this.rejectDialogClubId.set(null);
    this.rejectReason.set('');
  }

  confirmReject(): void {
    const id = this.rejectDialogClubId();
    if (id == null) return;
    const reason = this.rejectReason().trim() || 'No reason provided';
    this.service.adminReject(id, reason).subscribe({
      next: () => {
        this.cancelReject();
        this.load();
      }
    });
  }

  toggleVerify(c: RunClub): void {
    this.service.adminVerify(c.id).subscribe({
      next: updated => {
        this.clubs.set(this.clubs().map(x => x.id === c.id ? updated : x));
      }
    });
  }

  delete(c: RunClub): void {
    if (!confirm(`Delete club "${c.name}"?`)) return;
    this.service.adminDelete(c.id).subscribe({ next: () => this.load() });
  }

  trackById = (_: number, c: RunClub) => c.id;
}
