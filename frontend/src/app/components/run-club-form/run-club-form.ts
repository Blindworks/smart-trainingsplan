import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { RunClubService } from '../../services/run-club.service';
import { GeocodingService } from '../../services/geocoding.service';
import { LocationPickerDialogComponent } from '../location-picker-dialog/location-picker-dialog';
import {
  ALL_DAYS_OF_WEEK,
  CreateRunClubRequest,
  DayOfWeek,
  RunClubDetail,
  RunClubJoinPolicy
} from '../../models/run-club.model';

@Component({
  selector: 'app-run-club-form',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule, TranslateModule, LocationPickerDialogComponent],
  templateUrl: './run-club-form.html',
  styleUrl: './run-club-form.scss'
})
export class RunClubForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(RunClubService);
  private readonly geocoding = inject(GeocodingService);
  readonly translate = inject(TranslateService);

  @ViewChild('locationPicker') locationPicker!: LocationPickerDialogComponent;

  form!: FormGroup;
  isEditMode = signal(false);
  clubId = signal<number | null>(null);
  clubSlug = signal<string | null>(null);
  loading = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);
  geocoding$ = signal(false);
  loaded = signal<RunClubDetail | null>(null);

  readonly allDays: DayOfWeek[] = ALL_DAYS_OF_WEEK;

  // Image upload
  selectedLogo = signal<File | null>(null);
  selectedCover = signal<File | null>(null);
  logoPreview = signal<string | null>(null);
  coverPreview = signal<string | null>(null);
  private logoObjUrl: string | null = null;
  private coverObjUrl: string | null = null;

  ngOnInit(): void {
    this.initForm();
    const slug = this.route.snapshot.paramMap.get('slug');
    if (slug && this.router.url.endsWith('/edit')) {
      this.isEditMode.set(true);
      this.clubSlug.set(slug);
      this.load(slug);
    }
  }

  private initForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(120)]],
      description: [''],
      locationCity: [''],
      locationCountry: [''],
      latitude: [null],
      longitude: [null],
      meetingPointName: [''],
      meetingSchedule: [''],
      meetingTime: [''],
      meetingDays: [[] as DayOfWeek[]],
      joinPolicy: ['OPEN' as RunClubJoinPolicy],
      socialInstagram: [''],
      socialWebsite: [''],
      socialStrava: ['']
    });
  }

  private load(slug: string): void {
    this.loading.set(true);
    this.service.getBySlug(slug).subscribe({
      next: c => {
        this.clubId.set(c.id);
        this.loaded.set(c);
        this.form.patchValue({
          name: c.name,
          description: c.description ?? '',
          locationCity: c.locationCity ?? '',
          locationCountry: c.locationCountry ?? '',
          latitude: c.latitude,
          longitude: c.longitude,
          meetingPointName: c.meetingPointName ?? '',
          meetingSchedule: c.meetingSchedule ?? '',
          meetingTime: c.meetingTime ?? '',
          meetingDays: c.meetingDays ?? [],
          joinPolicy: c.joinPolicy,
          socialInstagram: c.socialInstagram ?? '',
          socialWebsite: c.socialWebsite ?? '',
          socialStrava: c.socialStrava ?? ''
        });
        if (c.hasLogo) this.logoPreview.set(this.service.getLogoUrl(c.id, c.id));
        if (c.hasCover) this.coverPreview.set(this.service.getCoverUrl(c.id, c.id));
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('RUN_CLUBS.LOAD_ERROR'));
        this.loading.set(false);
      }
    });
  }

  toggleDay(day: DayOfWeek): void {
    const cur: DayOfWeek[] = this.form.get('meetingDays')?.value || [];
    const idx = cur.indexOf(day);
    if (idx >= 0) cur.splice(idx, 1);
    else cur.push(day);
    this.form.get('meetingDays')?.setValue([...cur]);
  }

  isDaySelected(day: DayOfWeek): boolean {
    const cur: DayOfWeek[] = this.form.get('meetingDays')?.value || [];
    return cur.includes(day);
  }

  geocodeCity(): void {
    const city = this.form.get('locationCity')?.value;
    if (!city || city.trim().length < 2) return;
    this.geocoding$.set(true);
    this.geocoding.geocode(city).subscribe({
      next: (results: any[]) => {
        this.geocoding$.set(false);
        if (results.length > 0) {
          const r = results[0];
          this.form.patchValue({
            latitude: Math.round(parseFloat(r.lat) * 1000000) / 1000000,
            longitude: Math.round(parseFloat(r.lon) * 1000000) / 1000000
          });
        }
      },
      error: () => this.geocoding$.set(false)
    });
  }

  openLocationPicker(): void {
    const lat = this.form.get('latitude')?.value;
    const lng = this.form.get('longitude')?.value;
    this.locationPicker.open(lat, lng);
  }

  onLocationPicked(coords: { lat: number; lng: number }): void {
    this.form.patchValue({ latitude: coords.lat, longitude: coords.lng });
  }

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.selectedLogo.set(file);
    if (this.logoObjUrl) URL.revokeObjectURL(this.logoObjUrl);
    this.logoObjUrl = URL.createObjectURL(file);
    this.logoPreview.set(this.logoObjUrl);
  }

  onCoverSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.selectedCover.set(file);
    if (this.coverObjUrl) URL.revokeObjectURL(this.coverObjUrl);
    this.coverObjUrl = URL.createObjectURL(file);
    this.coverPreview.set(this.coverObjUrl);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const v = this.form.value;
    const req: CreateRunClubRequest = {
      name: v.name,
      description: v.description || null,
      locationCity: v.locationCity || null,
      locationCountry: v.locationCountry || null,
      latitude: v.latitude ?? null,
      longitude: v.longitude ?? null,
      meetingPointName: v.meetingPointName || null,
      meetingSchedule: v.meetingSchedule || null,
      meetingTime: v.meetingTime || null,
      meetingDays: v.meetingDays ?? [],
      joinPolicy: v.joinPolicy,
      socialInstagram: v.socialInstagram || null,
      socialWebsite: v.socialWebsite || null,
      socialStrava: v.socialStrava || null
    };

    if (this.isEditMode() && this.clubId()) {
      this.service.update(this.clubId()!, req).subscribe({
        next: c => this.afterSave(c.id, c.slug),
        error: err => this.handleError(err)
      });
    } else {
      this.service.create(req).subscribe({
        next: c => this.afterSave(c.id, c.slug),
        error: err => this.handleError(err)
      });
    }
  }

  private afterSave(id: number, slug: string): void {
    const logoFile = this.selectedLogo();
    const coverFile = this.selectedCover();

    const finish = () => {
      this.saving.set(false);
      this.router.navigate(['/run-clubs', slug]);
    };

    const uploadCover = () => {
      if (!coverFile) return finish();
      this.service.uploadCover(id, coverFile).subscribe({ next: finish, error: finish });
    };

    if (logoFile) {
      this.service.uploadLogo(id, logoFile).subscribe({
        next: uploadCover,
        error: uploadCover
      });
    } else {
      uploadCover();
    }
  }

  private handleError(err: any): void {
    this.saving.set(false);
    const msg = typeof err?.error === 'string' ? err.error : (err?.error?.message || this.translate.instant('RUN_CLUBS.SAVE_ERROR'));
    this.error.set(msg);
  }

  cancel(): void {
    if (this.clubSlug()) {
      this.router.navigate(['/run-clubs', this.clubSlug()]);
    } else {
      this.router.navigate(['/run-clubs']);
    }
  }

  isFieldInvalid(name: string): boolean {
    const f = this.form.get(name);
    return !!f && f.invalid && f.touched;
  }
}
