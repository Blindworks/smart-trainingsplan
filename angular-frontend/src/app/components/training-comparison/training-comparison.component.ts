import { Component, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Chart, registerables } from 'chart.js';
import { ApiService } from '../../services/api.service';
import { ActivityComparisonItem, CompletedTraining } from '../../models/competition.model';
import { catchError, of } from 'rxjs';

Chart.register(...registerables);

const CHART_COLORS = [
  { bg: 'rgba(45, 123, 255, 0.75)',  border: '#2D7BFF' },
  { bg: 'rgba(255, 107, 53, 0.75)',  border: '#FF6B35' },
  { bg: 'rgba(76, 175, 80, 0.75)',   border: '#4CAF50' },
  { bg: 'rgba(156, 39, 176, 0.75)',  border: '#9C27B0' },
  { bg: 'rgba(255, 152, 0, 0.75)',   border: '#FF9800' },
  { bg: 'rgba(0, 188, 212, 0.75)',   border: '#00BCD4' },
];

const ZONE_COLORS = [
  'rgba(100, 181, 246, 0.85)',  // Z1 blue
  'rgba(102, 187, 106, 0.85)',  // Z2 green
  'rgba(255, 202, 40, 0.85)',   // Z3 yellow
  'rgba(255, 112, 67, 0.85)',   // Z4 orange
  'rgba(239, 83, 80, 0.85)',    // Z5 red
];

@Component({
  selector: 'app-training-comparison',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  templateUrl: './training-comparison.component.html',
  styleUrl: './training-comparison.component.scss'
})
export class TrainingComparisonComponent implements OnInit, OnDestroy {
  startDate = '';
  endDate = '';
  activities: CompletedTraining[] = [];
  selectedIds = new Set<number>();
  comparisonData: ActivityComparisonItem[] = [];
  loading = false;
  comparing = false;
  searchDone = false;

  @ViewChild('metricsCanvas') metricsCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('zonesCanvas')   zonesCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('radarCanvas')   radarCanvas!: ElementRef<HTMLCanvasElement>;

  private charts: Chart[] = [];

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    const today = new Date();
    const ago90 = new Date(today.getTime() - 90 * 24 * 60 * 60 * 1000);
    this.endDate   = today.toISOString().split('T')[0];
    this.startDate = ago90.toISOString().split('T')[0];
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  search(): void {
    this.loading = true;
    this.searchDone = false;
    this.activities = [];
    this.selectedIds.clear();
    this.comparisonData = [];
    this.destroyCharts();

    this.apiService.getCompletedTrainingsByDateRange(this.startDate, this.endDate)
      .pipe(catchError(() => of([])))
      .subscribe(data => {
        this.activities = [...data].sort((a, b) => b.trainingDate.localeCompare(a.trainingDate));
        this.loading = false;
        this.searchDone = true;
      });
  }

  toggleSelection(id: number | undefined): void {
    if (id == null) return;
    if (this.selectedIds.has(id)) {
      this.selectedIds.delete(id);
    } else if (this.selectedIds.size < 6) {
      this.selectedIds.add(id);
    }
  }

  isDisabled(id: number | undefined): boolean {
    if (id == null) return true;
    return !this.selectedIds.has(id) && this.selectedIds.size >= 6;
  }

  compare(): void {
    if (this.selectedIds.size < 2) return;
    this.comparing = true;
    this.comparisonData = [];
    this.destroyCharts();

    this.apiService.compareActivities(Array.from(this.selectedIds))
      .pipe(catchError(() => of([])))
      .subscribe(data => {
        this.comparisonData = data;
        this.comparing = false;
        setTimeout(() => this.drawCharts(), 60);
      });
  }

  private destroyCharts(): void {
    this.charts.forEach(c => c.destroy());
    this.charts = [];
  }

  private drawCharts(): void {
    this.destroyCharts();
    if (!this.comparisonData.length) return;

    const labels = this.comparisonData.map((a, i) =>
      a.activityName ?? this.formatSportName(a.sport) + ' ' + this.formatDateShort(a.trainingDate)
    );

    this.drawMetricsChart(labels);
    this.drawZonesChart(labels);
    this.drawRadarChart(labels);
  }

