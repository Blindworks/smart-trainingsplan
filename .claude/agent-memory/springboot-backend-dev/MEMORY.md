# Spring Boot Backend Dev - Agent Memory

## Project Overview
- Package: `com.trainingsplan`
- Backend path: `C:\Users\bened\IdeaProjects\Smart_Trainingsplan\backend`
- Java 21, Spring Boot 3.2.0, MariaDB (H2 for tests)

## Entity Relationships (updated 2026-02)
- Competition has ManyToOne TrainingPlan (competition.training_plan_id FK, nullable)
- Competition has ManyToOne User (user_id FK, nullable, @JsonIgnore)
- Competition has OneToMany TrainingWeek
- TrainingWeek has OneToMany Training (via training_week_id FK)
- Training has ManyToOne TrainingPlan (training_plan_id FK — always points to the template plan)
- Training has ManyToOne TrainingDescription (optional, rich text)
- CompletedTraining: standalone, matched to Training by date only (no FK); has ManyToOne User (user_id FK, nullable, @JsonIgnore)
- StravaToken: single-row table (singleton via findFirstByOrderByIdAsc); ManyToOne User via user_id FK (nullable)
- User has OneToOne StravaToken (mappedBy="user", cascade ALL, optional)
- User implements UserDetails (Spring Security); password stored as passwordHash (@JsonIgnore)

## Architecture: All TrainingPlans are templates (2026-02 refactor)
- TrainingPlan has NO competition_id and NO is_template columns
- Competition holds the FK: competition.training_plan_id (nullable @ManyToOne @JsonIgnore)
- Training.training_plan_id still points to the plan (the template)
- assignPlanToCompetition: sets competition.trainingPlan = sourcePlan, no new plan created
- TrainingPlan has NO getTrainings() — use TrainingRepository.findByTrainingPlan_Id(planId)
- Liquibase 002: drops is_template + competition_id from training_plans, adds training_plan_id to competitions
- TrainingPlanRepository has only findByNameContainingIgnoreCase() (no competition/template queries)
- parseAndCreateTrainings() takes an explicit Competition parameter (not via trainingPlan.getCompetition())

## Established Patterns
- DTOs: plain classes with getters/setters (not Records — team lead spec uses plain classes); Auth DTOs are Records (AuthRequest, RegisterRequest, AuthResponse)
- @Autowired field injection in service classes (legacy pattern in this project)
- Constructor injection in controllers
- RestClient (Spring 6) used for HTTP calls, not RestTemplate
- @CrossOrigin(origins = "http://localhost:4200") on controllers (may be removed post-security)
- application.properties contains credentials and config (no application.yml)

