# Maven Installation für Windows

## Schritt 1: Maven herunterladen

1. **Gehen Sie zu**: https://maven.apache.org/download.cgi
2. **Download**: `apache-maven-3.9.6-bin.zip` (Binary zip archive)

## Schritt 2: Installation

1. **ZIP-Datei extrahieren** nach: `C:\apache-maven-3.9.6`
2. **Oder nach**: `C:\Program Files\apache-maven-3.9.6`

## Schritt 3: Umgebungsvariablen setzen

### Option A: Über Windows-GUI
1. **Systemeinstellungen öffnen** (Windows-Taste + Pause)
2. **"Erweiterte Systemeinstellungen"**
3. **"Umgebungsvariablen"** klicken
4. **Neue Systemvariable erstellen**:
   - Name: `MAVEN_HOME`
   - Wert: `C:\apache-maven-3.9.6`
5. **PATH-Variable bearbeiten**:
   - `%MAVEN_HOME%\bin` hinzufügen

### Option B: Über Kommandozeile (Als Administrator)
```cmd
setx MAVEN_HOME "C:\apache-maven-3.9.6" /M
setx PATH "%PATH%;%MAVEN_HOME%\bin" /M
```

## Schritt 4: Testen

**Neues Command Prompt öffnen** und testen:
```cmd
mvn --version
```

**Erwartete Ausgabe**:
```
Apache Maven 3.9.6
Maven home: C:\apache-maven-3.9.6
Java version: 21.0.5, vendor: Oracle Corporation
```

## Schnell-Installation mit PowerShell (als Administrator)

```powershell
# Maven herunterladen
Invoke-WebRequest -Uri "https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -OutFile "C:\apache-maven-3.9.6-bin.zip"

# Entpacken
Expand-Archive -Path "C:\apache-maven-3.9.6-bin.zip" -DestinationPath "C:\"

# Umgebungsvariablen setzen
[Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\apache-maven-3.9.6", "Machine")
$env:Path += ";C:\apache-maven-3.9.6\bin"
[Environment]::SetEnvironmentVariable("Path", $env:Path, "Machine")

# Aufräumen
Remove-Item "C:\apache-maven-3.9.6-bin.zip"
```

## Nach der Installation

**Neues Terminal öffnen** und das Backend starten:
```cmd
cd C:\Users\bened\IdeaProjects\Smart_Trainingsplan\backend
mvn spring-boot:run
```