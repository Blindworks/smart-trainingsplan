import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-base';

const BASE = apiUrl('/buddy-runs');
const PREFS = apiUrl('/users/me/buddy-preferences');

export type BuddyVisibility = 'FRIENDS_ONLY' | 'PUBLIC_NEARBY' | 'PRIVATE_INVITE';
export type BuddyStatus = 'OPEN' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
export type BuddyParticipantStatus = 'INVITED' | 'JOINED' | 'DECLINED' | 'WITHDRAWN';
export type BuddyParticipantRole = 'CREATOR' | 'PARTICIPANT';

export interface BuddyRunParticipantDto {
  id: number;
  userId: number;
  username: string;
  profileImageFilename: string | null;
  status: BuddyParticipantStatus;
  role: BuddyParticipantRole;
  invitedAt: string;
  respondedAt: string | null;
}

export interface BuddyRunDto {
  id: number;
  creatorId: number;
  creatorUsername: string;
  creatorProfileImage: string | null;
  title: string;
  description: string | null;
  scheduledAt: string;
  meetingPointName: string;
  meetingLatitude: number | null;
  meetingLongitude: number | null;
  communityRouteId: number | null;
  distanceKm: number | null;
  expectedDurationMinutes: number | null;
  targetPaceMinSecPerKm: number | null;
  targetPaceMaxSecPerKm: number | null;
  maxParticipants: number | null;
  visibility: BuddyVisibility;
  status: BuddyStatus;
  createdAt: string;
  participants: BuddyRunParticipantDto[];
  joinedCount: number;
}

export interface BuddyRunCreateRequest {
  title: string;
  description?: string | null;
  scheduledAt: string;
  meetingPointName: string;
  meetingLatitude?: number | null;
  meetingLongitude?: number | null;
  communityRouteId?: number | null;
  distanceKm?: number | null;
  expectedDurationMinutes?: number | null;
  targetPaceMinSecPerKm?: number | null;
  targetPaceMaxSecPerKm?: number | null;
  maxParticipants?: number | null;
  visibility: BuddyVisibility;
}

export interface BuddySuggestionDto {
  userId: number;
  username: string;
  profileImageFilename: string | null;
  city: string | null;
  distanceKm: number | null;
  userPaceSecPerKm: number | null;
  paceMatchScore: number | null;
  friendshipStatus: string | null;
}

export interface BuddyMatchesResponse {
  optedIn: boolean;
  matches: BuddySuggestionDto[];
}

export interface UserBuddyPreferencesDto {
  buddyDiscoverable: boolean;
  searchRadiusKm: number;
  paceTolerancePercent: number;
  availableWeekdays: string | null;
  availableTimeRanges: string | null;
  autoMatchEnabled: boolean;
  /** Optional buddy-specific pace in sec/km; falls back to the user's global pace when null. */
  targetPaceSecPerKm: number | null;
}

@Injectable({ providedIn: 'root' })
export class BuddyRunService {
  private readonly http = inject(HttpClient);

  create(req: BuddyRunCreateRequest): Observable<BuddyRunDto> {
    return this.http.post<BuddyRunDto>(BASE, req);
  }

  open(lat?: number, lon?: number, radiusKm?: number): Observable<BuddyRunDto[]> {
    let p = new HttpParams();
    if (lat != null) p = p.set('lat', String(lat));
    if (lon != null) p = p.set('lon', String(lon));
    if (radiusKm != null) p = p.set('radiusKm', String(radiusKm));
    return this.http.get<BuddyRunDto[]>(`${BASE}/open`, { params: p });
  }

  upcoming(): Observable<BuddyRunDto[]> {
    return this.http.get<BuddyRunDto[]>(`${BASE}/upcoming`);
  }

  mine(): Observable<BuddyRunDto[]> {
    return this.http.get<BuddyRunDto[]>(`${BASE}/mine`);
  }

  get(id: number): Observable<BuddyRunDto> {
    return this.http.get<BuddyRunDto>(`${BASE}/${id}`);
  }

  join(id: number): Observable<BuddyRunParticipantDto> {
    return this.http.post<BuddyRunParticipantDto>(`${BASE}/${id}/join`, {});
  }

  withdraw(id: number): Observable<void> {
    return this.http.post<void>(`${BASE}/${id}/withdraw`, {});
  }

  invite(id: number, userId: number): Observable<BuddyRunParticipantDto> {
    return this.http.post<BuddyRunParticipantDto>(`${BASE}/${id}/invite`, { userId });
  }

  respond(id: number, accept: boolean): Observable<BuddyRunParticipantDto> {
    return this.http.post<BuddyRunParticipantDto>(`${BASE}/${id}/respond`, { accept });
  }

  cancel(id: number): Observable<void> {
    return this.http.post<void>(`${BASE}/${id}/cancel`, {});
  }

  suggestions(id: number): Observable<BuddySuggestionDto[]> {
    return this.http.get<BuddySuggestionDto[]>(`${BASE}/${id}/suggested-buddies`);
  }

  matches(): Observable<BuddyMatchesResponse> {
    return this.http.get<BuddyMatchesResponse>(`${BASE}/matches`);
  }

  getPreferences(): Observable<UserBuddyPreferencesDto> {
    return this.http.get<UserBuddyPreferencesDto>(PREFS);
  }

  updatePreferences(prefs: UserBuddyPreferencesDto): Observable<UserBuddyPreferencesDto> {
    return this.http.put<UserBuddyPreferencesDto>(PREFS, prefs);
  }
}
