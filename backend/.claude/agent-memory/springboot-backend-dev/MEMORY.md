# Smart Trainingsplan — Backend Agent Memory

## Critical Rules
- **Database migrations always with Liquibase** — never use `ddl-auto=update`
  - Path: `backend/src/main/resources/db/changelog/changes/NNN-description.xml`
  - Always include `<preConditions onFail="MARK_RAN"><not><columnExists .../></not></preConditions>`
  - Register in `db/changelog/db.changelog-master.xml` (append at end)
  - Current highest migration: 019

## Entity Relationships (confirmed)
- `ActivityMetrics` — OneToOne with `CompletedTraining` (lazy, unique FK `completed_training_id`)
- `DailyMetrics` — ManyToOne with `User`; unique constraint on `(user_id, date)`
- `CompletedTraining` — ManyToOne with `User` (lazy)

## Key Fields (as of migration 019)
- `ActivityMetrics`: zones (z1–z5 min), strain21, trimp, decouplingPct, **efficiencyFactor** (DOUBLE NULL)
- `DailyMetrics`: dailyStrain21, dailyTrimp, **ef7**, **ef28** (DOUBLE NULL rolling EF averages)

## Service Layer Patterns
- `ActivityMetricsService`: two overloads of `calculateAndPersist` — 4-param (FIT files, no decoupling) and 6-param (Strava, with decoupling). Both call `dailyMetricsService.updateDailyStrain()` and `dailyMetricsService.updateDailyEf()` at the end.
- `DailyMetricsService.updateDailyEf(user, date)`: upserts ef7 + ef28; queries `findWithEfByUserIdAndDateRange` (28-day window), then filters in Java for 7-day subwindow.
- `DailyMetricsService.recomputeEfForUser(user)`: iterates today-89..today calling `updateDailyEf`.
- `BodyMetricService.recalculateForCurrentUser()`: after VO2max loop, also calls `dailyMetricsService.recomputeEfForUser(user)`.

## Test Patterns
- Pure unit tests (no Spring context) in `src/test/java/com/trainingsplan/service/`
- Use `mock(Repo.class)` + reflection-based field injection (no `@InjectMocks` — service uses `@Autowired` fields)
- `spring-boot-starter-test` provides JUnit 5 + Mockito

## API Endpoints
- `POST /api/daily-metrics/recompute-ef` — recomputes 90-day EF rolling averages for current user
- `POST /api/body-metrics/recalculate` — recalculates VO2max + EF for current user

## See Also
- `patterns.md` (not yet created — add if recurring patterns emerge)
