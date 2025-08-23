import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { 
  Competition, 
  TrainingPlan, 
  Training, 
  TrainingDescription, 
  CompletedTraining,
  TrainingFeedback,
  DailyTrainingCompletion 
} from '../models/competition.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  // Competition API
  getAllCompetitions(): Observable<Competition[]> {
    return this.http.get<Competition[]>(`${this.baseUrl}/competitions`);
  }

  getCompetitionById(id: number): Observable<Competition> {
    return this.http.get<Competition>(`${this.baseUrl}/competitions/${id}`);
  }

  createCompetition(competition: Competition): Observable<Competition> {
    return this.http.post<Competition>(`${this.baseUrl}/competitions`, competition);
  }

  updateCompetition(id: number, competition: Competition): Observable<Competition> {
    return this.http.put<Competition>(`${this.baseUrl}/competitions/${id}`, competition);
  }

  deleteCompetition(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/competitions/${id}`);
  }

  generateWeeks(id: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/competitions/${id}/generate-weeks`, {});
  }

  // Training Plan API
  getAllTrainingPlans(): Observable<TrainingPlan[]> {
    return this.http.get<TrainingPlan[]>(`${this.baseUrl}/training-plans`);
  }

  getTrainingPlanById(id: number): Observable<TrainingPlan> {
    return this.http.get<TrainingPlan>(`${this.baseUrl}/training-plans/${id}`);
  }

  getTrainingPlanByCompetition(competitionId: number): Observable<TrainingPlan[]> {
    return this.http.get<TrainingPlan[]>(`${this.baseUrl}/training-plans/competition/${competitionId}`);
  }

  createTrainingPlan(trainingPlan: TrainingPlan): Observable<TrainingPlan> {
    return this.http.post<TrainingPlan>(`${this.baseUrl}/training-plans`, trainingPlan);
  }

  updateTrainingPlan(id: number, trainingPlan: TrainingPlan): Observable<TrainingPlan> {
    return this.http.put<TrainingPlan>(`${this.baseUrl}/training-plans/${id}`, trainingPlan);
  }

  deleteTrainingPlan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/training-plans/${id}`);
  }

  uploadTrainingPlan(formData: FormData): Observable<any> {
    return this.http.post(`${this.baseUrl}/training-plans/upload`, formData);
  }

  // Training API
  getAllTrainings(): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.baseUrl}/trainings`);
  }

  getTrainingById(id: number): Observable<Training> {
    return this.http.get<Training>(`${this.baseUrl}/trainings/${id}`);
  }

  getTrainingsByWeek(weekId: number): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.baseUrl}/trainings/week/${weekId}`);
  }

  getTrainingsByDate(date: string): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.baseUrl}/trainings/date/${date}`);
  }

  getTrainingsByCompetitionAndDate(competitionId: number, date: string): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.baseUrl}/trainings/competition/${competitionId}/date/${date}`);
  }

  getMixedTrainings(competitionId: number, planIds: number[], date: string): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.baseUrl}/trainings/competition/${competitionId}/mixed`, {
      params: { planIds: planIds.join(','), date }
    });
  }

  getTrainingOverview(competitionIds: number[], startDate: string, endDate: string): Observable<Training[]> {
    // Use the existing /api/trainings endpoint and filter client-side
    return this.http.get<Training[]>(`${this.baseUrl}/trainings`);
  }

  createTraining(training: Training): Observable<Training> {
    return this.http.post<Training>(`${this.baseUrl}/trainings`, training);
  }

  updateTraining(id: number, training: Training): Observable<Training> {
    return this.http.put<Training>(`${this.baseUrl}/trainings/${id}`, training);
  }

  updateTrainingFeedback(id: number, feedback: TrainingFeedback): Observable<Training> {
    return this.http.put<Training>(`${this.baseUrl}/trainings/${id}/feedback`, feedback);
  }

  deleteTraining(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/trainings/${id}`);
  }

  // FIT File Upload
  uploadFitFile(formData: FormData): Observable<CompletedTraining> {
    return this.http.post<CompletedTraining>(`${this.baseUrl}/completed-trainings/upload`, formData);
  }

  getCompletedTrainingsByDate(date: string): Observable<CompletedTraining[]> {
    return this.http.get<CompletedTraining[]>(`${this.baseUrl}/completed-trainings/by-date`, {
      params: { date }
    });
  }
}
