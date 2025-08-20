# Maven Installation - Schritt für Schritt

## Methode 1: Manuelle Installation

### Schritt 1: Download
1. **Öffnen Sie**: https://maven.apache.org/download.cgi
2. **Suchen Sie**: "Binary zip archive" 
3. **Klicken Sie**: `apache-maven-3.9.6-bin.zip`

### Schritt 2: Installation
1. **Extrahieren nach**: `C:\apache-maven`
2. **Ordnerstruktur sollte sein**: `C:\apache-maven\bin\mvn.cmd`

### Schritt 3: Umgebungsvariablen (WICHTIG!)
**Als Administrator Command Prompt öffnen**:
```cmd
setx MAVEN_HOME "C:\apache-maven" /M
setx PATH "%PATH%;C:\apache-maven\bin" /M
```

### Schritt 4: Testen
**NEUES Command Prompt öffnen**:
```cmd
mvn --version
```

## Methode 2: Chocolatey (Schneller)

**Als Administrator PowerShell**:
```powershell
# Chocolatey installieren (falls nicht vorhanden)
Set-ExecutionPolicy Bypass -Scope Process -Force; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))

# Maven installieren
choco install maven
```

## Nach erfolgreicher Installation

**Backend starten**:
```cmd
cd C:\Users\bened\IdeaProjects\Smart_Trainingsplan\backend
mvn spring-boot:run
```

**Frontend starten** (neues Terminal):
```cmd
cd C:\Users\bened\IdeaProjects\Smart_Trainingsplan\frontend
npm install
npm start
```

## URLs nach dem Start
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080/api

## Troubleshooting
- **"mvn nicht erkannt"** → Neues Terminal öffnen nach Installation
- **PATH Problem** → Computer neu starten
- **Java Version** → Mindestens Java 8 erforderlich