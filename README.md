# Smart Trainingsplan

Eine intelligente Trainingsplan-Webapplikation für die optimale Vorbereitung auf Wettkämpfe.

## Features

- **Wettkampf-Management**: Erstellen und verwalten Sie Ihre Wettkämpfe mit Zieldatum
- **JSON-Upload**: Laden Sie Trainingspläne im JSON-Format hoch
- **Automatische Wochengenerierung**: Automatische Erstellung von Trainingswochen bis zum Wettkampf
- **Mischtraining**: Kombinieren Sie mehrere Trainingspläne für abwechslungsreiche Trainings
- **Feedback-System**: Dokumentieren Sie absolvierte Trainings und erhalten automatische Anpassungen
- **Dynamische Anpassung**: Bei verpassten Trainings wird der Plan automatisch angepasst

## Technologie-Stack

### Backend
- **Spring Boot 3.2.0**
- **Java 17**
- **MariaDB** als Datenbank
- **JPA/Hibernate** für ORM
- **REST API** für Frontend-Kommunikation

### Frontend
- **React 18**
- **React Bootstrap** für UI-Komponenten
- **React Router** für Navigation
- **Axios** für API-Kommunikation
- **React Datepicker** für Datumsauswahl
- **React Dropzone** für Datei-Upload

## Installation und Setup

### Voraussetzungen
- Java 17 oder höher
- Node.js 16 oder höher
- MariaDB Server
- Maven

### Backend Setup

1. **Datenbank konfigurieren**
```sql
CREATE DATABASE smart_trainingsplan;
CREATE USER 'trainingsplan_user'@'localhost' IDENTIFIED BY 'trainingsplan_password';
GRANT ALL PRIVILEGES ON smart_trainingsplan.* TO 'trainingsplan_user'@'localhost';
FLUSH PRIVILEGES;
```

2. **Backend starten**
```bash
cd backend
mvn spring-boot:run
```

Das Backend läuft auf http://localhost:8080

### Frontend Setup

1. **Dependencies installieren**
```bash
cd frontend
npm install
```

2. **Frontend starten**
```bash
npm start
```

Das Frontend läuft auf http://localhost:3000

## Verwendung

### 1. Wettkampf erstellen
- Navigieren Sie zu "Neuer Wettkampf"
- Geben Sie Name, Datum und optional eine Beschreibung ein
- Nach dem Erstellen werden automatisch Trainingswochen generiert

### 2. Trainingsplan hochladen
- Wählen Sie einen Wettkampf aus
- Klicken Sie auf "Plan hochladen"
- Laden Sie eine JSON-Datei mit folgendem Format hoch:

```json
{
  "trainings": [
    {
      "name": "Intervalltraining",
      "description": "5x1000m Intervalle mit 3min Pause",
      "date": "2024-01-15",
      "type": "speed",
      "intensity": "high",
      "startTime": "18:00",
      "duration": 90
    },
    {
      "name": "Grundlagenausdauer",
      "description": "Lockerer 10km Lauf",
      "date": "2024-01-17",
      "type": "endurance",
      "intensity": "low",
      "duration": 60
    }
  ]
}
```

### 3. Trainings verwalten
- Wählen Sie ein Datum aus
- Sehen Sie alle geplanten Trainings für den Tag
- Markieren Sie Trainings als absolviert/nicht absolviert
- Bei nicht absolvierten Trainings wird der Plan automatisch angepasst

### 4. Mischtraining
- Wählen Sie mehrere Trainingspläne aus
- Das System schlägt automatisch eine Kombination vor
- Perfekt für abwechslungsreiche Trainings

## API-Endpunkte

### Wettkämpfe
- `GET /api/competitions` - Alle Wettkämpfe
- `POST /api/competitions` - Neuen Wettkampf erstellen
- `PUT /api/competitions/{id}` - Wettkampf aktualisieren
- `DELETE /api/competitions/{id}` - Wettkampf löschen
- `POST /api/competitions/{id}/generate-weeks` - Trainingswochen generieren

### Trainingspläne
- `GET /api/training-plans` - Alle Trainingspläne
- `POST /api/training-plans/upload` - Trainingsplan hochladen
- `GET /api/training-plans/competition/{id}` - Pläne für Wettkampf

### Trainings
- `GET /api/trainings/competition/{id}/date/{date}` - Trainings für Datum
- `PUT /api/trainings/{id}/feedback` - Training-Feedback aktualisieren
- `GET /api/trainings/competition/{id}/mixed` - Mischtraining generieren

## Datenbankschema

### Haupttabellen
- **competitions** - Wettkampfinformationen
- **training_plans** - Hochgeladene Trainingspläne
- **training_weeks** - Generierte Trainingswochen
- **trainings** - Einzelne Trainingseinheiten

## Entwicklung

### Backend testen
```bash
cd backend
mvn test
```

### Frontend testen
```bash
cd frontend
npm test
```

### Production Build
```bash
cd frontend
npm run build
cd ../backend
mvn package
```

## Lizenz

Dieses Projekt ist für Demonstrationszwecke erstellt.