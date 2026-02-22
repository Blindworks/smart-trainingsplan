import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-body-status',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './body-status.component.html',
  styleUrl: './body-status.component.scss'
})
export class BodyStatusComponent implements OnInit {
  loading = true;
  vo2max: number | null = null;
  trainingDate?: string;
  activityName?: string;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadCurrentVo2Max();
  }

  loadCurrentVo2Max(): void {
    this.loading = true;
    this.apiService.getCurrentVo2Max().subscribe({
      next: (response) => {
        this.vo2max = response?.vo2max ?? null;
        this.trainingDate = response?.trainingDate;
        this.activityName = response?.activityName;
        this.loading = false;
      },
      error: () => {
        this.vo2max = null;
        this.trainingDate = undefined;
        this.activityName = undefined;
        this.loading = false;
      }
    });
  }

  getVo2MaxCategory(vo2max: number): string {
    if (vo2max >= 60) return 'Exzellent';
    if (vo2max >= 52) return 'Sehr gut';
    if (vo2max >= 44) return 'Gut';
    if (vo2max >= 36) return 'Durchschnittlich';
    return 'Ausbaufähig';
  }

  getVo2MaxColor(vo2max: number): string {
    if (vo2max >= 60) return '#4caf50';
    if (vo2max >= 52) return '#8bc34a';
    if (vo2max >= 44) return '#ffb300';
    if (vo2max >= 36) return '#ff7043';
    return '#ef5350';
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) {
      return '';
    }
    return new Date(dateString).toLocaleDateString('de-DE');
  }
}
