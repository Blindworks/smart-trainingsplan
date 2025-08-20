# IntelliJ IDEA Setup - Smart Trainingsplan

## Schritt 1: Projekt in IntelliJ öffnen

1. **IntelliJ IDEA starten**
2. **"Open"** klicken
3. **Ordner auswählen**: `C:\Users\bened\IdeaProjects\Smart_Trainingsplan\backend`
4. **"Open as Maven Project"** bestätigen
5. **"Import Maven project automatically"** aktivieren

## Schritt 2: Java SDK konfigurieren

1. **File** → **Project Structure** (Strg+Alt+Shift+S)
2. **Project Settings** → **Project**
3. **Project SDK**: Java 21 auswählen (falls nicht vorhanden, "Add SDK" klicken)
4. **Project language level**: 21 auswählen
5. **OK** klicken

## Schritt 3: Maven Konfiguration prüfen

1. **File** → **Settings** (Strg+Alt+S)
2. **Build, Execution, Deployment** → **Build Tools** → **Maven**
3. **Maven home path** sollte automatisch erkannt werden
4. **User settings file** und **Local repository** prüfen
5. **Apply** → **OK**

## Schritt 4: Dependencies laden

1. **Maven-Tab** öffnen (rechte Seite)
2. **Reload Maven Projects** klicken (🔄 Symbol)
3. Warten bis alle Dependencies heruntergeladen sind

## Schritt 5: Datenbank konfigurieren

### MariaDB/MySQL Connection
1. **Database-Tab** öffnen (rechte Seite)
2. **"+" klicken** → **Data Source** → **MariaDB**
3. **Connection Details**:
   - Host: `localhost`
   - Port: `3306`
   - Database: `smart_trainingsplan`
   - User: `trainingsplan_user`
   - Password: `trainingsplan_password`
4. **Test Connection** klicken
5. **OK** klicken

### SQL Script ausführen
```sql
CREATE DATABASE smart_trainingsplan;
CREATE USER 'trainingsplan_user'@'localhost' IDENTIFIED BY 'trainingsplan_password';
GRANT ALL PRIVILEGES ON smart_trainingsplan.* TO 'trainingsplan_user'@'localhost';
FLUSH PRIVILEGES;
```

## Schritt 6: Run Configuration erstellen

1. **Run** → **Edit Configurations**
2. **"+" klicken** → **Spring Boot**
3. **Configuration**:
   - **Name**: `Smart Trainingsplan Backend`
   - **Main class**: `com.trainingsplan.SmartTrainingsplanApplication`
   - **Working directory**: `$MODULE_WORKING_DIR$`
   - **Environment variables**: (leer lassen)
4. **OK** klicken

## Schritt 7: Anwendung starten

### Backend starten
1. **Run Configuration auswählen** (Dropdown oben)
2. **Grünen Play-Button** klicken ▶️
3. **Warten** bis "Started SmartTrainingsplanApplication" erscheint
4. **Backend läuft** auf http://localhost:8080

### Frontend starten
1. **Terminal öffnen** (Alt+F12)
2. **Zum Frontend wechseln**:
   ```bash
   cd ../frontend
   ```
3. **Dependencies installieren** (nur beim ersten Mal):
   ```bash
   npm install
   ```
4. **Frontend starten**:
   ```bash
   npm start
   ```
5. **Frontend öffnet** automatisch auf http://localhost:3000

## Schritt 8: Testen

1. **Frontend**: http://localhost:3000
2. **Neuen Wettkampf erstellen**
3. **Trainingsplan hochladen** (`example_training_plan.json`)
4. **Trainings anzeigen** und testen

## Troubleshooting

### Maven Dependencies nicht geladen
- **Maven** → **Reload Projects** klicken
- **File** → **Invalidate Caches and Restart**

### Port 8080 bereits belegt
- **Terminal**: `netstat -ano | findstr :8080`
- **Process beenden**: Task Manager verwenden

### Datenbank Connection Fehler
- **MariaDB/MySQL** Service starten
- **Credentials** in `application.properties` prüfen

## Fertig! 🎯

Nach diesem Setup können Sie die Anwendung vollständig in IntelliJ entwickeln und ausführen.