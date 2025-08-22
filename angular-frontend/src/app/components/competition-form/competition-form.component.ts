import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';

import { ApiService } from '../../services/api.service';
import { Competition } from '../../models/competition.model';

@Component({
  selector: 'app-competition-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    MatIconModule
  ],
  templateUrl: './competition-form.component.html',
  styleUrl: './competition-form.component.scss'
})
export class CompetitionFormComponent implements OnInit {
  competitionForm: FormGroup;
  isEditing = false;
  competitionId: number | null = null;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.competitionForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      targetDate: ['', Validators.required],
      location: [''],
      description: ['']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.competitionId = +id;
      this.isEditing = true;
      this.loadCompetition(this.competitionId);
    }
  }

  loadCompetition(id: number): void {
    this.loading = true;
    this.apiService.getCompetitionById(id).subscribe({
      next: (competition) => {
        this.competitionForm.patchValue({
          name: competition.name,
          targetDate: new Date(competition.date || competition.targetDate || new Date()),
          location: competition.location,
          description: competition.description
        });
        this.loading = false;
      },
      error: (error) => {
        this.snackBar.open('Fehler beim Laden des Wettkampfs', 'Schließen', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.competitionForm.valid) {
      this.loading = true;
      const formData = this.competitionForm.value;
      const competition: Competition = {
        name: formData.name,
        date: formData.targetDate.toISOString().split('T')[0],
        targetDate: formData.targetDate.toISOString().split('T')[0],
        location: formData.location,
        description: formData.description
      };

      const operation = this.isEditing
        ? this.apiService.updateCompetition(this.competitionId!, competition)
        : this.apiService.createCompetition(competition);

      operation.subscribe({
        next: (result) => {
          const message = this.isEditing ? 'Wettkampf aktualisiert' : 'Wettkampf erstellt';
          this.snackBar.open(message, 'Schließen', { duration: 3000 });
          this.router.navigate(['/competitions']);
        },
        error: (error) => {
          this.snackBar.open('Fehler beim Speichern', 'Schließen', { duration: 3000 });
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/competitions']);
  }
}
