export interface Competition {
  id?: number;
  name: string;
  date?: string;
  type?: string;
  ranking?: string;
  location?: string;
  description?: string;
  trainingPlanId?: number;
  trainingPlanName?: string;
  createdAt?: string;
  updatedAt?: string;
  // Registration info (from backend DTO)
  registered?: boolean;
  registrationId?: number;
  targetTime?: string;
  registeredWithOrganizer?: boolean;
  // Legacy field for compatibility
  targetDate?: string;
}

export const COMPETITION_TYPES = [
  '5K', '10K', 'Halbmarathon', 'Marathon', '50K', '100K',
  'Backyard Ultra', 'Catcher car', 'Sonstige'
] as const;

export type CompetitionTypeValue = typeof COMPETITION_TYPES[number];

export interface TrainingPlan {
  id?: number;
  name: string;
  description?: string;
  targetTime?: string;
  prerequisites?: string;
  competitionType?: string;
  uploadDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Training {
  id?: number;
  name: string;
  trainingDescription?: TrainingDescription;
  trainingDate?: string;
  weekNumber?: number;
  dayOfWeek?: string;
  durationMinutes?: number;
  intensityLevel: 'high' | 'medium' | 'low' | 'recovery' | 'rest';
  trainingType: 'speed' | 'endurance' | 'strength' | 'race' | 'interval' | 'recovery' | 'swimming' | 'cycling' | 'general' | 'fartlek';
  isCompleted: boolean;
  completionStatus?: string;
  // Training plan information
  trainingPlanId?: number;
  trainingPlanName?: string;
  // Legacy fields for compatibility
  description?: string;
  date?: string;
  type?: string;
  intensity?: string;
  duration?: number;
  completed?: boolean;
  feedback?: string;
  rating?: number;
}

export interface UserTrainingEntry {
  id: number;
  trainingDate: string;          // "YYYY-MM-DD"
  weekNumber: number;
  completed: boolean;
  completionStatus?: string;
  training: Training;            // embedded Training template
  registrationId?: number;
  competitionId?: number;
}

export interface TrainingDescription {
  id?: number;
  name: string;
  detailedInstructions?: string;
  warmupInstructions?: string;
  cooldownInstructions?: string;
  equipment?: string;
  tips?: string;
  estimatedDurationMinutes?: number;
  difficultyLevel?: string;
}

export interface CompletedTraining {
  id?: number;
  trainingDate: string;
  uploadDate?: string;
  
  // Basic Training Metrics
  distanceKm?: number;
  durationSeconds?: number;
  movingTimeSeconds?: number;
  
  // Pace and Speed
  averagePaceSecondsPerKm?: number;
  averageSpeedKmh?: number;
  maxSpeedKmh?: number;
  
  // Heart Rate Data
  averageHeartRate?: number;
  maxHeartRate?: number;
  minHeartRate?: number;
  
  // Power Data
  averagePowerWatts?: number;
  maxPowerWatts?: number;
  normalizedPowerWatts?: number;
  
  // Cadence Data
  averageCadence?: number;
  maxCadence?: number;
  
  // Other Data
  calories?: number;
  elevationGainM?: number;
  elevationLossM?: number;
  temperatureCelsius?: number;
  
  // Sport Information
  sport?: string;
  subSport?: string;
  trainingType?: string;

  // Source Information
  source?: string;
  stravaActivityId?: number;
  activityName?: string;
  
  // File Information
  originalFilename?: string;
  
  // Device Information
  deviceManufacturer?: string;
  deviceProduct?: string;
  deviceSerialNumber?: string;
  softwareVersion?: string;
  
