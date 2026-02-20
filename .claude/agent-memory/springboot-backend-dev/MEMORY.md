# Spring Boot Backend Dev - Agent Memory

## Project Overview
- Package: `com.trainingsplan`
- Backend path: `C:\Users\bened\IdeaProjects\Smart_Trainingsplan\backend`
- Java 21, Spring Boot 3.2.0, MariaDB (H2 for tests)

## Entity Relationships
- Competition -> TrainingPlan -> TrainingWeek -> Training
- Training -> TrainingDescription (optional, rich text)
- CompletedTraining: standalone, matched to Training by date only (no FK)
- StravaToken: single-row table (singleton via findFirstByOrderByIdAsc)
- TrainingPlan.competition is nullable (isTemplate=true plans have no competition)

## Established Patterns
- DTOs: plain classes with getters/setters (not Records used here — team lead spec uses plain classes)
- Constructor injection throughout (no @Autowired on fields)
- RestClient (Spring 6) used for HTTP calls, not RestTemplate
- @CrossOrigin(origins = "http://localhost:4200") on controllers
- application.properties contains credentials and config (no application.yml)

## Key Files
- `entity/StravaToken.java` — OAuth token storage (single row)
- `repository/StravaTokenRepository.java` — findFirstByOrderByIdAsc() pattern
- `service/StravaService.java` — RestClient for Strava API, token refresh logic
- `controller/StravaController.java` — /api/strava/* endpoints, redirect on OAuth callback
- `dto/StravaStatusDto.java`, `dto/StravaActivityDto.java`

## API Conventions
- Base URL: `http://localhost:8080/api`
- ResponseEntity<T> wrapping on all controller responses
- Map.of("url", url) pattern for single-value responses
- @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) for LocalDate query params

## Strava OAuth Flow
- Auth URL: GET /api/strava/auth-url
- Callback: GET /api/strava/callback?code=... -> redirects to frontend /overview?strava=connected
- Status: GET /api/strava/status
- Activities: GET /api/strava/activities?startDate=&endDate=
- Disconnect: DELETE /api/strava/disconnect

## TrainingPlan Template Feature (added 2026-02)
- isTemplate=true: plan saved without competition; no Training records created yet
- POST /api/training-plans/upload-template — file, name, description (no competitionId)
- GET  /api/training-plans/templates — all templates
- POST /api/training-plans/assign?planId=&competitionId= — clones plan, shifts dates, creates Trainings
- Old-format JSON (has "trainings" array): dates shifted so last date == competition date
- Marathon/half-marathon format: dates computed from competition.getDate() inside service, no shifting needed
- TrainingPlanDto wraps entity to avoid Lazy-loading exceptions in controller responses

## Compilation
- `mvn compile -q` from backend/ directory (clean output = success)
