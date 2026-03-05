import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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

@Injectable({
  providedIn: 'root'
})
export class AiTrainingPlanService {
  private readonly baseUrl = `${environment.apiUrl}/ai/training-plan`;

  constructor(private http: HttpClient) {}

  generatePlan(userId: number, weekStartDate: string): Observable<AITrainingPlanDTO> {
    return this.http.post<AITrainingPlanDTO>(`${this.baseUrl}/generate`, { userId, weekStartDate });
  }

  getPlan(planId: string): Observable<AITrainingPlanDTO> {
    return this.http.get<AITrainingPlanDTO>(`${this.baseUrl}/${planId}`);
  }
}
