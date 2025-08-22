# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Backend (Spring Boot + Maven)
```bash
# Navigate to backend directory
cd backend

# Run the application in development mode
mvn spring-boot:run

# Run tests
mvn test

# Package for production
mvn package
```

The backend runs on `http://localhost:8080` and uses MariaDB as the primary database with H2 for testing.

### Frontend (React)
```bash
# Navigate to frontend directory  
cd frontend

# Install dependencies
npm install

# Start development server
npm start

# Run tests
npm test

# Build for production
npm run build
```

The React frontend runs on `http://localhost:3000` and proxies API requests to the backend on port 8080.

### Angular Frontend (Alternative)
```bash
# Navigate to Angular frontend directory
cd angular-frontend

# Install dependencies
npm install

# Start development server
npm start

# Run tests
npm test

# Build for production
npm run build
```

The Angular frontend runs on `http://localhost:4200` and proxies API requests to the backend on port 8080.

### Database Setup
Create MariaDB database and user:
```sql
CREATE DATABASE smart_trainingsplan;
CREATE USER 'smart_trainingsplan'@'localhost' IDENTIFIED BY 'taxcRH51#';
GRANT ALL PRIVILEGES ON smart_trainingsplan.* TO 'smart_trainingsplan'@'localhost';
FLUSH PRIVILEGES;
```

## Architecture Overview

### Backend (Spring Boot)
- **Technology**: Java 21, Spring Boot 3.2.0, JPA/Hibernate, MariaDB
- **Package Structure**: `com.trainingsplan` with clear separation of concerns
  - `controller/` - REST endpoints for API layer
  - `entity/` - JPA entities for data model
  - `repository/` - Data access layer
  - `service/` - Business logic layer  
  - `dto/` - Data transfer objects
- **Key Features**:
  - CORS configured for frontend integration
  - Garmin FIT file processing with external SDK
  - Automatic training plan generation and management
  - Training completion tracking with daily/weekly progress

### Frontend Options

#### React Frontend (Original)
- **Technology**: React 18, React Bootstrap, React Router, Axios
- **Port**: `http://localhost:3000`
- **Key Components**:
  - `TrainingPlanOverview.js` - Main unified calendar/planning view
  - `CompetitionList.js` & `CompetitionForm.js` - Competition management
  - `TrainingPlanUpload.js` - JSON training plan upload
  - `TrainingCompletion.js` - Training feedback and completion tracking
- **API Integration**: Centralized in `services/api.js` with axios

#### Angular Frontend (Alternative)
- **Technology**: Angular 19, Angular Material, TypeScript, RxJS
- **Port**: `http://localhost:4200`
- **Architecture**: Standalone Components with Reactive Forms
- **Key Features**:
  - Material Design UI with Azure Blue theme
  - Type-safe API services with full TypeScript support
  - Advanced UX features (keyboard shortcuts, drag & drop)
  - Responsive design for all screen sizes
  - Professional training calendar with multi-competition support
- **Key Components**:
  - `TrainingPlanOverviewComponent` - Advanced calendar with keyboard navigation
  - `CompetitionListComponent` & `CompetitionFormComponent` - Material Design forms
  - `TrainingPlanUploadComponent` - Drag & drop JSON upload with documentation
  - `TrainingCompletionComponent` - FIT file upload with star ratings
- **API Integration**: Observable-based services in `services/api.service.ts`

### Core Business Logic
1. **Competition Management**: Create competitions with target dates
2. **Training Plan Upload**: JSON format upload with automatic week generation  
3. **Unified Training View**: Multi-competition comparison with week-by-week navigation
4. **FIT File Integration**: Upload and parse Garmin/Polar/Suunto .fit files for actual training data
5. **Training Completion Tracking**: Mark trainings as completed with automatic plan adjustments

### Database Schema
Key entities with JPA relationships:
- `Competition` → `TrainingPlan` → `Training` (one-to-many chains)
- `TrainingDescription` - detailed training instructions and metadata
- `CompletedTraining` - FIT file derived actual training data
- `TrainingWeek` - generated weekly training periods

### JSON Training Plan Format
Training plans are uploaded in JSON format:
```json
{
  "trainings": [
    {
      "name": "Intervalltraining",
      "description": "5x1000m Intervalle mit 3min Pause",
      "date": "2024-01-15",
      "type": "speed|endurance|strength|race|fartlek|recovery|swimming|cycling|general",
      "intensity": "high|medium|low|recovery|rest",
      "startTime": "18:00",
      "duration": 90
    }
  ]
}
```

## API Endpoints Structure
- `/api/competitions` - Competition CRUD operations
- `/api/training-plans` - Plan upload and management
- `/api/trainings` - Individual training management and completion
- `/api/training-descriptions` - Detailed training instruction management
- `/api/completed-trainings` - FIT file upload and actual training data

## Development Notes
- Backend uses Java 21 features and Spring Boot 3.x patterns
- Frontend follows React functional component patterns with hooks
- Database credentials are in `application.properties` (dev) and `application-dev.properties`
- CORS is configured for localhost development
- The app supports multi-competition training plan comparison and mixing
- FIT file processing requires the Garmin FIT SDK dependency