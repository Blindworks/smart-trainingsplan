import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Competition,
  TrainingPlan,
  TrainingPlanDto,
  Training,
  TrainingDescription,
  CompletedTraining,
  TrainingFeedback,
  DailyTrainingCompletion,
  User,
  BodyStatusVo2Max,
  BodyMetric,
  BodyMeasurement,
  ActivityMetrics
} from '../models/competition.model';
import { StravaStatus, StravaActivity } from '../models/strava.model';

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

  getTemplatePlans(): Observable<TrainingPlanDto[]> {
    return this.http.get<TrainingPlanDto[]>(`${this.baseUrl}/training-plans/templates`);
  }

  uploadAsTemplate(formData: FormData): Observable<TrainingPlanDto> {
    return this.http.post<TrainingPlanDto>(`${this.baseUrl}/training-plans/upload-template`, formData);
  }

  assignPlanToCompetition(planId: number, competitionId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/training-plans/assign`, null, {
      params: { planId: planId.toString(), competitionId: competitionId.toString() }
    });
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

  getCompletedTrainingsByDateRange(startDate: string, endDate: string): Observable<CompletedTraining[]> {
    return this.http.get<CompletedTraining[]>(`${this.baseUrl}/completed-trainings/by-date-range`, {
      params: { startDate, endDate }
    });
  }

  getActivityMetrics(completedTrainingId: number): Observable<ActivityMetrics> {
    return this.http.get<ActivityMetrics>(`${this.baseUrl}/completed-trainings/${completedTrainingId}/metrics`);
  }

  computeStravaMetrics(completedTrainingId: number): Observable<ActivityMetrics> {
    return this.http.post<ActivityMetrics>(`${this.baseUrl}/completed-trainings/${completedTrainingId}/compute-strava-metrics`, {});
  }

  // Strava API
  getStravaAuthUrl(): Observable<{url: string}> {
    return this.http.get<{url: string}>(`${this.baseUrl}/strava/auth-url`);
  }

  getStravaStatus(): Observable<StravaStatus> {
    return this.http.get<StravaStatus>(`${this.baseUrl}/strava/status`);
  }

  getStravaActivities(startDate: string, endDate: string): Observable<StravaActivity[]> {
    return this.http.get<StravaActivity[]>(`${this.baseUrl}/strava/activities`, {
      params: { startDate, endDate }
    });
  }

  disconnectStrava(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/strava/disconnect`);
  }

  getVo2MaxEstimate(distanceKm: number, movingTimeSeconds: number, sport: string): Observable<{ vo2max: number }> {
    return this.http.post<{ vo2max: number }>(`${this.baseUrl}/vo2max/estimate/training`, {
      distanceKm, movingTimeSeconds, sport
    });
  }

  getCurrentVo2Max(): Observable<BodyStatusVo2Max> {
    return this.http.get<BodyStatusVo2Max>(`${this.baseUrl}/vo2max/current`);
  }

  getBodyMetrics(): Observable<BodyMetric[]> {
    return this.http.get<BodyMetric[]>(`${this.baseUrl}/body-metrics/current`);
  }

  recalculateBodyMetrics(): Observable<{ message: string; activitiesProcessed: number }> {
    return this.http.post<{ message: string; activitiesProcessed: number }>(
      `${this.baseUrl}/body-metrics/recalculate`, {}
    );
  }

  // User API
  getMe(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/me`);
  }

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/users`);
  }

  updateUser(id: number, data: {
    username: string;
    email: string;
    firstName?: string;
    lastName?: string;
    dateOfBirth?: string;
    heightCm?: number | null;
    weightKg?: number | null;
    maxHeartRate?: number | null;
  }): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/users/${id}`, data);
  }

  // Body Measurement API
  getBodyMeasurements(): Observable<BodyMeasurement[]> {
    return this.http.get<BodyMeasurement[]>(`${this.baseUrl}/body-measurements`);
  }

  getLatestBodyMeasurement(): Observable<BodyMeasurement> {
    return this.http.get<BodyMeasurement>(`${this.baseUrl}/body-measurements/latest`);
  }

  createBodyMeasurement(measurement: BodyMeasurement): Observable<BodyMeasurement> {
    return this.http.post<BodyMeasurement>(`${this.baseUrl}/body-measurements`, measurement);
  }

  updateBodyMeasurement(id: number, measurement: BodyMeasurement): Observable<BodyMeasurement> {
    return this.http.put<BodyMeasurement>(`${this.baseUrl}/body-measurements/${id}`, measurement);
  }

  deleteBodyMeasurement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/body-measurements/${id}`);
  }
}
