import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { apiUrl, apiBaseUrl } from '../core/api-base';
import {
  CreatePostRequest,
  CreateRunClubRequest,
  MyRunClubEntry,
  RunClub,
  RunClubComment,
  RunClubDetail,
  RunClubFeedPost,
  RunClubMembership,
  RunClubPost,
  RunClubStats,
  UpdateRunClubRequest
} from '../models/run-club.model';
import { GroupEventDto } from './group-event.service';
import { CreateGroupEventRequest, UpdateGroupEventRequest } from './trainer-event.service';

const BASE = apiUrl('/run-clubs');
const ADMIN_BASE = apiUrl('/admin/run-clubs');

@Injectable({ providedIn: 'root' })
export class RunClubService {
  private readonly http = inject(HttpClient);

  // ----- Public discovery -----

  search(city?: string | null, q?: string | null, day?: string | null): Observable<RunClub[]> {
    let params = new HttpParams();
    if (city) params = params.set('city', city);
    if (q) params = params.set('q', q);
    if (day) params = params.set('day', day);
    return this.http.get<RunClub[]>(BASE, { params });
  }

  getNearby(lat: number, lng: number, radiusKm = 25): Observable<RunClub[]> {
    const params = new HttpParams()
      .set('lat', lat)
      .set('lng', lng)
      .set('radiusKm', radiusKm);
    return this.http.get<RunClub[]>(`${BASE}/nearby`, { params });
  }

  getMapData(): Observable<RunClub[]> {
    return this.http.get<RunClub[]>(`${BASE}/map`);
  }

  getById(id: number): Observable<RunClubDetail> {
    return this.http.get<RunClubDetail>(`${BASE}/${id}`);
  }

  getBySlug(slug: string): Observable<RunClubDetail> {
    return this.http.get<RunClubDetail>(`${BASE}/by-slug/${slug}`);
  }

  // ----- CRUD -----

  create(req: CreateRunClubRequest): Observable<RunClub> {
    return this.http.post<RunClub>(BASE, req);
  }

  update(id: number, req: UpdateRunClubRequest): Observable<RunClub> {
    return this.http.put<RunClub>(`${BASE}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  // ----- Images -----

  uploadLogo(id: number, file: File): Observable<void> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<void>(`${BASE}/${id}/logo`, fd);
  }

  uploadCover(id: number, file: File): Observable<void> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<void>(`${BASE}/${id}/cover`, fd);
  }

  getLogoUrl(clubId: number, cacheBust?: string | number): string {
    const cb = cacheBust ? `?v=${encodeURIComponent(String(cacheBust))}` : '';
    return `${apiBaseUrl}/run-clubs/${clubId}/logo${cb}`;
  }

  getCoverUrl(clubId: number, cacheBust?: string | number): string {
    const cb = cacheBust ? `?v=${encodeURIComponent(String(cacheBust))}` : '';
    return `${apiBaseUrl}/run-clubs/${clubId}/cover${cb}`;
  }

  // ----- Stats -----

  getStats(id: number): Observable<RunClubStats> {
    return this.http.get<RunClubStats>(`${BASE}/${id}/stats`);
  }

  // ----- Members -----

  getMembers(id: number, status: 'ACTIVE' | 'PENDING' = 'ACTIVE'): Observable<RunClubMembership[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<RunClubMembership[]>(`${BASE}/${id}/members`, { params });
  }

  join(id: number): Observable<RunClubMembership> {
    return this.http.post<RunClubMembership>(`${BASE}/${id}/join`, {});
  }

  leave(id: number): Observable<void> {
    return this.http.post<void>(`${BASE}/${id}/leave`, {});
  }

  approveMember(membershipId: number): Observable<RunClubMembership> {
    return this.http.post<RunClubMembership>(`${BASE}/memberships/${membershipId}/approve`, {});
  }

  rejectMember(membershipId: number): Observable<void> {
    return this.http.post<void>(`${BASE}/memberships/${membershipId}/reject`, {});
  }

  promoteMember(membershipId: number): Observable<RunClubMembership> {
    return this.http.post<RunClubMembership>(`${BASE}/memberships/${membershipId}/promote`, {});
  }

  demoteMember(membershipId: number): Observable<RunClubMembership> {
    return this.http.post<RunClubMembership>(`${BASE}/memberships/${membershipId}/demote`, {});
  }

  kickMember(membershipId: number): Observable<void> {
    return this.http.post<void>(`${BASE}/memberships/${membershipId}/kick`, {});
  }

  // ----- Posts -----

  getPosts(clubId: number, page = 0): Observable<RunClubPost[]> {
    const params = new HttpParams().set('page', page);
    // Backend returns a Spring Page<RunClubPostDto>; unwrap to its content array.
    return this.http
      .get<RunClubPost[] | { content: RunClubPost[] }>(`${BASE}/${clubId}/posts`, { params })
      .pipe(map(res => Array.isArray(res) ? res : (res?.content ?? [])));
  }

  createPost(clubId: number, req: CreatePostRequest, images: File[] = []): Observable<RunClubPost> {
    const fd = new FormData();
    fd.append('content', req.content ?? '');
    if (req.linkedActivityId != null) fd.append('linkedActivityId', String(req.linkedActivityId));
    if (req.linkedCommunityRouteId != null) fd.append('linkedCommunityRouteId', String(req.linkedCommunityRouteId));
    if (req.linkedGroupEventId != null) fd.append('linkedGroupEventId', String(req.linkedGroupEventId));
    for (const f of images) {
      fd.append('images', f);
    }
    return this.http.post<RunClubPost>(`${BASE}/${clubId}/posts`, fd);
  }

  getPostImageUrl(postId: number, imageId: number): string {
    return `${apiBaseUrl}/run-clubs/posts/${postId}/images/${imageId}`;
  }

  deletePost(postId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/posts/${postId}`);
  }

