import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { ApiService } from '../../services/api.service';
import { Training, CompletedTraining, TrainingFeedback } from '../../models/competition.model';
import { Subject, takeUntil } from 'rxjs';

interface TrainingWithCompletion extends Training {
  isCompleted: boolean;
  completedTraining?: CompletedTraining;
}

@Component({
  selector: 'app-training-completion',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSliderModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatListModule,
    MatDividerModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './training-completion.component.html',
  styleUrl: './training-completion.component.scss'
})
export class TrainingCompletionComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  selectedDate = new Date();
  trainings: TrainingWithCompletion[] = [];
  loading = false;
  uploadProgress = 0;
  isUploading = false;

  // FIT File upload
  selectedFile: File | null = null;
  dragOver = false;

  // Feedback form
  feedbackForm: FormGroup;
  showFeedbackForm = false;
  selectedTraining: TrainingWithCompletion | null = null;

  // Training type colors
  trainingTypeColors: { [key: string]: string } = {
    speed: '#ff5722',
    endurance: '#2196f3',
    strength: '#9c27b0',
    race: '#ff9800',
    fartlek: '#607d8b',
    recovery: '#4caf50',
    swimming: '#00bcd4',
    cycling: '#795548',
    general: '#9e9e9e'
  };

  constructor(
    private apiService: ApiService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.feedbackForm = this.fb.group({
      rating: [5],
      feedback: [''],
      completed: [true]
    });
  }

  ngOnInit(): void {
    this.loadTrainingsForDate();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onDateChange(): void {
    this.loadTrainingsForDate();
  }

  loadTrainingsForDate(): void {
    const dateString = this.selectedDate.toISOString().split('T')[0];
    this.loading = true;

    this.apiService.getAllTrainings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (allTrainings) => {
          // Filter trainings for the selected date
          const filteredTrainings = allTrainings.filter(training => 
            training.trainingDate === dateString
          );
          this.processTrainings(filteredTrainings, dateString);
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading trainings:', error);
          this.snackBar.open('Fehler beim Laden der Trainings', 'Schließen', { duration: 3000 });
          this.loading = false;
        }
      });
  }

  private processTrainings(trainings: Training[], dateString: string): void {
    // Map trainings to the expected format and include completion status
    this.trainings = trainings.map(training => ({
      ...training,
      // Map backend fields to component expected fields
      date: training.trainingDate,
      type: training.trainingType,
      intensity: training.intensityLevel,
      duration: training.durationMinutes,
      completed: training.isCompleted,
      description: training.trainingDescription?.name || training.name,
      // Component specific fields
      isCompleted: training.isCompleted,
      completedTraining: undefined // Will be loaded separately if needed
    }));
  }

  // FIT File Upload Methods
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
    if (!file.name.toLowerCase().endsWith('.fit')) {
      this.snackBar.open('Bitte wählen Sie eine FIT-Datei aus', 'Schließen', { duration: 3000 });
      return;
    }
    this.selectedFile = file;
  }

  uploadFitFile(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.uploadProgress = 0;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    // Simulate progress for demo
    const progressInterval = setInterval(() => {
      this.uploadProgress += 10;
      if (this.uploadProgress >= 90) {
        clearInterval(progressInterval);
      }
    }, 200);

    this.apiService.uploadFitFile(formData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          clearInterval(progressInterval);
          this.uploadProgress = 100;
          this.snackBar.open('FIT-Datei erfolgreich hochgeladen', 'Schließen', { duration: 3000 });
          this.selectedFile = null;
          this.isUploading = false;
          this.loadTrainingsForDate(); // Reload to show completed training
        },
        error: (error) => {
          clearInterval(progressInterval);
          this.isUploading = false;
          this.uploadProgress = 0;
          this.snackBar.open('Fehler beim Upload: ' + (error.error?.message || 'FIT-Datei konnte nicht verarbeitet werden'), 'Schließen', { duration: 5000 });
        }
      });
  }

  removeFile(): void {
    this.selectedFile = null;
  }

  // Training Feedback Methods
  openFeedbackForm(training: TrainingWithCompletion): void {
    this.selectedTraining = training;
    this.feedbackForm.patchValue({
      rating: training.rating || 5,
      feedback: training.feedback || '',
      completed: training.isCompleted
    });
    this.showFeedbackForm = true;
  }

  closeFeedbackForm(): void {
    this.showFeedbackForm = false;
    this.selectedTraining = null;
  }

  submitFeedback(): void {
    if (!this.selectedTraining) return;

    const formData = this.feedbackForm.value;
    const feedback: TrainingFeedback = {
      rating: formData.rating,
      feedback: formData.feedback,
      completed: formData.completed
    };

    this.apiService.updateTrainingFeedback(this.selectedTraining.id!, feedback)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedTraining) => {
          this.snackBar.open('Training-Feedback gespeichert', 'Schließen', { duration: 3000 });
          this.closeFeedbackForm();
          this.loadTrainingsForDate(); // Reload to show updates
        },
        error: (error) => {
          this.snackBar.open('Fehler beim Speichern', 'Schließen', { duration: 3000 });
        }
      });
  }

  // Quick completion toggle
  toggleTrainingCompletion(training: TrainingWithCompletion): void {
    const feedback: TrainingFeedback = {
      rating: training.rating || 5,
      feedback: training.feedback || '',
      completed: !training.isCompleted
    };

    this.apiService.updateTrainingFeedback(training.id!, feedback)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          training.isCompleted = !training.isCompleted;
          const message = training.isCompleted ? 'Training als abgeschlossen markiert' : 'Training als offen markiert';
          this.snackBar.open(message, 'Schließen', { duration: 2000 });
        },
        error: (error) => {
          this.snackBar.open('Fehler beim Aktualisieren', 'Schließen', { duration: 3000 });
        }
      });
  }

  // Utility Methods
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('de-DE');
  }

  formatTime(timeString: string): string {
    return timeString.substring(0, 5); // HH:MM
  }

  getTrainingTypeColor(type: string | undefined): string {
    return type ? this.trainingTypeColors[type] || '#9e9e9e' : '#9e9e9e';
  }

  getRatingStars(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }

  getCompletedTrainingsCount(): number {
    return this.trainings.filter(t => t.isCompleted).length;
  }

  getTotalTrainingsCount(): number {
    return this.trainings.length;
  }

  getCompletionPercentage(): number {
    if (this.trainings.length === 0) return 0;
    return Math.round((this.getCompletedTrainingsCount() / this.getTotalTrainingsCount()) * 100);
  }
}
