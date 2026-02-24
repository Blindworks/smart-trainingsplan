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
  // Legacy field for compatibility
  targetDate?: string;
}

export interface TrainingPlan {
  id?: number;
  name: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Training {
  id?: number;
  name: string;
  trainingDescription?: TrainingDescription;
  trainingDate: string;
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
}

export interface User {
  id?: number;
  username: string;
  email: string;
  createdAt?: string;
  firstName?: string;
  lastName?: string;
  dateOfBirth?: string;
  heightCm?: number;
  weightKg?: number;
  maxHeartRate?: number;
  hrRest?: number;
  gender?: string;
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
