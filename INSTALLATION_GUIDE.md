# Smart Trainingsplan - Installationsanleitung

## Option 1: Mit IntelliJ IDEA (Empfohlen)

### Backend Setup
1. **IntelliJ IDEA öffnen**
2. **"Open or Import"** wählen
3. **Ordner auswählen**: `C:\Users\bened\IdeaProjects\Smart_Trainingsplan\backend`
4. **"Open as Gradle Project"** wählen
5. Warten bis IntelliJ alle Dependencies herunterlädt
6. **Run Configuration erstellen**:
   - Gehe zu "Run" → "Edit Configurations"
   - Klicke "+", wähle "Spring Boot"
   - Name: "Smart Trainingsplan Backend"
   - Main class: `com.trainingsplan.SmartTrainingsplanApplication`
   - Working directory: `C:\Users\bened\IdeaProjects\Smart_Trainingsplan\backend`
   - Click "OK"
7. **Starten**: Grünen Play-Button klicken

### Frontend Setup
1. **Neues Terminal in IntelliJ** (Alt+F12)
2. **Zum Frontend wechseln**:
   ```bash
   cd ../frontend
   ```
3. **Dependencies installieren**:
   ```bash
   npm install
   ```
4. **Frontend starten**:
   ```bash
   npm start
   ```

## Option 2: Mit VS Code

### Backend Setup
1. **VS Code öffnen**
2. **Extensions installieren**:
   - "Extension Pack for Java" (Microsoft)
   - "Spring Boot Extension Pack" (VMware)
3. **Ordner öffnen**: `Smart_Trainingsplan`
4. **Gradle Wrapper verwenden**:
   ```bash
   cd backend
   ./gradlew bootRun
   ```

### Frontend Setup
1. **Neues Terminal** (Strg+Shift+`)
2. **Zum Frontend wechseln**:
   ```bash
   cd frontend
   npm install
   npm start
   ```

## Option 3: Kommandozeile

### Backend
```bash
cd C:\Users\bened\IdeaProjects\Smart_Trainingsplan\backend
./gradlew bootRun
```

### Frontend
```bash
cd C:\Users\bened\IdeaProjects\Smart_Trainingsplan\frontend
npm install
npm start
```

## MariaDB Setup

### Installation
1. **MariaDB herunterladen**: https://mariadb.org/download/
2. **Installieren** mit Standard-Einstellungen
3. **Root-Passwort setzen** (z.B. "admin")

### Datenbank erstellen
1. **MySQL/MariaDB Command Line öffnen**
2. **Folgenden SQL ausführen**:
```sql
CREATE DATABASE smart_trainingsplan;
CREATE USER 'trainingsplan_user'@'localhost' IDENTIFIED BY 'trainingsplan_password';
GRANT ALL PRIVILEGES ON smart_trainingsplan.* TO 'trainingsplan_user'@'localhost';
FLUSH PRIVILEGES;
```

### Alternative: XAMPP verwenden
1. **XAMPP installieren**: https://www.apachefriends.org/
2. **MySQL starten** über XAMPP Control Panel
3. **phpMyAdmin öffnen** (http://localhost/phpmyadmin)
4. **Datenbank "smart_trainingsplan" erstellen**

## URLs nach dem Start

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Database**: localhost:3306

## Troubleshooting

### "Port 8080 already in use"
```bash
# Windows: Port freigeben
netstat -ano | findstr :8080
taskkill /PID [PID-NUMMER] /F
```

### "npm command not found"
- **Node.js installieren**: https://nodejs.org/
- **Neu starten** nach Installation

### MariaDB Verbindungsfehler
- **application.properties anpassen**:
```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/smart_trainingsplan
spring.datasource.username=root
spring.datasource.password=DEIN_MYSQL_PASSWORT
```

## Erste Schritte

1. **Wettkampf erstellen** (Name + Datum in der Zukunft)
2. **Trainingsplan hochladen** (`example_training_plan.json` verwenden)
3. **Trainings anzeigen** und Feedback geben

Das war's! Die Applikation ist jetzt bereit. 🎯