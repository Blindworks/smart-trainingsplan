import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';

import { ApiService } from '../../services/api.service';
import { Competition } from '../../models/competition.model';

@Component({
  selector: 'app-training-plan-upload',
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  templateUrl: './training-plan-upload.component.html',
  styleUrl: './training-plan-upload.component.scss'
})
export class TrainingPlanUploadComponent implements OnInit {
  competitionId!: number;
  competition: Competition | null = null;
  selectedFile: File | null = null;
  uploading = false;
  dragOver = false;

  // JSON Template for training plans
  jsonTemplate = {
    trainings: [
      {
        name: "Intervalltraining",
        description: "5x1000m Intervalle mit 3min Pause",
        date: "2024-01-15",
        type: "speed",
        intensity: "high",
        startTime: "18:00",
        duration: 90
      },
      {
        name: "Grundlagenausdauer",
        description: "Lockerer 10km Lauf",
        date: "2024-01-16",
        type: "endurance",
        intensity: "medium",
        startTime: "07:00",
        duration: 60
      }
    ]
  };

  trainingTypes = [
    { value: 'speed', label: 'Schnelligkeit' },
    { value: 'endurance', label: 'Ausdauer' },
    { value: 'strength', label: 'Kraft' },
    { value: 'race', label: 'Wettkampf' },
    { value: 'fartlek', label: 'Fahrtspiel' },
    { value: 'recovery', label: 'Regeneration' },
    { value: 'swimming', label: 'Schwimmen' },
    { value: 'cycling', label: 'Radfahren' },
    { value: 'general', label: 'Allgemein' }
  ];

  intensityLevels = [
    { value: 'high', label: 'Hoch' },
    { value: 'medium', label: 'Mittel' },
    { value: 'low', label: 'Niedrig' },
    { value: 'recovery', label: 'Regeneration' },
    { value: 'rest', label: 'Pause' }
  ];

  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.competitionId = +id;
      this.loadCompetition();
    }
  }

  loadCompetition(): void {
    this.apiService.getCompetitionById(this.competitionId).subscribe({
      next: (competition) => {
        this.competition = competition;
      },
      error: (error) => {
        this.snackBar.open('Fehler beim Laden des Wettkampfs', 'Schließen', { duration: 3000 });
      }
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.selectFile(files[0]);
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectFile(file);
    }
  }

  selectFile(file: File): void {
    if (file.type !== 'application/json' && !file.name.endsWith('.json')) {
      this.snackBar.open('Bitte wählen Sie eine JSON-Datei aus', 'Schließen', { duration: 3000 });
      return;
    }
    this.selectedFile = file;
  }

  uploadFile(): void {
    if (!this.selectedFile) return;

    this.uploading = true;
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('competitionId', this.competitionId.toString());

    this.apiService.uploadTrainingPlan(formData).subscribe({
      next: (response) => {
        this.snackBar.open('Trainingsplan erfolgreich hochgeladen', 'Schließen', { duration: 3000 });
        this.router.navigate(['/overview']);
      },
      error: (error) => {
        this.snackBar.open('Fehler beim Upload: ' + (error.error?.message || 'Unbekannter Fehler'), 'Schließen', { duration: 5000 });
        this.uploading = false;
      }
    });
  }

  downloadTemplate(): void {
    const dataStr = JSON.stringify(this.jsonTemplate, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'trainingsplan-vorlage.json';
    link.click();
    URL.revokeObjectURL(url);
  }

  removeFile(): void {
    this.selectedFile = null;
  }

  cancel(): void {
    this.router.navigate(['/competitions']);
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('de-DE');
  }

  getFormattedExample(): string {
    return JSON.stringify(this.jsonTemplate, null, 2);
  }
}
