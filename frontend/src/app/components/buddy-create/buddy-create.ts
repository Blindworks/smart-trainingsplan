import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { BuddyRunService, BuddyRunCreateRequest, BuddyVisibility } from '../../services/buddy-run.service';
import { LocationPickerDialogComponent } from '../location-picker-dialog/location-picker-dialog';

@Component({
  selector: 'app-buddy-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule, LocationPickerDialogComponent],
  templateUrl: './buddy-create.html',
  styleUrl: './buddy-create.scss'
})
export class BuddyCreate implements OnInit {
  private readonly buddyService = inject(BuddyRunService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);

  @ViewChild('locationPicker') locationPicker!: LocationPickerDialogComponent;

  inviteUserId: number | null = null;
  inviteName: string | null = null;

  title = '';
  description = '';
  scheduledDate = '';
  scheduledTime = '';
  meetingPointName = '';
  meetingLatitude: number | null = null;
  meetingLongitude: number | null = null;
  distanceKm: number | null = null;
  expectedDurationMinutes: number | null = null;
  targetPaceMin = '';
  targetPaceMax = '';
  maxParticipants: number | null = null;
  visibility: BuddyVisibility = 'FRIENDS_ONLY';

  submitting = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const id = params.get('inviteUserId');
    if (id) {
      const parsed = Number(id);
      if (!isNaN(parsed)) {
        this.inviteUserId = parsed;
        this.inviteName = params.get('inviteName');
        this.visibility = 'PRIVATE_INVITE';
      }
    }
  }

  parsePaceToSec(p: string): number | null {
    if (!p) return null;
    const m = p.match(/^(\d+):(\d{1,2})$/);
    if (!m) return null;
    return parseInt(m[1], 10) * 60 + parseInt(m[2], 10);
  }

  useCurrentLocation(): void {
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(pos => {
        this.meetingLatitude = pos.coords.latitude;
        this.meetingLongitude = pos.coords.longitude;
        this.reverseGeocodeIfEmpty();
      });
    }
  }

  openLocationPicker(): void {
    this.locationPicker.open(this.meetingLatitude, this.meetingLongitude);
  }

  onLocationPicked(coords: { lat: number; lng: number }): void {
    this.meetingLatitude = coords.lat;
    this.meetingLongitude = coords.lng;
    this.reverseGeocodeIfEmpty();
  }

  private reverseGeocodeIfEmpty(): void {
    if (this.meetingPointName && this.meetingPointName.trim().length > 0) return;
    if (this.meetingLatitude == null || this.meetingLongitude == null) return;
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${this.meetingLatitude}&lon=${this.meetingLongitude}&zoom=16&addressdetails=1`;
    this.http.get<any>(url).subscribe({
      next: r => {
        if (this.meetingPointName && this.meetingPointName.trim().length > 0) return;
        const a = r?.address ?? {};
        const place = a.attraction || a.park || a.leisure || a.tourism || a.amenity
          || a.road || a.pedestrian || a.path || a.cycleway
          || a.neighbourhood || a.suburb;
        const cityLike = a.city || a.town || a.village || a.municipality;
        const parts = [place, cityLike].filter(Boolean);
        if (parts.length > 0) {
          this.meetingPointName = parts.join(', ');
        } else if (r?.display_name) {
          this.meetingPointName = String(r.display_name).split(',').slice(0, 2).join(',').trim();
        }
      },
      error: () => { /* silent */ }
    });
  }

  submit(): void {
    this.error.set(null);
    if (!this.title || !this.scheduledDate || !this.scheduledTime || !this.meetingPointName) {
      this.error.set('VALIDATION');
      return;
    }
    const isoLocal = `${this.scheduledDate}T${this.scheduledTime}:00`;
    const req: BuddyRunCreateRequest = {
      title: this.title,
      description: this.description || null,
      scheduledAt: isoLocal,
      meetingPointName: this.meetingPointName,
      meetingLatitude: this.meetingLatitude,
      meetingLongitude: this.meetingLongitude,
      distanceKm: this.distanceKm,
      expectedDurationMinutes: this.expectedDurationMinutes,
      targetPaceMinSecPerKm: this.parsePaceToSec(this.targetPaceMin),
      targetPaceMaxSecPerKm: this.parsePaceToSec(this.targetPaceMax),
      maxParticipants: this.maxParticipants,
      visibility: this.visibility
    };

    this.submitting.set(true);
    this.buddyService.create(req).subscribe({
      next: br => {
        if (this.inviteUserId) {
          this.buddyService.invite(br.id, this.inviteUserId).subscribe({
            next: () => this.router.navigate(['/buddy', br.id]),
            error: () => this.router.navigate(['/buddy', br.id])
          });
        } else {
          this.router.navigate(['/buddy', br.id]);
        }
      },
      error: err => {
        this.submitting.set(false);
        this.error.set(typeof err?.error === 'string' ? err.error : 'ERROR');
      }
    });
  }
}