  private drawMetricsChart(labels: string[]): void {
    const el = this.metricsCanvas?.nativeElement;
    if (!el) return;

    // Normalize each metric to 0-100 score
    const paces    = this.comparisonData.map(a => a.averagePaceSecondsPerKm ?? null);
    const hrs      = this.comparisonData.map(a => a.averageHeartRate ?? null);
    const dists    = this.comparisonData.map(a => a.distanceKm ?? null);
    const elevs    = this.comparisonData.map(a => a.elevationGainM ?? null);
    const powers   = this.comparisonData.map(a => a.averagePowerWatts ?? null);

    const normalize = (vals: (number | null)[], invert = false): (number | null)[] => {
      const valid = vals.filter(v => v != null) as number[];
      if (!valid.length) return vals.map(() => null);
      const min = Math.min(...valid);
      const max = Math.max(...valid);
      if (min === max) return vals.map(v => v == null ? null : 100);
      return vals.map(v => {
        if (v == null) return null;
        const score = (v - min) / (max - min) * 100;
        return invert ? 100 - score : score;
      });
    };

    const datasets: any[] = [];

    const hasPace  = paces.some(v => v != null);
    const hasHR    = hrs.some(v => v != null);
    const hasDist  = dists.some(v => v != null);
    const hasElev  = elevs.some(v => v != null);
    const hasPower = powers.some(v => v != null);

    if (hasPace)  datasets.push({ label: 'Pace-Score',     data: normalize(paces, true),  backgroundColor: 'rgba(45, 123, 255, 0.7)', borderColor: '#2D7BFF', borderWidth: 1 });
    if (hasHR)    datasets.push({ label: 'HF-Effizienz',   data: normalize(hrs, true),     backgroundColor: 'rgba(239, 83, 80, 0.7)',  borderColor: '#ef5350', borderWidth: 1 });
    if (hasDist)  datasets.push({ label: 'Distanz-Score',  data: normalize(dists, false),  backgroundColor: 'rgba(76, 175, 80, 0.7)', borderColor: '#4CAF50', borderWidth: 1 });
    if (hasElev)  datasets.push({ label: 'Höhen-Score',    data: normalize(elevs, false),  backgroundColor: 'rgba(255, 152, 0, 0.7)', borderColor: '#FF9800', borderWidth: 1 });
    if (hasPower) datasets.push({ label: 'Leistungs-Score',data: normalize(powers, false), backgroundColor: 'rgba(156, 39, 176, 0.7)',borderColor: '#9C27B0', borderWidth: 1 });

    const chart = new Chart(el, {
      type: 'bar',
      data: { labels, datasets },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'bottom', labels: { color: '#e0e0e0', font: { size: 11 } } },
          tooltip: {
            callbacks: {
              label: ctx => `${ctx.dataset.label}: ${(ctx.raw as number)?.toFixed(0) ?? '—'} Pkt`
            }
          }
        },
        scales: {
          x: { ticks: { color: '#aaa', maxRotation: 20 }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: {
            beginAtZero: true, max: 100,
            ticks: { color: '#aaa', callback: (v) => v + ' Pkt' },
            grid: { color: 'rgba(255,255,255,0.06)' },
            title: { display: true, text: 'Score (0–100, höher = besser)', color: '#aaa', font: { size: 11 } }
          }
        }
      }
    });
    this.charts.push(chart);
  }

  private drawZonesChart(labels: string[]): void {
    const el = this.zonesCanvas?.nativeElement;
    if (!el) return;

    const hasZones = this.comparisonData.some(a =>
      (a.z1Min ?? 0) + (a.z2Min ?? 0) + (a.z3Min ?? 0) + (a.z4Min ?? 0) + (a.z5Min ?? 0) > 0
    );
    if (!hasZones) return;

    const zoneNames = ['Zone 1 (Regeneration)', 'Zone 2 (Grundlage)', 'Zone 3 (Tempo)', 'Zone 4 (Schwelle)', 'Zone 5 (Anaerob)'];
    const zoneGetters: ((a: ActivityComparisonItem) => number)[] = [
      a => a.z1Min ?? 0,
      a => a.z2Min ?? 0,
      a => a.z3Min ?? 0,
      a => a.z4Min ?? 0,
      a => a.z5Min ?? 0,
    ];

    const datasets = zoneNames.map((name, i) => ({
      label: name,
      data: this.comparisonData.map(a => +zoneGetters[i](a).toFixed(1)),
      backgroundColor: ZONE_COLORS[i],
      borderColor: ZONE_COLORS[i].replace('0.85', '1'),
      borderWidth: 1
    }));

    const chart = new Chart(el, {
      type: 'bar',
      data: { labels, datasets },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'bottom', labels: { color: '#e0e0e0', font: { size: 11 } } },
          tooltip: {
            callbacks: {
              label: ctx => `${ctx.dataset.label}: ${(ctx.raw as number).toFixed(1)} min`
            }
          }
        },
        scales: {
          x: { stacked: true, ticks: { color: '#aaa', maxRotation: 20 }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: {
            stacked: true,
            ticks: { color: '#aaa', callback: v => v + ' min' },
            grid: { color: 'rgba(255,255,255,0.06)' },
            title: { display: true, text: 'Zeit in Zone (Minuten)', color: '#aaa', font: { size: 11 } }
          }
        }
      }
    });
    this.charts.push(chart);
  }

  private drawRadarChart(labels: string[]): void {
    const el = this.radarCanvas?.nativeElement;
    if (!el) return;

    const axisLabels = ['Pace', 'Ø Herzfrequenz', 'Distanz', 'Höhenmeter', 'Belastung (Strain)'];
    const normalize = (vals: (number | null)[], invert = false): number[] => {
      const valid = vals.filter(v => v != null) as number[];
      if (!valid.length) return vals.map(() => 0);
      const min = Math.min(...valid);
      const max = Math.max(...valid);
      if (min === max) return vals.map(v => v == null ? 0 : 75);
      return vals.map(v => {
        if (v == null) return 0;
        const score = (v - min) / (max - min) * 100;
        return invert ? 100 - score : score;
      });
    };

    const paceScores   = normalize(this.comparisonData.map(a => a.averagePaceSecondsPerKm ?? null), true);
    const hrScores     = normalize(this.comparisonData.map(a => a.averageHeartRate ?? null), true);
    const distScores   = normalize(this.comparisonData.map(a => a.distanceKm ?? null), false);
    const elevScores   = normalize(this.comparisonData.map(a => a.elevationGainM ?? null), false);
    const strainScores = normalize(this.comparisonData.map(a => a.strain21 ?? null), false);

    const datasets = this.comparisonData.map((a, i) => ({
      label: a.activityName ?? this.formatSportName(a.sport) + ' ' + this.formatDateShort(a.trainingDate),
      data: [paceScores[i], hrScores[i], distScores[i], elevScores[i], strainScores[i]],
      backgroundColor: CHART_COLORS[i % CHART_COLORS.length].bg,
      borderColor: CHART_COLORS[i % CHART_COLORS.length].border,
      borderWidth: 2,
      pointBackgroundColor: CHART_COLORS[i % CHART_COLORS.length].border,
      pointRadius: 4
    }));

    const chart = new Chart(el, {
      type: 'radar',
      data: { labels: axisLabels, datasets },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'bottom', labels: { color: '#e0e0e0', font: { size: 11 } } },
          tooltip: {
            callbacks: {
              label: ctx => `${ctx.dataset.label}: ${(ctx.raw as number).toFixed(0)} Pkt`
            }
          }
        },
        scales: {
          r: {
            beginAtZero: true,
            max: 100,
            ticks: { color: '#888', backdropColor: 'transparent', stepSize: 25 },
            grid: { color: 'rgba(255,255,255,0.1)' },
            angleLines: { color: 'rgba(255,255,255,0.1)' },
            pointLabels: { color: '#ccc', font: { size: 11 } }
          }
        }
      }
    });
    this.charts.push(chart);
  }

  // ---- Formatting helpers ----

  getActivityLabel(a: ActivityComparisonItem, index: number): string {
    return a.activityName ?? (this.formatSportName(a.sport) + ' ' + this.formatDateShort(a.trainingDate));
  }

  getColor(index: number): string {
    return CHART_COLORS[index % CHART_COLORS.length].border;
  }

  hasZoneData(a: ActivityComparisonItem): boolean {
    return (a.z1Min ?? 0) + (a.z2Min ?? 0) + (a.z3Min ?? 0) + (a.z4Min ?? 0) + (a.z5Min ?? 0) > 0;
  }

  getSportIcon(sport: string | undefined): string {
    const s = (sport ?? '').toLowerCase();
    if (s.includes('run'))                        return 'directions_run';
    if (s.includes('rid') || s.includes('cycl')) return 'directions_bike';
    if (s.includes('swim'))                       return 'pool';
    if (s.includes('walk') || s.includes('hike'))return 'hiking';
    if (s.includes('weight') || s.includes('strength')) return 'fitness_center';
    return 'sports';
  }

  formatSportName(sport: string | undefined): string {
    const map: Record<string, string> = {
      running: 'Laufen', cycling: 'Radfahren', swimming: 'Schwimmen',
      Run: 'Laufen', Ride: 'Radfahren', Swim: 'Schwimmen', generic: 'Training'
    };
    if (!sport) return 'Training';
    return map[sport] ?? (sport.charAt(0).toUpperCase() + sport.slice(1));
  }

  formatDate(dateStr: string): string {
    const [y, m, d] = dateStr.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('de-DE', {
      weekday: 'short', day: '2-digit', month: '2-digit', year: '2-digit'
    });
  }

  formatDateShort(dateStr: string): string {
    const [y, m, d] = dateStr.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit' });
  }

  formatDuration(sec: number | undefined): string {
    if (!sec) return '—';
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    return h > 0 ? `${h}h ${m}min` : `${m}min`;
  }

  formatPace(spk: number | undefined): string {
    if (!spk) return '—';
    const min = Math.floor(spk / 60);
    const sec = Math.round(spk % 60).toString().padStart(2, '0');
    return `${min}:${sec} /km`;
  }

  formatDistance(km: number | undefined): string {
    if (!km) return '—';
    return `${km.toFixed(2)} km`;
  }

  formatSpeed(kmh: number | undefined): string {
    if (!kmh) return '—';
    return `${kmh.toFixed(1)} km/h`;
  }

  getDisplayName(a: CompletedTraining): string {
    return a.activityName ?? this.formatSportName(a.sport);
  }
}
