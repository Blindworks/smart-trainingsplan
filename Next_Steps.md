# PACR AI Roadmap

## Overview

PACR already provides:

* detailed training analytics (TRIMP, efficiency, fatigue, etc.)
* workout tracking
* training plan assignment
* performance analysis

The next step is transforming PACR from a **training tracker** into an **AI-assisted training system**.

The AI should not replace the existing metrics engine.
Instead it should operate as a **decision layer on top of deterministic training models**.

---

# System Architecture

```
User Data Layer
      ↓
Training Metrics Engine
      ↓
Training Simulation Engine
      ↓
AI Coaching Layer
```

Each layer has a clear responsibility.

---

# 1. User Data Layer

Already implemented in PACR.

Contains:

* workout history
* pace zones
* heart rate data
* TRIMP
* fatigue metrics
* efficiency metrics
* weekly load
* long run history
* training goals

Example state representation:

```json
AthleteState {
  fitness: 0.74
  fatigue: 0.61
  weekly_load: 54
  threshold_pace: "4:10/km"
  long_run_capacity: 28
}
```

---

# 2. Training Metrics Engine

Responsible for deterministic calculations.

This layer must remain **mathematical and deterministic**.

Examples:

* TRIMP
* Acute Load
* Chronic Load
* Fatigue
* Training Monotony
* Training Strain
* Efficiency score

Example output:

```
AthleteState
fitness: 0.74
fatigue: 0.63
injury_risk: low
weekly_load: 58
```

LLMs should **not compute these metrics**.

---

# 3. Training Simulation Engine

This layer simulates the **future impact of workouts**.

Goal: predict the effect of a planned workout or training week.

Example simulation:

Workout:

```
5 × 1000m intervals
Zone: Z4
```

Simulation output:

```
Expected TRIMP: 92
Fatigue increase: +0.12
Recovery time: 36h
```

Updated athlete state:

```
fatigue: 0.73
fitness: +0.02
```

---

## Weekly Simulation

Simulating a full training week:

```
Mon rest
Tue intervals
Wed recovery
Thu tempo
Sat long run
```

Simulation result:

```
Fatigue trajectory

Mon 0.60
Tue 0.74
Wed 0.68
Thu 0.81
Sat 0.93
```

Potential risk detection:

```
High fatigue spike detected
Overtraining risk
```

---

# 4. AI Coaching Layer

The AI interprets the deterministic simulation results.

The AI does **not calculate metrics** but evaluates strategy.

Input to AI:

```
AthleteState
SimulationResults
TrainingGoals
WeeksToRace
```

Example prompt:

```
You are a professional endurance running coach.

Athlete state:
fatigue: 0.63
fitness: 0.71
goal: marathon
weeks_to_race: 10

Simulation results:
fatigue_peak: 0.93

Evaluate the training week and suggest improvements.
```

Expected output:

```
Reduce tempo session intensity
Move long run to Sunday
Add recovery day
```

---

# 5. Adaptive Training Plan Generation

Replace static plans with dynamic plans.

Current system:

```
static_plan_id → workouts
```

Target system:

```
goal + athlete_state → generated_week_plan
```

Example output:

```
Mon rest
Tue 12km Z2
Wed intervals 6x800m
Thu recovery 8km
Fri rest
Sat tempo 10km Z3
Sun long run 26km
```

---

# 6. Micro Adjustments

Training plans should adapt to real behavior.

Example scenario:

Planned workout:

```
14km tempo
```

Actual:

```
Workout skipped
```

AI reaction:

```
Move tempo session to Friday
Reduce long run load
```

---

# 7. Explainable Training Decisions

PACR should explain training decisions to the user.

Example UI output:

```
Why this workout?

Your fatigue is elevated (0.67).
Therefore today's threshold session was replaced
with an aerobic endurance run.
```

This builds trust in the system.

---

# 8. AI Coach Chat (optional)

Natural language interface to training data.

User question:

```
Why is my efficiency dropping?
```

PACR response:

```
Efficiency dropped by 7% over the last 3 runs.

Possible causes:
- accumulated fatigue
- higher temperatures
- insufficient recovery
```

---

# 9. Mission Generator (Gamification)

AI-generated weekly challenges.

Example:

```
Mission: Hill Strength
Run 120m elevation gain this week.
```

Based on:

* training history
* terrain data
* current fatigue

---

# 10. Technical Stack

Backend:

```
Spring Boot
LangChain4j
OpenAI / Claude
```

Example service:

```
TrainingAIService
```

Methods:

```
generateWeekPlan()
adjustWorkout()
explainWorkout()
generateMission()
```

---

# 11. Long-Term Vision

PACR evolves from:

```
Training Tracker
```

to:

```
AI-Assisted Training System
```

Core capabilities:

* training impact prediction
* adaptive training plans
* fatigue forecasting
* race readiness estimation
* intelligent coaching feedback

---

# Key Principle

PACR AI should always follow this rule:

```
Deterministic calculations first
AI interpretation second
```

This ensures reliability, transparency, and scientific consistency.
