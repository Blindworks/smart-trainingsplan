---
slug: pacr
name: PACR
full_name: PACR – Personal Adaptive Coach for Runners
tagline_de: Die adaptive Lauf-App aus Europa, die sich an Zyklus, Asthma und Tagesform anpasst.
tagline_en: The adaptive running app from Europe that adapts to your cycle, asthma and daily form.
year: 2026
status: Live
live_url: https://pacr.app
github_url: (privates Repository – auf Anfrage)
tech_stack:
  - Spring Boot 3 (Java 21)
  - Angular 19
  - MariaDB + Liquibase
  - LangChain4j / OpenAI
  - Garmin FIT SDK
tags:
  - Sports-Tech
  - AI/LLM
  - DSGVO
  - PWA
  - Made in Germany
---

# PACR – Personal Adaptive Coach for Runners

## Kurzbeschreibung

**DE:** PACR ist die erste europäische Lauf-App, die ihren Trainingsplan in Echtzeit an Tagesform, Menstruationszyklus und gesundheitliche Besonderheiten wie Asthma anpasst – DSGVO-konform und Made in Germany.

**EN:** PACR is the first European running app that adapts its training plan in real time to daily form, menstrual cycle and individual health factors like asthma – GDPR-compliant and made in Germany.

## Worum es geht

Die meisten Lauf-Apps tun so, als hätten Läufer:innen keinen Körper. Ein verpasster Tag und der ganze Plan ist Schrott; ein stressiger Arbeitstag und das Intervalltraining wird trotzdem in den roten Bereich geschoben; Zyklus, Asthma oder einfach ein schlechter Schlaf kommen schlicht nicht vor.

PACR macht es anders. Die App kombiniert klassische Trainingswissenschaft (VDOT, ACWR, Training Load, Readiness) mit einer adaptiven Engine, die jede neue Information – ein FIT-File von der Uhr, eine kurze Selbsteinschätzung, ein Zyklus-Update – sofort in die nächsten Einheiten einrechnet. Statt einem starren Plan bekommt jede:r Läufer:in einen Plan, der sich mitbewegt.

Dazu kommt eine Community ohne Leaderboard: echte Laufgruppen, gemeinsame Strecken, ein News Hub mit kuratierten Inhalten – aber kein Wettkampf um die schnellste Kilometerzeit.

## Zielgruppe

Hobby- und ambitionierte Hobbyläufer:innen in Deutschland und Europa zwischen 25 und 45 Jahren, die strukturiertes Training suchen, aber genug von US-zentrierten Apps haben, die ihren Körper ignorieren und ihre Gesundheitsdaten unkontrolliert exportieren.

## Kern-Features

- **Adaptive Engine** – jede Trainingseinheit, jedes Feedback und jede Zyklusphase verändert den Plan live.
- **Cycle Sync** – Intensitäten werden an die hormonellen Phasen angepasst (manuell oder via Schnittstelle).
- **Smart Calendar** – verpasste Einheiten werden intelligent kompensiert, statt den ganzen Plan zu kippen.
- **FIT-Datei-Import** – direkter Upload von Garmin, Coros & Co. via Garmin FIT SDK.
- **Trainings-Bibliothek** – Intervalle, Tempoläufe, Steigerungen als modulare Step-Blöcke (Nx-Wiederholungen).
- **VO2max & Readiness** – Daniels/VDOT-Berechnung, HF-korrigiert; Readiness-Score mit 4-Tages-Decay.
- **Community Routes** – GPX-Upload, Mini-Map-Karten, virtuelle Bot-Runner für realistische Vergleichswerte.
- **Run Clubs & Trainer Events** – echte Laufgruppen mit Trainer-Rolle, wiederkehrenden Events (RRULE) und Geocoding.
- **News Hub** – kuratierte Lauf-News (RSS-Ingestion) plus Friend-Feed mit Likes, Kommentaren und Trending.
- **Running Buddy** – Match-basiertes Finden von Trainingspartner:innen nach Pace, Distanz und Region.
- **Wetter-Widget** – Lauf-Wetterprognose für die hinterlegte Heimatadresse.
- **PRO-Subscription** – fairer Founder-Preis, 14 Tage kostenlos testen.

## Technik-Architektur

PACR ist ein klassisches, sauber geschnittenes Drei-Schichten-System:

