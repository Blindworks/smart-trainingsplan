import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  ElementRef,
  effect,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import * as L from 'leaflet';

import { RunClubService } from '../../services/run-club.service';
import { ThemeService } from '../../services/theme.service';
import { UserService } from '../../services/user.service';
import { ALL_DAYS_OF_WEEK, DayOfWeek, RunClub } from '../../models/run-club.model';

type TabKey = 'list' | 'map';

@Component({
  selector: 'app-run-clubs',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule],
  templateUrl: './run-clubs.html',
  styleUrl: './run-clubs.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RunClubs implements OnInit, AfterViewInit, OnDestroy {
  private readonly service = inject(RunClubService);
  private readonly userService = inject(UserService);
  private readonly themeService = inject(ThemeService);
  private readonly router = inject(Router);
  readonly translate = inject(TranslateService);

  @ViewChild('mapEl') private mapEl?: ElementRef<HTMLDivElement>;

  readonly clubs = signal<RunClub[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<TabKey>('list');

  readonly searchQuery = signal('');
  readonly cityFilter = signal('');
  readonly dayFilter = signal<DayOfWeek | ''>('');
  readonly allDays = ALL_DAYS_OF_WEEK;

  private map: L.Map | null = null;
  private tileLayer: L.TileLayer | null = null;
  private markersLayer: L.LayerGroup | null = null;
  private currentTheme: 'light' | 'dark' = 'dark';

  constructor() {
    effect(() => {
      const t = this.themeService.resolved();
      if (this.map && t !== this.currentTheme) {
        this.currentTheme = t;
        this.refreshTileLayer();
      }
    });
  }

  ngOnInit(): void {
    const user = this.userService.currentUser();
    if (user && user.runClubsEnabled === false) {
      this.router.navigate(['/settings']);
      return;
    }
    this.load();
  }

  ngAfterViewInit(): void {
    // Map initialized lazily on tab switch
  }

  ngOnDestroy(): void {
    this.destroyMap();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service
      .search(this.cityFilter() || null, this.searchQuery() || null, this.dayFilter() || null)
      .subscribe({
        next: data => {
          this.clubs.set(data);
          this.loading.set(false);
          if (this.activeTab() === 'map') {
            setTimeout(() => this.renderMarkers(), 50);
          }
        },
        error: () => {
          this.error.set(this.translate.instant('RUN_CLUBS.LOAD_ERROR'));
          this.loading.set(false);
        }
      });
  }

  setTab(tab: TabKey): void {
    this.activeTab.set(tab);
    if (tab === 'map') {
      setTimeout(() => this.ensureMap(), 50);
    }
  }

  onSearch(): void {
    this.load();
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.cityFilter.set('');
    this.dayFilter.set('');
    this.load();
  }

  trackById = (_: number, c: RunClub) => c.id;

  logoUrl(c: RunClub): string | null {
    return c.hasLogo ? this.service.getLogoUrl(c.id, c.id) : null;
  }

  meetingDaysShort(c: RunClub): string {
    if (!c.meetingDays || c.meetingDays.length === 0) return '';
    return c.meetingDays.map(d => this.translate.instant('RUN_CLUBS.DAY_' + d.substring(0, 3))).join(', ');
  }

  private ensureMap(): void {
    if (this.map || !this.mapEl) return;
    this.currentTheme = this.themeService.resolved();
    this.map = L.map(this.mapEl.nativeElement, {
      center: [51.16, 10.45],
      zoom: 6,
      zoomControl: true
    });
    this.tileLayer = this.createTileLayer();
    this.tileLayer.addTo(this.map);
    this.markersLayer = L.layerGroup().addTo(this.map);
    this.renderMarkers();
    setTimeout(() => this.map?.invalidateSize(), 100);
  }

  private renderMarkers(): void {
    if (!this.map || !this.markersLayer) return;
    this.markersLayer.clearLayers();

    const points: L.LatLngExpression[] = [];
    for (const c of this.clubs()) {
      if (c.latitude == null || c.longitude == null) continue;
      const html = this.logoUrl(c)
        ? `<div class="rc-marker"><img src="${this.logoUrl(c)}" alt="${c.name}"/></div>`
        : `<div class="rc-marker rc-marker--initials">${this.initials(c.name)}</div>`;
      const icon = L.divIcon({
        className: '',
        html,
        iconSize: [40, 40],
        iconAnchor: [20, 20]
      });
      const marker = L.marker([c.latitude, c.longitude], { icon });
      marker.on('click', () => this.router.navigate(['/run-clubs', c.slug]));
      marker.addTo(this.markersLayer);
      points.push([c.latitude, c.longitude]);
    }

    if (points.length > 0) {
      this.map.fitBounds(L.latLngBounds(points as L.LatLngTuple[]), { padding: [40, 40], maxZoom: 12 });
    }
  }

  private createTileLayer(): L.TileLayer {
    const isDark = this.currentTheme === 'dark';
    const tileUrl = isDark
      ? 'https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/rastertiles/light_all/{z}/{x}/{y}{r}.png';
    return L.tileLayer(tileUrl, {
      attribution: '&copy; OpenStreetMap &copy; CARTO',
      subdomains: 'abcd',
      maxZoom: 19
    });
  }

  private refreshTileLayer(): void {
    if (!this.map) return;
    if (this.tileLayer) this.map.removeLayer(this.tileLayer);
    this.tileLayer = this.createTileLayer();
    this.tileLayer.addTo(this.map);
  }

  private destroyMap(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
    this.markersLayer = null;
    this.tileLayer = null;
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(p => p[0]?.toUpperCase() ?? '')
      .join('');
  }
}
