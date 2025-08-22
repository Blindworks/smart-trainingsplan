# Smart Trainingsplan - Angular Frontend

Modernes Angular Frontend für die Smart Trainingsplan Anwendung mit Angular Material Design.

## Technische Details

- **Angular 19** mit Standalone Components
- **Angular Material 19.2** für UI-Komponenten
- **SCSS** für erweiterte Styling-Optionen
- **TypeScript** für Typsicherheit
- **RxJS** für reaktive Programmierung

## Entwicklung

```bash
# Installiere Abhängigkeiten
npm install

# Starte Entwicklungsserver
npm start
# oder
ng serve

# Anwendung läuft auf http://localhost:4200
```

## Features

### ✅ Implementierte Komponenten

1. **Navigation** - Material Toolbar mit Routing
2. **Competition Management** - CRUD-Operationen für Wettkämpfe
3. **Training Plan Upload** - JSON-Upload mit Drag & Drop
4. **Training Plan Overview** - Komplexe Kalender-Ansicht mit:
   - Wochennavigation mit Tastaturkürzel
   - Multi-Competition-Auswahl
   - Farbkodierte Trainingstypen und Intensitäten
   - Responsive Grid-Layout
5. **Training Completion** - Training-Tracking mit:
   - FIT-File Upload (Garmin/Polar/Suunto)
   - Bewertungssystem (1-5 Sterne)
   - Feedback-Kommentare
   - Fortschrittsanzeige

### 🎨 UI/UX Features

- **Responsive Design** für Desktop, Tablet & Mobile
- **Material Design** mit Azure Blue Theme
- **Tastaturkürzel** für effiziente Navigation
- **Drag & Drop** für Datei-Uploads
- **Loading States** und Error Handling
- **Deutsche Lokalisierung**

### 🔧 Architektur

- **Standalone Components** (neuester Angular-Standard)
- **Reactive Forms** mit Validierung
- **Observable-basierte** HTTP-Services
- **TypeScript Interfaces** für Type Safety
- **SCSS Modules** für komponenten-spezifische Styles

## API Integration

Das Frontend kommuniziert über eine Proxy-Konfiguration mit dem Spring Boot Backend:

- **Development Proxy**: `http://localhost:8080/api/*`
- **Type-Safe Models**: Vollständig typisierte API-Responses
- **Error Handling**: Zentrale Fehlerbehandlung mit Snackbars

## Vergleich: React vs Angular

| Feature | React Frontend | Angular Frontend |
|---------|---------------|------------------|
| Port | :3000 | :4200 |
| Framework | React 18 + Bootstrap | Angular 19 + Material |
| State Management | React State + Hooks | RxJS Observables |
| Styling | CSS + Bootstrap | SCSS + Material Theme |
| Forms | Controlled Components | Reactive Forms |
| Routing | React Router | Angular Router |
| Build Tool | Create React App | Angular CLI |

## Kommandos

```bash
# Entwicklung
npm start                 # Dev-Server starten
npm test                 # Tests ausführen
npm run build            # Production Build

# Angular CLI
ng generate component    # Neue Komponente
ng generate service      # Neuer Service
ng build --prod         # Production Build
```

## Projektstruktur

```
src/app/
├── components/          # UI-Komponenten
│   ├── navigation/      # Haupt-Navigation
│   ├── competition-*/   # Wettkampf-Management
│   ├── training-*/      # Training-Komponenten
│   └── ...
├── models/             # TypeScript Interfaces
├── services/           # API-Services
└── ...
```

Das Angular Frontend bietet eine moderne, typsichere Alternative zum React Frontend mit verbesserter UX durch Material Design und erweiterte Features wie Tastaturkürzel und Drag & Drop.