  // Legacy fields for compatibility
  duration?: number;
  distance?: number;
  averageSpeed?: number;
  maxSpeed?: number;
  fileName?: string;
  uploadedAt?: string;
}

export interface TrainingFeedback {
  rating?: number;
  feedback?: string;
  completed: boolean;
  completionStatus?: string;
}

export interface DailyTrainingCompletion {
  date: string;
  planned: number;
  completed: number;
}

export interface TrainingPlanDto {
  id: number;
  name: string;
  description?: string;
  trainingCount?: number;
  uploadDate?: string;
  targetTime?: string;
  prerequisites?: string;
  competitionType?: string;
}

export interface User {
  id?: number;
  username: string;
  email: string;
  role?: string;
  status?: 'EMAIL_VERIFICATION_PENDING' | 'ADMIN_APPROVAL_PENDING' | 'BLOCKED' | 'INACTIVE' | 'ACTIVE';
  createdAt?: string;
  firstName?: string;
  lastName?: string;
  dateOfBirth?: string;
  heightCm?: number;
  weightKg?: number;
  maxHeartRate?: number;
  hrRest?: number;
  gender?: string;
  profileImageFilename?: string;
}

export interface ProfileCompletion {
  complete: boolean;
  missingFields: string[];
  message: string;
}

export interface ActivityMetrics {
  id?: number;
  zonesUnknown: boolean;
  z1Min?: number;
  z2Min?: number;
  z3Min?: number;
  z4Min?: number;
  z5Min?: number;
  hrDataCoverage?: number;
  rawLoad?: number;
  strain21?: number;
  trimp?: number;
  trimpQuality?: 'LOW' | 'OK';
  decouplingPct?: number;
  decouplingEligible?: boolean;
  decouplingReason?: string;
  efficiencyFactor?: number;
}

export interface DailyMetrics {
  id?: number;
  date: string;
  dailyStrain21?: number;
  dailyTrimp?: number;
  ef7?: number;
  ef28?: number;
  acute7?: number;
  chronic28?: number;
  acwr?: number;
  acwrFlag?: 'BLUE' | 'GREEN' | 'ORANGE' | 'RED';
  acwrMessage?: string;
  readinessScore?: number;                              // 0–100
  recommendation?: 'EASY' | 'MODERATE' | 'HARD' | 'REST';
  reasonsJson?: string;                                 // JSON string like ["reason1","reason2"]
  coachTitle?: string;                                  // e.g. "Easy day recommended"
  coachBulletsJson?: string;                            // JSON string of max 3 bullet strings
}

export interface DecouplingHistoryPoint {
  date: string;
  activityName?: string;
  sport?: string;
  decouplingPct: number;
}

export interface ActivityComparisonItem {
  id: number;
  activityName?: string;
  sport?: string;
  trainingDate: string;
  distanceKm?: number;
  durationSeconds?: number;
  movingTimeSeconds?: number;
  averagePaceSecondsPerKm?: number;
  averageSpeedKmh?: number;
  averageHeartRate?: number;
  maxHeartRate?: number;
  averagePowerWatts?: number;
  normalizedPowerWatts?: number;
  averageCadence?: number;
  elevationGainM?: number;
  calories?: number;
  source?: string;
  z1Min?: number;
  z2Min?: number;
  z3Min?: number;
  z4Min?: number;
  z5Min?: number;
  strain21?: number;
  trimp?: number;
  efficiencyFactor?: number;
  decouplingPct?: number;
}

export interface BodyStatusVo2Max {
  vo2max: number | null;
  trainingDate?: string;
  activityName?: string;
}

export interface BodyMetric {
  metricType: string;
  label: string;
  value: number;
  unit: string;
  recordedAt?: string;
  sourceActivityId?: number;
}

export interface RaceDistancePrediction {
  distance: string;       // "1km" | "5km" | "10km" | "Halbmarathon" | "Marathon"
  baseTime: string;       // VDOT-Basisvorhersage, z.B. "3:45"
  adjustedTime: string;   // kontextbereinigt, z.B. "3:52"
  adjustmentPct: number;  // Aufschlag in %, z.B. 3
}

export interface CurrentRaceTimePredictions {
  vo2max: number;
  vo2maxDate?: string;
  avgWeeklyKm: number;
  maxLongRunKm: number;
  runsPerWeek: number;
  acwr?: number;
  readinessScore?: number;
  confidence: 'HOCH' | 'MITTEL' | 'NIEDRIG';
  predictions: RaceDistancePrediction[];
}

export interface Vo2MaxHistoryPoint {
  date: string;    // "YYYY-MM-DD"
  vo2max: number;  // ml/kg/min
  predictions: {
    '1km'?: string;
    '5km'?: string;
    '10km'?: string;
    'Halbmarathon'?: string;
    'Marathon'?: string;
    [key: string]: string | undefined;
  };
}

export interface BodyMeasurement {
  id?: number;
  measuredAt: string;        // "YYYY-MM-DD"
  weightKg?: number;         // Gewicht in kg
  fatPercentage?: number;    // Körperfettanteil %
  waterPercentage?: number;  // Wasseranteil %
  muscleMassKg?: number;     // Muskelmasse in kg
  boneMassKg?: number;       // Knochenmasse in kg
  visceralFatLevel?: number; // Viszeralfett-Level (Skala)
  metabolicAge?: number;     // Metabolisches Alter
  bmi?: number;              // BMI
  notes?: string;            // Notizen
}

export interface BloodPressure {
  id?: number;
  measuredAt: string;           // "YYYY-MM-DD"
  systolicPressure: number;     // Systolischer Druck (mmHg)
  diastolicPressure: number;    // Diastolischer Druck (mmHg)
  pulseAtMeasurement?: number;  // Puls bei Messung (bpm)
  notes?: string;
}

export interface SleepData {
  id?: number;
  recordedAt: string;              // "YYYY-MM-DD"
  sleepScore?: number;             // Schlaf-Score
  sleepScore7Days?: number;        // Sleep Score 7 Tage
  restingHeartRate?: number;       // Ruheherzfrequenz (bpm)
  bodyBattery?: number;            // Body Battery
  spO2?: number;                   // Pulsoximeter (%)
  breathingRate?: number;          // Atemfrequenz (Atemzüge/min)
  hrvStatus?: string;              // HFV-Status
  sleepQuality?: string;           // Qualität (z.B. "Gut", "Fair", "Schlecht")
  sleepDurationMinutes?: number;   // Schlafdauer in Minuten
  sleepNeedMinutes?: number;       // Schlafbedürfnis in Minuten
  bedtime?: string;                // Schlafenszeit "HH:mm"
  wakeTime?: string;               // Aufstehzeit "HH:mm"
}

export interface PaceZone {
  zone: number;                       // 1–5
  name: string;                       // z.B. "Schwelle"
  description: string;
  fastestPaceSecPerKm: number | null; // untere Grenze (schneller), null bei Zone 5
  slowestPaceSecPerKm: number | null; // obere Grenze (langsamer), null bei Zone 1
}

export interface PaceZones {
  referenceDistanceM: number | null;
  referenceTimeSeconds: number | null;
  referenceLabel: string | null;
  thresholdPaceSecPerKm: number;
  zones: PaceZone[];
}

export interface TrainingStatsBucket {
  label: string;
  startDate: string;
  endDate: string;
  distanceKm: number;
  durationSeconds: number;
  elevationGainM: number;
  activityCount: number;
}

export interface TrainingStatsDto {
  buckets: TrainingStatsBucket[];
  totalDistanceKm: number;
  totalDurationSeconds: number;
  totalActivityCount: number;
}
