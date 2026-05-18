import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval, startWith, switchMap, tap } from 'rxjs';
import { apiUrl } from '../core/api-base';

const BASE = apiUrl('/notifications');

export interface NotificationDto {
  id: number;
  type: string;
  title: string;
  message: string | null;
  linkPath: string | null;
  referenceId: number | null;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);

  readonly unreadCount = signal<number>(0);

  list(): Observable<NotificationDto[]> {
    return this.http.get<NotificationDto[]>(BASE);
  }

  fetchUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${BASE}/unread-count`).pipe(
      tap(r => this.unreadCount.set(r.count))
    );
  }

  startPolling(intervalMs = 60000): Observable<{ count: number }> {
    return interval(intervalMs).pipe(
      startWith(0),
      switchMap(() => this.fetchUnreadCount())
    );
  }

  markRead(id: number): Observable<void> {
    return this.http.post<void>(`${BASE}/${id}/read`, {}).pipe(
      tap(() => {
        const c = this.unreadCount();
        if (c > 0) this.unreadCount.set(c - 1);
      })
    );
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>(`${BASE}/read-all`, {}).pipe(
      tap(() => this.unreadCount.set(0))
    );
  }
}
