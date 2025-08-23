export interface Competition {
  id?: number;
  name: string;
  date?: string;
  location?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  // Legacy field for compatibility
  targetDate?: string;
}

export interface TrainingPlan {
  id?: number;
  competitionId: number;
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
  startTime?: string;
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
  rating: number;
  feedback: string;
  completed: boolean;
}

export interface DailyTrainingCompletion {
  date: string;
  planned: number;
  completed: number;
}