  likePost(postId: number): Observable<void> {
    return this.http.post<void>(`${BASE}/posts/${postId}/like`, {});
  }

  unlikePost(postId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/posts/${postId}/like`);
  }

  // ----- Comments -----

  getComments(postId: number): Observable<RunClubComment[]> {
    return this.http.get<RunClubComment[]>(`${BASE}/posts/${postId}/comments`);
  }

  createComment(postId: number, content: string): Observable<RunClubComment> {
    return this.http.post<RunClubComment>(`${BASE}/posts/${postId}/comments`, { content });
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/comments/${commentId}`);
  }

  // ----- Linked Events -----

  getEvents(clubId: number): Observable<GroupEventDto[]> {
    return this.http.get<GroupEventDto[]>(`${BASE}/${clubId}/events`);
  }

  getEvent(clubId: number, eventId: number): Observable<GroupEventDto> {
    return this.http.get<GroupEventDto>(`${BASE}/${clubId}/events/${eventId}`);
  }

  createEvent(clubId: number, request: CreateGroupEventRequest): Observable<GroupEventDto> {
    return this.http.post<GroupEventDto>(`${BASE}/${clubId}/events`, request);
  }

  updateEvent(clubId: number, eventId: number, request: UpdateGroupEventRequest): Observable<GroupEventDto> {
    return this.http.put<GroupEventDto>(`${BASE}/${clubId}/events/${eventId}`, request);
  }

  publishEvent(clubId: number, eventId: number): Observable<GroupEventDto> {
    return this.http.put<GroupEventDto>(`${BASE}/${clubId}/events/${eventId}/publish`, {});
  }

  cancelEvent(clubId: number, eventId: number): Observable<void> {
    return this.http.put<void>(`${BASE}/${clubId}/events/${eventId}/cancel`, {});
  }

  deleteEvent(clubId: number, eventId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${clubId}/events/${eventId}`);
  }

  // ----- News Hub aggregated feed -----

  /**
   * Returns posts from every Run Club where the current user holds an ACTIVE
   * membership, newest first. Used by the News Hub to mix club content into the
   * main feed. Backend returns a Spring Page; we unwrap to the content array.
   */
  getNewsHubFeed(page = 0, size = 20): Observable<RunClubFeedPost[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<RunClubFeedPost[] | { content: RunClubFeedPost[] }>(`${BASE}/feed`, { params })
      .pipe(map(res => Array.isArray(res) ? res : (res?.content ?? [])));
  }

  /**
   * Returns upcoming events from every Run Club where the current user holds an
   * ACTIVE membership. Shown in the News Hub sidebar alongside trainer events.
   */
  getNewsHubClubEvents(): Observable<GroupEventDto[]> {
    return this.http.get<GroupEventDto[]>(`${BASE}/feed/upcoming-events`);
  }

  // ----- Me -----

  getMyClubs(): Observable<MyRunClubEntry[]> {
    return this.http.get<MyRunClubEntry[]>(`${BASE}/me/clubs`);
  }

  toggleEnabled(enabled: boolean): Observable<void> {
    return this.http.patch<void>(`${BASE}/me/enabled`, { enabled });
  }

  // ----- Admin -----

  adminList(status?: 'PENDING' | 'APPROVED' | 'REJECTED'): Observable<RunClub[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<RunClub[]>(ADMIN_BASE, { params });
  }

  adminApprove(id: number): Observable<RunClub> {
    return this.http.post<RunClub>(`${ADMIN_BASE}/${id}/approve`, {});
  }

  adminReject(id: number, reason: string): Observable<RunClub> {
    return this.http.post<RunClub>(`${ADMIN_BASE}/${id}/reject`, { reason });
  }

  adminVerify(id: number): Observable<RunClub> {
    return this.http.post<RunClub>(`${ADMIN_BASE}/${id}/verify`, {});
  }

  adminDelete(id: number): Observable<void> {
    return this.http.delete<void>(`${ADMIN_BASE}/${id}`);
  }
}
