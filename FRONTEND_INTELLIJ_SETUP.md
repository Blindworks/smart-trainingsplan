# Frontend in IntelliJ IDEA einrichten

## Option 1: Frontend als separates Projekt öffnen (Empfohlen)

### Schritt 1: Neues Projekt öffnen
1. **File** → **Open** (oder **New** → **Project from Existing Sources**)
2. **Ordner auswählen**: `C:\Users\bened\IdeaProjects\Smart_Trainingsplan\frontend`
3. **"Open"** klicken
4. **IntelliJ erkennt automatisch**: package.json und konfiguriert Node.js

### Schritt 2: Node.js konfigurieren
1. **File** → **Settings** (Strg+Alt+S)
2. **Languages & Frameworks** → **Node.js**
3. **Node interpreter**: Automatisch erkannt (z.B. `C:\Program Files\nodejs\node.exe`)
4. **Package manager**: npm (automatisch erkannt)
5. **Apply** → **OK**

### Schritt 3: Dependencies installieren
**Terminal in IntelliJ öffnen** (Alt+F12):
```bash
npm install
```

### Schritt 4: Run Configuration erstellen
1. **Run** → **Edit Configurations**
2. **"+" klicken** → **npm**
3. **Configuration**:
   - **Name**: `React Start`
   - **Command**: `start`
   - **Scripts**: `start` (aus package.json)
4. **OK** klicken

### Schritt 5: Frontend starten
1. **Run Configuration** auswählen: "React Start"
2. **Grünen Play-Button** klicken ▶️
3. **Frontend öffnet automatisch**: http://localhost:3000

## Option 2: Als Multi-Module Projekt (Erweitert)

### Schritt 1: Root-Projekt öffnen
1. **File** → **Open**
2. **Root-Ordner auswählen**: `C:\Users\bened\IdeaProjects\Smart_Trainingsplan`
3. **IntelliJ erkennt**: Sowohl Maven (backend) als auch npm (frontend)

### Schritt 2: Module konfigurieren
1. **File** → **Project Structure** (Strg+Alt+Shift+S)
2. **Modules** → **"+" klicken** → **Import Module**
3. **Frontend-Ordner auswählen**: `frontend`
4. **"Create module from existing sources"** wählen
5. **OK** klicken

### Schritt 3: Run Configurations erstellen
**Backend:**
1. **"+" klicken** → **Spring Boot**
2. **Name**: `Backend`
3. **Main class**: `com.trainingsplan.SmartTrainingsplanApplication`
4. **Module**: smart-trainingsplan-backend

**Frontend:**
1. **"+" klicken** → **npm**
2. **Name**: `Frontend`
3. **Command**: `start`
4. **Working directory**: `$PROJECT_DIR$/frontend`

## Option 3: Terminal-basiert (Einfachste Methode)

### Im Backend-Projekt (bereits geöffnet):
1. **Terminal öffnen** (Alt+F12)
2. **Zum Frontend wechseln**:
   ```bash
   cd ../frontend
   ```
3. **Dependencies installieren** (nur einmal):
   ```bash
   npm install
   ```
4. **Frontend starten**:
   ```bash
   npm start
   ```

## Entwicklungs-Workflow

### Beide Anwendungen parallel laufen lassen:
1. **Backend starten** (Spring Boot Run Config)
2. **Frontend starten** (npm Run Config oder Terminal)
3. **URLs**:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api

### Debugging
**Frontend:**
- **Browser DevTools** (F12)
- **React Developer Tools** Browser-Extension

**Backend:**
- **IntelliJ Debugger** mit Breakpoints
- **Debug Mode** statt Run Mode

## Useful IntelliJ Plugins für React

1. **File** → **Settings** → **Plugins**
2. **Marketplace** durchsuchen:
   - **JavaScript and TypeScript**
   - **Node.js**
   - **React**
   - **Prettier** (Code-Formatierung)
   - **ESLint** (Code-Qualität)

## File Watcher Setup (Optional)

**Für automatische Code-Formatierung:**
1. **File** → **Settings** → **Tools** → **File Watchers**
2. **"+" klicken** → **Prettier**
3. **File type**: JavaScript, JSX
4. **Scope**: Project Files

## Quick Actions

### Beide Anwendungen starten:
1. **Backend**: ▶️ Spring Boot Config
2. **Terminal**: `cd ../frontend && npm start`

### Beide Anwendungen stoppen:
1. **Backend**: ⏹️ Stop Button
2. **Frontend**: Terminal Strg+C

## Fertig! 🎯

Jetzt haben Sie eine vollständige Entwicklungsumgebung in IntelliJ für Frontend und Backend!