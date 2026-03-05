import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface AIWorkoutDTO {
  type: string;
  targetZone?: string;
  durationMinutes?: number;
  description?: string;
  structure?: string;
}

export interface AITrainingDayDTO {
  date: string;
  workouts?: AIWorkoutDTO[];
}

export interface AITrainingPlanDTO {
  id?: string;
  weekStartDate: string;
  createdAt?: string;
  modelName?: string;
  modelVersion?: string;
  status?: string;
  days: AITrainingDayDTO[];
}

export interface AITrainingPlanGenerateRequest {
  userId: string;
  weekStart: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiTrainingPlanService {
  private readonly baseUrl = `${environment.apiUrl}/ai/training-plan`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  generatePlan(request: AITrainingPlanGenerateRequest): Observable<AITrainingPlanDTO> {
    return this.http.post<AITrainingPlanDTO>(
      `${this.baseUrl}/generate`,
      request,
      this.createAuthOptions()
    );
  }

  getPlan(planId: string): Observable<AITrainingPlanDTO> {
    return this.http.get<AITrainingPlanDTO>(`${this.baseUrl}/${planId}`, this.createAuthOptions());
  }

  private createAuthOptions(): { headers?: HttpHeaders } {
    const token = this.authService.getToken();
    if (!token) {
      return {};
    }
    return {
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` })
    };
  }
}
