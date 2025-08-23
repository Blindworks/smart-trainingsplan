import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';

import { ApiService } from '../../services/api.service';
import { Competition, Training, CompletedTraining } from '../../models/competition.model';
import { Subject, takeUntil, forkJoin } from 'rxjs';

interface DayTraining {
  date: string;
  trainings: Training[];
  completedTrainings: CompletedTraining[];
  isEmpty: boolean;
}

interface WeekData {
  weekNumber: number;
  startDate: Date;
  endDate: Date;
  days: DayTraining[];
}

@Component({
  selector: 'app-training-plan-overview',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatBadgeModule
  ],
  templateUrl: './training-plan-overview.component.html',
  styleUrl: './training-plan-overview.component.scss'
})
export class TrainingPlanOverviewComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  competitions: Competition[] = [];
  selectedCompetitions: number[] = [];
  currentDate = new Date();
  weekData: WeekData | null = null;
  loading = false;
  showEmptyDays = true;

  // FIT File Upload
  showFitUploadModal = false;
  selectedFile: File | null = null;
  selectedTrainingForUpload: Training | null = null;
  selectedUploadDate = '';
  dragOver = false;
  isUploading = false;
  uploadProgress = 0;

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

  // Intensity colors
  intensityColors: { [key: string]: string } = {
    high: '#f44336',
    medium: '#ff9800',
    low: '#ffeb3b',
    recovery: '#4caf50',
    rest: '#9e9e9e'
  };

  dayNames = ['Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag', 'Sonntag'];

  constructor(
    private apiService: ApiService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCompetitions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (event.target && (event.target as HTMLElement).tagName === 'INPUT') {
      return;
    }

    switch (event.key) {
      case 'ArrowLeft':
        event.preventDefault();
        this.goToPreviousWeek();
        break;
      case 'ArrowRight':
        event.preventDefault();
        this.goToNextWeek();
        break;
      case 't':
      case 'T':
        event.preventDefault();
        this.goToToday();
        break;
    }
  }

  loadCompetitions(): void {
    this.apiService.getAllCompetitions()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (competitions) => {
          this.competitions = competitions;
          // Select all competitions by default
          this.selectedCompetitions = competitions.map(c => c.id!);
          this.loadWeekData();
        },
        error: (error) => {
          this.snackBar.open('Fehler beim Laden der Wettkämpfe', 'Schließen', { duration: 3000 });
        }
      });
  }

  onCompetitionSelectionChange(): void {
    this.loadWeekData();
  }

  loadWeekData(): void {
    this.loading = true;
    const week = this.getWeekDates(this.currentDate);
    
    // Load all trainings and filter client-side
    this.apiService.getTrainingOverview([], '', '')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (allTrainings) => {
          this.processWeekData(week, allTrainings);
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading trainings:', error);
          this.snackBar.open('Fehler beim Laden der Trainingsdaten', 'Schließen', { duration: 3000 });
          this.loading = false;
        }
      });
  }

  private processWeekData(week: { startDate: Date; endDate: Date }, allTrainings: Training[]): void {
    const mondayOfWeek = this.getMondayOfWeek(this.currentDate);
    
    const weekData: WeekData = {
      weekNumber: this.getWeekNumber(mondayOfWeek),
      startDate: mondayOfWeek,
      endDate: new Date(mondayOfWeek.getTime() + 6 * 24 * 60 * 60 * 1000), // Sunday
      days: this.createWeekDays(mondayOfWeek)
    };

    this.populateTrainingsForWeek(weekData, allTrainings);
    this.loadCompletedTrainingsForWeek(weekData);
    
    this.weekData = weekData;
  }

  private createWeekDays(startDate: Date): DayTraining[] {
    const days: DayTraining[] = [];
    
    for (let i = 0; i < 7; i++) {
      const date = new Date(startDate);
      date.setDate(startDate.getDate() + i);
      
      const dateString = this.formatDateString(date);
      
      days.push({
        date: dateString,
        trainings: [],
        completedTrainings: [],
        isEmpty: true
      });
    }
    
    return days;
  }

  private formatDateString(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private populateTrainingsForWeek(weekData: WeekData, allTrainings: Training[]): void {
    allTrainings.forEach(training => {
      const dayIndex = weekData.days.findIndex(d => d.date === training.trainingDate);
      if (dayIndex !== -1) {
        const mappedTraining = this.mapTrainingForDisplay(training);
        weekData.days[dayIndex].trainings.push(mappedTraining);
        weekData.days[dayIndex].isEmpty = false;
      }
    });
  }

  private mapTrainingForDisplay(training: Training): Training {
    return {
      ...training,
      date: training.trainingDate,
      type: training.trainingType,
      intensity: training.intensityLevel,
      duration: training.durationMinutes,
      completed: training.isCompleted,
      description: training.trainingDescription?.name || training.name,
      trainingPlanName: training.trainingPlanName,
      trainingPlanId: training.trainingPlanId
    };
  }

  private getMondayOfWeek(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // adjust when day is sunday
    const monday = new Date(d.setDate(diff));
    monday.setHours(0, 0, 0, 0);
    return monday;
  }

  private getWeekDates(date: Date): { startDate: Date; endDate: Date } {
    const startDate = this.getMondayOfWeek(date);
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + 6); // Sunday
    endDate.setHours(23, 59, 59, 999);
    return { startDate, endDate };
  }

  private getWeekNumber(date: Date): number {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
  }

  goToPreviousWeek(): void {
    this.currentDate.setDate(this.currentDate.getDate() - 7);
    this.loadWeekData();
  }

  goToNextWeek(): void {
    this.currentDate.setDate(this.currentDate.getDate() + 7);
    this.loadWeekData();
  }

  goToToday(): void {
    this.currentDate = new Date();
    this.loadWeekData();
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('de-DE', { 
      day: '2-digit', 
      month: '2-digit' 
    });
  }

  formatDayName(dateString: string): string {
    // Parse the date string directly as YYYY-MM-DD
    const [year, month, day] = dateString.split('-').map(Number);
    const date = new Date(year, month - 1, day); // month is 0-indexed in Date constructor
    const dayOfWeek = date.getDay(); // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    
    // Map JavaScript day (0=Sunday) to our array index (0=Monday)  
    const dayNames = ['Sonntag', 'Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag'];
    return dayNames[dayOfWeek];
  }

  isToday(dateString: string): boolean {
    const date = new Date(dateString);
    const today = new Date();
    return date.toDateString() === today.toDateString();
  }

  getTrainingTypeColor(type: string | undefined): string {
    return type ? this.trainingTypeColors[type] || '#9e9e9e' : '#9e9e9e';
  }

  getIntensityColor(intensity: string | undefined): string {
    return intensity ? this.intensityColors[intensity] || '#9e9e9e' : '#9e9e9e';
  }

  openTrainingDetails(training: Training): void {
    // TODO: Implement training details modal
    this.snackBar.open('Training Details Modal wird noch implementiert', 'Schließen', { duration: 2000 });
  }

  getCompetitionName(competitionId: number): string {
    const competition = this.competitions.find(c => c.id === competitionId);
    return competition ? competition.name : 'Unbekannt';
  }

  getTotalTrainingsCount(): number {
    if (!this.weekData) return 0;
    return this.weekData.days.reduce((total, day) => total + day.trainings.length, 0);
  }

  getCompletedTrainingsCount(): number {
    if (!this.weekData) return 0;
    return this.weekData.days.reduce((total, day) => total + day.completedTrainings.length, 0);
  }

  private loadCompletedTrainingsForWeek(weekData: WeekData): void {
    // Load completed trainings for each day of the week
    weekData.days.forEach(day => {
      this.apiService.getCompletedTrainingsByDate(day.date)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (completedTrainings) => {
            day.completedTrainings = completedTrainings;
            if (completedTrainings.length > 0) {
              day.isEmpty = false;
            }
          },
          error: (error) => {
            // Silently handle errors for completed trainings
            console.warn('Could not load completed trainings for', day.date);
          }
        });
    });
  }

  // Training Completion Methods
  toggleTrainingCompletion(training: Training): void {
    const newCompletionStatus = !training.completed;
    
    this.apiService.updateTrainingFeedback(training.id!, {
      completed: newCompletionStatus,
      rating: training.rating || 5,
      feedback: training.feedback || ''
    }).pipe(takeUntil(this.destroy$))
    .subscribe({
      next: () => {
        training.completed = newCompletionStatus;
        const message = newCompletionStatus ? 'Training als abgeschlossen markiert' : 'Training als offen markiert';
        this.snackBar.open(message, 'Schließen', { duration: 2000 });
        this.loadWeekData(); // Reload to refresh display
      },
      error: (error) => {
        this.snackBar.open('Fehler beim Aktualisieren des Training-Status', 'Schließen', { duration: 3000 });
      }
    });
  }

  // FIT File Upload Methods
  openFitFileUpload(training: Training, date: string): void {
    this.selectedTrainingForUpload = training;
    this.selectedUploadDate = date;
    this.showFitUploadModal = true;
    this.selectedFile = null;
    this.uploadProgress = 0;
  }

  closeFitUploadModal(): void {
    this.showFitUploadModal = false;
    this.selectedTrainingForUpload = null;
    this.selectedUploadDate = '';
    this.selectedFile = null;
    this.dragOver = false;
    this.isUploading = false;
    this.uploadProgress = 0;
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
    if (!file.name.toLowerCase().endsWith('.fit')) {
      this.snackBar.open('Bitte wählen Sie eine FIT-Datei aus', 'Schließen', { duration: 3000 });
      return;
    }
    this.selectedFile = file;
  }

  removeFile(): void {
    this.selectedFile = null;
  }

  uploadFitFile(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.uploadProgress = 0;

    const formData = new FormData();
    formData.append('file', this.selectedFile);
    if (this.selectedUploadDate) {
      formData.append('trainingDate', this.selectedUploadDate);
    }

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
          this.closeFitUploadModal();
          this.loadWeekData(); // Reload to show completed training
        },
        error: (error) => {
          clearInterval(progressInterval);
          this.isUploading = false;
          this.uploadProgress = 0;
          this.snackBar.open('Fehler beim Upload: ' + (error.error?.message || 'FIT-Datei konnte nicht verarbeitet werden'), 'Schließen', { duration: 5000 });
        }
      });
  }

}