## JWT / Spring Security (added 2026-02)
- SecurityConfig in config/ package: CSRF disabled, CORS from app.cors.allowed-origins, stateless sessions
- Public endpoints: /api/auth/**, /api/strava/callback, OPTIONS /**
- JWT: jjwt 0.12.6; secret in app.jwt.secret; expiry in app.jwt.expiration-ms (86400000 = 24h)
- security/ package: JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl, SecurityUtils
- SecurityUtils.getCurrentUser() returns User from SecurityContext; null if not authenticated
- CompetitionService.findAll() scoped by userId if authenticated, falls back to findAll() for tests
- CompetitionService.save() sets user if null (guards against re-saves)
- CompletedTrainingService sets user before save in uploadAndParseFitFile()
- Liquibase 005: adds password_hash to users, user_id to competitions and completed_trainings
- Auth endpoints: POST /api/auth/register, POST /api/auth/login
- UserController: GET /api/users/me (returns current authenticated user)

## Key Files
- `entity/TrainingPlan.java` — id, name, description, uploadDate, jsonContent, trainingCount only
- `entity/Competition.java` — ManyToOne TrainingPlan (training_plan_id FK)
- `entity/User.java` — id, username (unique), email (unique), createdAt, OneToOne StravaToken
- `entity/StravaToken.java` — OAuth token storage; ManyToOne User (user_id FK, nullable)
- `repository/UserRepository.java` — findByEmail(), findByUsername()
- `service/UserService.java` — createUser(username, email), findByEmail(), findAll(), findById()
- `controller/UserController.java` — POST/GET /api/users, GET /api/users/{id}; inline record CreateUserRequest
- `repository/TrainingRepository.java` — findByTrainingPlan_Id(), findByCompetitionId() via TrainingWeek join
- `repository/TrainingPlanRepository.java` — findByNameContainingIgnoreCase() only
- `repository/StravaTokenRepository.java` — findFirstByOrderByIdAsc()
- `service/TrainingPlanService.java` — parseAndCreateTrainings(plan, competition, json)
- `service/StravaService.java` — RestClient for Strava API, token refresh logic
- `controller/StravaController.java` — /api/strava/* endpoints, redirect on OAuth callback
- `dto/TrainingPlanDto.java` — id, name, description, trainingCount, uploadDate (no isTemplate/competitionId)

## API Conventions
- Base URL: `http://localhost:8080/api`
- ResponseEntity<T> wrapping on all controller responses
- Map.of("url", url) pattern for single-value responses
- @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) for LocalDate query params

## TrainingPlan API Endpoints
- GET  /api/training-plans — all plans
- GET  /api/training-plans/{id}
- GET  /api/training-plans/templates — all plans (findAllTemplates = findAll)
- POST /api/training-plans/upload — file, name, description, competitionId
- POST /api/training-plans/upload-template — file, name, description (no competition)
- POST /api/training-plans/assign?planId=&competitionId=
- PUT/DELETE /api/training-plans/{id}
- REMOVED: GET /api/training-plans/competition/{competitionId}

## Strava OAuth Flow
- Auth URL: GET /api/strava/auth-url
- Callback: GET /api/strava/callback?code=... -> redirects to frontend /overview?strava=connected
- Status: GET /api/strava/status
- Activities: GET /api/strava/activities?startDate=&endDate=
- Disconnect: DELETE /api/strava/disconnect

## JSON Plan Formats
- Old format (has "trainings" array): dates shifted so last date == competition date
- Marathon/half-marathon format: dates computed from competition.getDate(), no shifting needed
- TrainingPlanDto wraps entity to avoid Lazy-loading in controller responses

## ACWR / Load Model (added 2026-02)
- `entity/AcwrFlag.java` — enum: BLUE, GREEN, ORANGE, RED
- `DailyMetrics` now has: acute7, chronic28, acwr, acwrFlag (EnumType.STRING), acwrMessage
- `service/LoadModelService.java` — updateAcwr(user, date), recomputeAcwrForUser(user)
- acute7 = sum of daily_strain21 for 7 days ending on date; chronic28 = sum28 / 4
- ACWR flag thresholds: BLUE <0.8, GREEN 0.8–1.3 (inclusive), ORANGE 1.3–1.6 (inclusive), RED >1.6
- DailyMetricsService.updateDailyStrain() calls loadModelService.updateAcwr() then readinessService.compute() after save
- Liquibase 020: adds acute7, chronic28, acwr, acwr_flag (VARCHAR 10), acwr_message (VARCHAR 100)
- Test: `AcwrServiceTest.java` — 10 tests, JUnit5/Mockito/reflection injection, fixed TODAY=2026-02-24

## Readiness Proxy Model v1 (added 2026-02)
- `entity/Recommendation.java` — enum: EASY, MODERATE, HARD, REST
- `DailyMetrics` new fields: readinessScore (INT), recommendation (EnumType.STRING, length 10), reasonsJson (VARCHAR 500)
- `service/ReadinessService.java` — compute(user, date), recomputeForUser(user, 90 days)
- Heuristic: base 80, deductions: RED -35, ORANGE -20, yesterdayStrain>14 -15, decoupling>10% -10, >5% -5, z4z5>20min -5; clamped [0,100]
- Recommendation thresholds: <30=REST, 30-49=EASY, 50-69=MODERATE, >=70=HARD; RED flag caps at EASY
- ActivityMetricsRepository.sumZ4Z5MinByUserIdAndDateRange() — sums z4Min+z5Min for date range
- Liquibase 021: adds readiness_score (INT), recommendation (VARCHAR 10), reasons_json (VARCHAR 500)
- Endpoint: POST /api/daily-metrics/recompute-readiness (401 if not authenticated)
- DailyMetricsController uses securityUtils.getCurrentUser() (not userService) — follows existing pattern

## Coach Cards (added 2026-02)
- `service/CoachCardService.java` — pure stateless @Service, no DB; record CoachCard(title, bullets)
- `CoachCardService.generate(recommendation, acwrFlag, readinessScore, yesterdayStrain, lastDecouplingPct, z45Sum)`
- Title: REST→"Rest / Recovery", EASY→"Easy day recommended", MODERATE→"Moderate training day", HARD→"Hard training possible", null→"Training day"
- Bullets (max 3, priority order): RED acwr, REST+high strain>14, REST+z45>20, decoupling>10, score>=70+GREEN
- `DailyMetrics` new fields: coachTitle (VARCHAR 100), coachBulletsJson (VARCHAR 500)
- ReadinessService: @Autowired CoachCardService; generates+stores coach card in compute() before save
- Liquibase 022: adds coach_title, coach_bullets_json to daily_metrics
- Test: `CoachCardServiceTest.java` — 28 tests, no Spring context (service is stateless)

## Compilation
- `mvn compile -q -f /path/to/pom.xml` (clean output = success; use Unix paths in bash)