**Backend (Spring Boot 3.2 / Java 21):**
- REST-API mit JWT-Auth (Spring Security), Ownership-Checks auf jedem User-Endpoint.
- JPA/Hibernate auf **MariaDB**, Schema-Migrationen ausschließlich über **Liquibase** (kein `ddl-auto`).
- AOP-basierte `@RequiresSubscription`-Annotation für PRO-Features.
- LLM-Integration über **LangChain4j + OpenAI** (Sprache wird im Prompt mitgegeben).
- Garmin **FIT SDK** für das Parsen von `.fit`-Workouts (HR, Pace, Power, Cadence, Elevation).
- Globaler `GlobalExceptionHandler` für generische, sichere Fehlermeldungen.
- DSGVO-konforme User-Löschung über `UserDeletionService` mit kaskadierender Bereinigung.

**Frontend (Angular 19):**
- Standalone Components, Angular Material, eigenes **KINETIC Design System** (CSS-Variablen + Mixins).
- Brand-Farbe **Electric Lime `#8ffc2e`** auf Charcoal-Hintergrund.
- Internationalisierung mit `@ngx-translate` (DE/EN), Material Symbols als Icons.
- Leaflet für interaktive Karten und Location-Picker.
- PWA-Auslieferung, Mobile-First.

**Infrastruktur:**
- Server in **Frankfurt**, Secrets via `.env`, eigene Prod-Properties ohne Defaults.
- Verschlüsselte Speicherung, keine Weitergabe von Nutzerdaten, vollständig DSGVO-konform.
- Security-Audit abgeschlossen am 05.04.2026 (IDOR-Fixes, Privilege-Escalation-Schutz, CORS-Hardening).

## Was PACR besonders macht

1. **Adaptiv, nicht starr.** Jeder Datenpunkt – Lauf, Feedback, Zyklus, Schlaf – beeinflusst das nächste Training. Sofort.
2. **Frauen mitgedacht.** Cycle Sync ist nicht aufgeklebtes Feature, sondern Teil der Trainingslogik.
3. **Europäisch.** Daten in Frankfurt, DSGVO statt „Privacy Policy". Kein Datenexport in die USA.
4. **Community statt Wettkampf.** Run Clubs, gemeinsame Strecken, News Hub – aber kein Leaderboard.
5. **Wissenschaftlich fundiert.** VDOT, ACWR, Training Load, Readiness – nicht aus dem Bauch, sondern aus der Sportwissenschaft.

## Tech-Stack-Badges (für die Karte)

`Spring Boot` · `Angular 19` · `MariaDB` · `LangChain4j`

## Links

- **Live:** [pacr.app](https://pacr.app)
- **Landing Page (DE):** [pacr.app](https://pacr.app)
- **Landing Page (EN):** [pacr.app/landing/en](https://pacr.app/landing/en/)
- **Wissens-Hub:** [pacr.app/wissen](https://pacr.app/wissen/)
- **Repository:** privat (auf Anfrage)

## Screenshots / Visuals (Vorschlag)

Für die Detailseite eignen sich folgende Screenshots aus der App:
1. **Dashboard** – Training Load Chart mit ACWR/Readiness.
2. **Smart Calendar** – Wochenansicht mit adaptiven Einheiten.
3. **Training Details** – Step-Blöcke mit Intervall-Wiederholungen.
4. **Cycle Sync** – Zyklusphasen-Übersicht mit angepassten Intensitäten.
5. **Community Routes** – Karte mit GPX-Tracks und Bot-Runner-Zeiten.
6. **News Hub** – Friend-Feed mit Likes & Kommentaren.

(Screenshots können aus `frontend/public/landing/` bzw. direkt aus der laufenden App gezogen werden.)

## Zeitstrahl

- **2024** – Erste Version als „Smart Trainingsplan" (Spring Boot + React).
- **2025** – Migration auf Angular 19, neues Datenmodell (Training-Templates + UserTrainingEntry).
- **Q1 2026** – Rebranding zu PACR, Cycle Sync, Community Routes, News Hub.
- **05.04.2026** – Security-Audit & Go-Live.
- **Heute** – Live unter [pacr.app](https://pacr.app), in aktiver Weiterentwicklung.

## Tonalität für die Detailseite

Warm, kompetent, Du-Anrede. PACR ist kein technokratisches Tool für Daten-Nerds, sondern ein digitaler Coach, der zuhört. Auf der Marketing-Seite arbeiten wir mit kurzen, klaren Sätzen, kontrastreichen Farben (Charcoal + Electric Lime) und der Schrift **Lexend**.
