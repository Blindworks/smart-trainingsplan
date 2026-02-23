import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';

import { Training, TrainingDescription } from '../../models/competition.model';

export interface TrainingDetailsDialogData {
  training: Training;
}

@Component({
  selector: 'app-training-details-dialog',
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatDividerModule,
    MatTooltipModule,
    MatBadgeModule
  ],
  templateUrl: './training-details-dialog.component.html',
  styleUrl: './training-details-dialog.component.scss'
})
export class TrainingDetailsDialogComponent {
  training: Training;

  // Training type colors matching the main component
  trainingTypeColors: { [key: string]: string } = {
    speed: '#ff5722',
    endurance: '#2196f3',
    strength: '#9c27b0',
    race: '#ff9800',
    fartlek: '#607d8b',
    recovery: '#4caf50',
    swimming: '#00bcd4',
    cycling: '#795548',
    interval: '#e91e63',
    general: '#9e9e9e'
  };

  // Intensity colors matching the main component
  intensityColors: { [key: string]: string } = {
    high: '#f44336',
    medium: '#ff9800',
    low: '#ffeb3b',
    recovery: '#4caf50',
    rest: '#9e9e9e'
  };

  constructor(
    private dialogRef: MatDialogRef<TrainingDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TrainingDetailsDialogData
  ) {
    this.training = data.training;
  }

  onClose(): void {
    this.dialogRef.close();
  }

  onEdit(): void {
    this.dialogRef.close({ action: 'edit' });
  }

  getTrainingTypeColor(type: string | undefined): string {
    return type ? this.trainingTypeColors[type] || '#9e9e9e' : '#9e9e9e';
  }

  getIntensityColor(intensity: string | undefined): string {
    return intensity ? this.intensityColors[intensity] || '#9e9e9e' : '#9e9e9e';
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('de-DE', { 
      weekday: 'long',
      day: '2-digit', 
      month: 'long',
      year: 'numeric'
    });
  }

  formatDuration(minutes: number | undefined): string {
    if (!minutes) return 'Nicht angegeben';
    
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    
    if (hours > 0) {
      return `${hours}h ${remainingMinutes}min`;
    } else {
      return `${remainingMinutes}min`;
    }
  }

  getTypeDisplayName(type: string | undefined): string {
    const typeMap: { [key: string]: string } = {
      speed: 'Schnelligkeitstraining',
      endurance: 'Ausdauertraining',
      strength: 'Krafttraining',
      race: 'Wettkampf',
      fartlek: 'Fahrtspiel',
      recovery: 'Regenerationstraining',
      swimming: 'Schwimmen',
      cycling: 'Radfahren',
      interval: 'Intervalltraining',
      general: 'Allgemeines Training'
    };
    return type ? typeMap[type] || type : 'Nicht angegeben';
  }

  getIntensityDisplayName(intensity: string | undefined): string {
    const intensityMap: { [key: string]: string } = {
      high: 'Hoch',
      medium: 'Mittel',
      low: 'Niedrig',
      recovery: 'Regeneration',
      rest: 'Ruhe'
    };
    return intensity ? intensityMap[intensity] || intensity : 'Nicht angegeben';
  }

  hasDetailedInformation(): boolean {
    const desc = this.training.trainingDescription;
    return !!(desc && (
      desc.detailedInstructions ||
      desc.warmupInstructions ||
      desc.cooldownInstructions ||
      desc.equipment ||
      desc.tips
    ));
  }

  getCompletionStatusText(): string {
    if (this.training.isCompleted || this.training.completed) {
      return 'Abgeschlossen';
    }
    return 'Geplant';
  }

  getCompletionStatusIcon(): string {
    if (this.training.isCompleted || this.training.completed) {
      return 'check_circle';
    }
    return 'schedule';
  }

  getCompletionStatusColor(): string {
    if (this.training.isCompleted || this.training.completed) {
      return '#4caf50';
    }
    return '#ff9800';
  }
}