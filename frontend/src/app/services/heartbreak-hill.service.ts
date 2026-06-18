import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';
import {
  ActivityType, SegmentChallenge, LeaderboardEntry, EffortResult, EffortTrack, Gender, LeaderboardScope
} from '../models/heartbreak-hill.model';

const SLUG = 'heartbreak-hill-2026';
const BASE = apiUrl(`/public/challenges/${SLUG}`);

@Injectable({ providedIn: 'root' })
export class HeartbreakHillService {
  private readonly http = inject(HttpClient);

  getChallenge(): Observable<SegmentChallenge> {
    return this.http.get<SegmentChallenge>(BASE);
  }

  getLeaderboard(type: ActivityType, scope: LeaderboardScope = 'OVERALL',
                 ageGroup?: string | null): Observable<LeaderboardEntry[]> {
    let params = new HttpParams().set('type', type).set('scope', scope);
    if (ageGroup) {
      params = params.set('ageGroup', ageGroup);
    }
    return this.http.get<LeaderboardEntry[]>(`${BASE}/leaderboard`, { params });
  }

  submitEffort(type: ActivityType, displayName: string, file: File,
               gender?: Gender | null, birthYear?: number | null): Observable<EffortResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('displayName', displayName);
    form.append('type', type);
    if (gender) {
      form.append('gender', gender);
    }
    if (birthYear != null) {
      form.append('birthYear', String(birthYear));
    }
    return this.http.post<EffortResult>(`${BASE}/efforts`, form);
  }

  getTrack(effortId: number): Observable<EffortTrack> {
    return this.http.get<EffortTrack>(`${BASE}/efforts/${effortId}/track`);
  }
}
