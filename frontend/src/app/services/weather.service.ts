import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError, map } from 'rxjs';
import { apiUrl } from '../core/api-base';

export type WeatherVerdict = 'GOOD' | 'CAUTION' | 'BAD' | 'UNKNOWN';

export interface RunWeatherForecastDto {
  verdict: WeatherVerdict;
  locationLabel: string | null;
  latitude: number | null;
  longitude: number | null;
  validFrom: string | null;
  validUntil: string | null;
  dataSource: string | null;
  currentTemperatureC: number | null;
  currentPrecipitationProbabilityPct: number | null;
  currentPrecipitationMm: number | null;
  currentWindKmh: number | null;
  currentWeatherCode: number | null;
  maxPrecipitationProbabilityPct: number | null;
  totalPrecipitationMm: number | null;
  maxWindKmh: number | null;
  minTemperatureC: number | null;
  maxTemperatureC: number | null;
  thunderstormExpected: boolean | null;
  firstRainAt: string | null;
  hourly: HourlyForecastPoint[] | null;
  reasons: string[] | null;
}

export interface HourlyForecastPoint {
  time: string;
  temperatureC: number | null;
  precipitationProbabilityPct: number | null;
  precipitationMm: number | null;
  windKmh: number | null;
  weatherCode: number | null;
}

@Injectable({ providedIn: 'root' })
export class WeatherService {
  private readonly http = inject(HttpClient);

  /**
   * Fetches the short-term run-weather forecast for the current user.
   * Returns null when the user has no location set (backend returns 204 No Content).
   */
  getForecast(): Observable<RunWeatherForecastDto | null> {
    return this.http
      .get<RunWeatherForecastDto>(apiUrl('/weather/forecast'), { observe: 'response' })
      .pipe(
        map(resp => (resp.status === 204 ? null : resp.body)),
        catchError(() => of(null))
      );
  }
}
