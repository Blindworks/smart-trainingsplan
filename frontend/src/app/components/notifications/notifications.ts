import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { NotificationService, NotificationDto } from '../../services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss'
})
export class Notifications implements OnInit {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  list = signal<NotificationDto[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.notificationService.list().subscribe({
      next: l => { this.list.set(l); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
    this.notificationService.fetchUnreadCount().subscribe();
  }

  open(n: NotificationDto): void {
    if (!n.read) this.notificationService.markRead(n.id).subscribe();
    if (n.linkPath) this.router.navigateByUrl(n.linkPath);
  }

  markAll(): void {
    this.notificationService.markAllRead().subscribe(() => {
      this.list.update(l => l.map(n => ({ ...n, read: true })));
    });
  }

  iconFor(type: string): string {
    switch (type) {
      case 'BUDDY_INVITE': return 'mail';
      case 'BUDDY_JOIN': return 'group_add';
      case 'BUDDY_WITHDRAW': return 'person_remove';
      case 'BUDDY_CANCELLED': return 'cancel';
      case 'BUDDY_REMINDER_24H':
      case 'BUDDY_REMINDER_1H': return 'alarm';
      default: return 'notifications';
    }
  }
}
