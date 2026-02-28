import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSelectModule } from '@angular/material/select';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, startWith, switchMap } from 'rxjs/operators';

import { ApiService } from '../../services/api.service';
import { Competition } from '../../models/competition.model';

export interface AdminRaceEditDialogData {
  race: Competition | null;
}

interface NominatimResult {
  display_name: string;
  address: {
    city?: string;
    town?: string;
    village?: string;
    municipality?: string;
    country?: string;
  };
}

@Component({
  selector: 'app-admin-race-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatAutocompleteModule,
    MatSelectModule
  ],
  templateUrl: './admin-race-edit-dialog.component.html',
  styleUrl: './admin-race-edit-dialog.component.scss'
})
export class AdminRaceEditDialogComponent {
  saving = false;
  errorMessage = '';
  isNew: boolean;
  competitionTypes: string[] = [];
  form!: ReturnType<FormBuilder['group']>;
  citySuggestions$!: Observable<string[]>;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private dialogRef: MatDialogRef<AdminRaceEditDialogComponent>,
    private apiService: ApiService,
    @Inject(MAT_DIALOG_DATA) public data: AdminRaceEditDialogData
  ) {
    this.isNew = data.race === null;
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      date: [null as Date | null, Validators.required],
      type: [''],
      location: [''],
      description: ['']
    });
    if (data.race) {
      this.form.patchValue({
        name: data.race.name,
        date: data.race.date ? new Date(data.race.date) : null,
        type: data.race.type ?? '',
        location: data.race.location ?? '',
        description: data.race.description ?? ''
      });
    }

    this.apiService.getCompetitionTypes().subscribe(types => this.competitionTypes = types);

    this.citySuggestions$ = this.form.get('location')!.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(value => {
        if (!value || value.length < 2) return of([]);
        return this.http.get<NominatimResult[]>(
          `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(value)}&format=json&limit=5&addressdetails=1&featureType=city`
        ).pipe(
          map(results => results.map(r => {
            const city = r.address.city || r.address.town || r.address.municipality || r.address.village || '';
            const country = r.address.country || '';
            return city && country ? `${city}, ${country}` : r.display_name;
          })),
          catchError(() => of([]))
        );
      })
    );
  }

  onCancel(): void {
    if (!this.saving) this.dialogRef.close();
  }

  onSave(): void {
    if (this.form.invalid || this.saving) return;
    this.saving = true;
    this.errorMessage = '';

    const v = this.form.getRawValue();
    const competition: Competition = {
      name: v.name!,
      date: (v.date as Date).toISOString().split('T')[0],
      type: v.type || undefined,
      location: v.location || undefined,
      description: v.description || undefined
    };

    const op = this.isNew
      ? this.apiService.createCompetition(competition)
      : this.apiService.updateCompetition(this.data.race!.id!, competition);

    op.subscribe({
      next: (result) => this.dialogRef.close(result),
      error: () => {
        this.errorMessage = 'Race konnte nicht gespeichert werden.';
        this.saving = false;
      }
    });
  }
}
