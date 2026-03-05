import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Competition,
  TrainingPlan,
  TrainingPlanDto,
  Training,
  UserTrainingEntry,
  TrainingDescription,
  CompletedTraining,
  TrainingFeedback,
  DailyTrainingCompletion,
  User,
  ProfileCompletion,
  BodyStatusVo2Max,
  BodyMetric,
  Vo2MaxHistoryPoint,
  CurrentRaceTimePredictions,
  BodyMeasurement,
  BloodPressure,
  SleepData,
  ActivityMetrics,
  DailyMetrics,
  DecouplingHistoryPoint,
  PaceZones,
  ActivityComparisonItem,
  TrainingStatsDto,
  TrainingImpactRequest,
  TrainingImpactResponse
} from '../models/competition.model';
import { StravaStatus, StravaActivity } from '../models/strava.model';
import { DashboardDto } from '../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  // Competition API
  getCompetitionTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/competitions/types`);
  }

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

  registerForCompetition(id: number, data?: { targetTime?: string; registeredWithOrganizer?: boolean; ranking?: string }): Observable<any> {
    return this.http.post(`${this.baseUrl}/competitions/${id}/register`, data ?? {});
  }

  unregisterFromCompetition(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/competitions/${id}/register`);
  }

  updateCompetitionRegistration(id: number, data: { ranking?: string; targetTime?: string; registeredWithOrganizer?: boolean }): Observable<any> {
    return this.http.put(`${this.baseUrl}/competitions/${id}/register`, data);
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

  patchTrainingPlanMetadata(id: number, updates: { name?: string; description?: string; targetTime?: string; prerequisites?: string; competitionType?: string }): Observable<TrainingPlan> {
    return this.http.patch<TrainingPlan>(`${this.baseUrl}/training-plans/${id}/metadata`, updates);
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

  getTrainingsByPlan(planId: number): Observable<Training[]> {
    return this.http.get<Training[]>(`${this.baseUrl}/trainings/plan/${planId}`);
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

  createTraining(training: Training, planId?: number): Observable<Training> {
    const params: Record<string, string> = {};
    if (planId != null) params['planId'] = planId.toString();
    return this.http.post<Training>(`${this.baseUrl}/trainings`, training, { params });
  }

  updateTraining(id: number, training: Training): Observable<Training> {
    return this.http.put<Training>(`${this.baseUrl}/trainings/${id}`, training);
  }

  updateTrainingFeedback(id: number, feedback: TrainingFeedback): Observable<Training> {
    return this.http.put<Training>(`${this.baseUrl}/trainings/${id}/feedback`, feedback);
  }

  getTrainingImpact(request: TrainingImpactRequest): Observable<TrainingImpactResponse> {
    return this.http.post<TrainingImpactResponse>(`${this.baseUrl}/training/impact`, request);
  }

  deleteTraining(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/trainings/${id}`);
  }

  // User Training Entries API
  getUserTrainingEntries(registrationId: number): Observable<UserTrainingEntry[]> {
    return this.http.get<UserTrainingEntry[]>(`${this.baseUrl}/user-training-entries`, {
      params: { registrationId: registrationId.toString() }
    });
  }

  getUserCalendarEntries(from: string, to: string): Observable<UserTrainingEntry[]> {
    return this.http.get<UserTrainingEntry[]>(`${this.baseUrl}/user-training-entries/calendar`, {
      params: { from, to }
    });
  }

  updateTrainingEntryFeedback(id: number, feedback: TrainingFeedback): Observable<UserTrainingEntry> {
    return this.http.put<UserTrainingEntry>(`${this.baseUrl}/user-training-entries/${id}/feedback`, feedback);
  }

  generateUserSchedule(registrationId: number): Observable<UserTrainingEntry[]> {
    return this.http.post<UserTrainingEntry[]>(`${this.baseUrl}/user-training-entries/generate`, null, {
      params: { registrationId: registrationId.toString() }
    });
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

  getCompletedTrainingTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/completed-trainings/training-types`);
  }

  updateCompletedTrainingType(id: number, trainingType: string | null): Observable<CompletedTraining> {
    return this.http.patch<CompletedTraining>(`${this.baseUrl}/completed-trainings/${id}/training-type`, { trainingType });
  }

  getDailyMetrics(startDate: string, endDate: string): Observable<DailyMetrics[]> {
    return this.http.get<DailyMetrics[]>(`${this.baseUrl}/daily-metrics`, {
      params: { startDate, endDate }
    });
  }

  getDecouplingHistory(startDate: string, endDate: string): Observable<DecouplingHistoryPoint[]> {
    return this.http.get<DecouplingHistoryPoint[]>(
      `${this.baseUrl}/completed-trainings/decoupling-history`,
      { params: { startDate, endDate } }
    );
  }

  compareActivities(ids: number[]): Observable<ActivityComparisonItem[]> {
    let params = new HttpParams();
    ids.forEach(id => { params = params.append('ids', id.toString()); });
    return this.http.get<ActivityComparisonItem[]>(`${this.baseUrl}/completed-trainings/compare`, { params });
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

  getVo2MaxEstimate(distanceKm: number, movingTimeSeconds: number, sport: string, avgHeartRate?: number | null): Observable<{ vo2max: number; vo2maxHRCorrected?: number | null }> {
    return this.http.post<{ vo2max: number; vo2maxHRCorrected?: number | null }>(`${this.baseUrl}/vo2max/estimate/training`, {
      distanceKm, movingTimeSeconds, sport, avgHeartRate: avgHeartRate ?? null
    });
  }

  getCurrentVo2Max(): Observable<BodyStatusVo2Max> {
    return this.http.get<BodyStatusVo2Max>(`${this.baseUrl}/vo2max/current`);
  }

  getBodyMetrics(): Observable<BodyMetric[]> {
    return this.http.get<BodyMetric[]>(`${this.baseUrl}/body-metrics/current`);
  }

  getCurrentRacePredictions(): Observable<CurrentRaceTimePredictions | null> {
    return this.http.get<CurrentRaceTimePredictions>(`${this.baseUrl}/body-metrics/race-predictions-current`);
  }

  getVo2MaxHistory(): Observable<Vo2MaxHistoryPoint[]> {
    return this.http.get<Vo2MaxHistoryPoint[]>(`${this.baseUrl}/body-metrics/vo2max-history`);
  }

  recalculateBodyMetrics(): Observable<{ message: string; activitiesProcessed: number }> {
    return this.http.post<{ message: string; activitiesProcessed: number }>(
      `${this.baseUrl}/body-metrics/recalculate`, {}
    );
  }

  recomputeReadiness(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/daily-metrics/recompute-readiness`, {});
  }

  computeToday(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/daily-metrics/compute-today`, {});
  }

  getDashboard(): Observable<DashboardDto> {
    return this.http.get<DashboardDto>(`${this.baseUrl}/dashboard`);
  }

  // User API
  getMe(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/me`);
  }

  getMyProfileCompletion(): Observable<ProfileCompletion> {
    return this.http.get<ProfileCompletion>(`${this.baseUrl}/users/me/profile-completion`);
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
    hrRest?: number | null;
    gender?: string | null;
    status?: 'EMAIL_VERIFICATION_PENDING' | 'ADMIN_APPROVAL_PENDING' | 'BLOCKED' | 'INACTIVE' | 'ACTIVE';
  }): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/users/${id}`, data);
  }

  uploadProfileImage(id: number, file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${this.baseUrl}/users/${id}/profile-image`, formData);
  }

  getProfileImage(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/users/${id}/profile-image`, { responseType: 'blob' });
  }

  getPaceZones(): Observable<PaceZones> {
    return this.http.get<PaceZones>(`${this.baseUrl}/users/me/pace-zones`);
  }

  setPaceZoneReference(referenceDistanceM: number, referenceTimeSeconds: number, referenceLabel: string): Observable<PaceZones> {
    return this.http.put<PaceZones>(`${this.baseUrl}/users/me/pace-zones`, {
      referenceDistanceM,
      referenceTimeSeconds,
      referenceLabel
    });
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

  // Blood Pressure API
  getBloodPressures(): Observable<BloodPressure[]> {
    return this.http.get<BloodPressure[]>(`${this.baseUrl}/blood-pressure`);
  }

  getLatestBloodPressure(): Observable<BloodPressure> {
    return this.http.get<BloodPressure>(`${this.baseUrl}/blood-pressure/latest`);
  }

  createBloodPressure(bp: BloodPressure): Observable<BloodPressure> {
    return this.http.post<BloodPressure>(`${this.baseUrl}/blood-pressure`, bp);
  }

  updateBloodPressure(id: number, bp: BloodPressure): Observable<BloodPressure> {
    return this.http.put<BloodPressure>(`${this.baseUrl}/blood-pressure/${id}`, bp);
  }

  deleteBloodPressure(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/blood-pressure/${id}`);
  }

  // Sleep Data API
  getSleepData(): Observable<SleepData[]> {
    return this.http.get<SleepData[]>(`${this.baseUrl}/sleep-data`);
  }

  getLatestSleepData(): Observable<SleepData> {
    return this.http.get<SleepData>(`${this.baseUrl}/sleep-data/latest`);
  }

  createSleepData(sleepData: SleepData): Observable<SleepData> {
    return this.http.post<SleepData>(`${this.baseUrl}/sleep-data`, sleepData);
  }

  updateSleepData(id: number, sleepData: SleepData): Observable<SleepData> {
    return this.http.put<SleepData>(`${this.baseUrl}/sleep-data/${id}`, sleepData);
  }

  deleteSleepData(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sleep-data/${id}`);
  }

  importSleepCsvFromGarmin(file: File): Observable<{ imported: number; skipped: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ imported: number; skipped: number }>(`${this.baseUrl}/sleep-data/import`, formData);
  }

  // Training Statistics API
  getTrainingStats(period: string, trainingType?: string, sport?: string): Observable<TrainingStatsDto> {
    let params = new HttpParams().set('period', period);
    if (trainingType) params = params.set('trainingType', trainingType);
    if (sport) params = params.set('sport', sport);
    return this.http.get<TrainingStatsDto>(`${this.baseUrl}/completed-trainings/stats`, { params });
  }

  getTrainingTypesUsed(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/completed-trainings/training-types-used`);
  }

  getSportsUsed(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/completed-trainings/sports-used`);
  }
}
