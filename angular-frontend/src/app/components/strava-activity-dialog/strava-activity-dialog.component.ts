import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';

import { StravaActivity } from '../../models/strava.model';

export interface StravaActivityDialogData {
  activity: StravaActivity;
}

@Component({
  selector: 'app-strava-activity-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatDividerModule,
    MatTooltipModule
  ],
  templateUrl: './strava-activity-dialog.component.html',
  styleUrl: './strava-activity-dialog.component.scss'
})
export class StravaActivityDialogComponent {
  activity: StravaActivity;

  constructor(
    private dialogRef: MatDialogRef<StravaActivityDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: StravaActivityDialogData
  ) {
    this.activity = data.activity;
  }

  onClose(): void {
    this.dialogRef.close();
  }

  formatDate(isoString: string): string {
    const date = new Date(isoString);
    return date.toLocaleDateString('de-DE', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  formatTime(isoString: string): string {
    // Extract HH:MM from an ISO date string such as "2024-03-15T07:30:00Z"
    const date = new Date(isoString);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  formatDuration(seconds: number): string {
    const totalMinutes = Math.floor(seconds / 60);
    const hours = Math.floor(totalMinutes / 60);
    const remainingMinutes = totalMinutes % 60;

    if (hours > 0) {
      return `${hours}h ${remainingMinutes}min`;
    }
    return `${remainingMinutes}min`;
  }

  formatDistance(meters: number): string {
    return (meters / 1000).toFixed(1) + ' km';
  }

  formatPace(speedMs: number): string {
    if (speedMs <= 0) return '—';
    // Convert m/s to seconds per km, then to min:ss per km
    const secondsPerKm = 1000 / speedMs;
    const paceMinutes = Math.floor(secondsPerKm / 60);
    const paceSeconds = Math.round(secondsPerKm % 60).toString().padStart(2, '0');
    return `${paceMinutes}:${paceSeconds} min/km`;
  }

  formatElevation(meters: number): string {
    return meters.toFixed(0) + ' m';
  }

  formatActivityType(type: string): string {
    const typeMap: { [key: string]: string } = {
      Run: 'Laufen',
      Ride: 'Radfahren',
      Swim: 'Schwimmen',
      Walk: 'Gehen',
      Hike: 'Wandern',
      WeightTraining: 'Krafttraining',
      Yoga: 'Yoga',
      Workout: 'Training',
      VirtualRide: 'Virtuelles Radfahren',
      TrailRun: 'Trailrunning',
      NordicSki: 'Skilanglauf',
      AlpineSki: 'Skifahren',
      Rowing: 'Rudern'
    };
    return typeMap[type] || type;
  }

  getSportIcon(type: string): string {
    const iconMap: { [key: string]: string } = {
      Run: 'directions_run',
      Ride: 'directions_bike',
      VirtualRide: 'directions_bike',
      Swim: 'pool',
      Walk: 'hiking',
      Hike: 'hiking',
      WeightTraining: 'fitness_center',
      Yoga: 'self_improvement',
      Rowing: 'rowing'
    };
    return iconMap[type] || 'sports';
  }
}
