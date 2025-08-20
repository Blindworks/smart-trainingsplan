# Trainingswochen-Planung - Erklärung

## Konzept der rückwärts geplanten Trainingswochen

### Ausgangspunkt: Wettkampftermin
- **Wettkampf**: z.B. Marathon am **Sonntag, 15. September 2024**
- **Wettkampfwoche**: Hat die höchste Wochennummer im Plan (z.B. **Woche 12**)

### Rückwärts-Planung
Die Trainingswochen werden **vom Wettkampftermin rückwärts** geplant:

#### Beispiel Marathon-Plan (12 Wochen):
```
Wettkampf: Sonntag, 15. September 2024

Woche 12: Mo 09.09. - So 15.09.2024  ← Wettkampfwoche (Wettkampf am Sonntag)
Woche 11: Mo 02.09. - So 08.09.2024  ← 1 Woche vor Wettkampf
Woche 10: Mo 26.08. - So 01.09.2024  ← 2 Wochen vor Wettkampf
...
Woche 2:  Mo 08.07. - So 14.07.2024  ← 10 Wochen vor Wettkampf
Woche 1:  Mo 01.07. - So 07.07.2024  ← 11 Wochen vor Wettkampf
```

### Wochenstruktur
- **Jede Trainingswoche**: **Montag bis Sonntag**
- **Wettkämpfe**: Meist am **Sonntag** (Ende der Trainingswoche)

### JSON-Format
Im JSON-Trainingsplan entspricht:
- `"week": 12` → Wettkampfwoche
- `"week": 11` → 1 Woche vor Wettkampf
- `"week": 1` → 11 Wochen vor Wettkampf

### Automatische Generierung
1. **Wettkampftermin eingeben** (z.B. 15.09.2024)
2. **System findet Wettkampf-Sonntag** (15.09.2024)
3. **Rückwärts planen** 12 Wochen Training
4. **TrainingWeeks erstellen** mit korrekten Wochennummern

### Trainingsplan-Upload
Beim Upload eines JSON-Trainingsplans:
- **Wochennummer bleibt erhalten** (keine Umrechnung!)
- **Termine werden berechnet** basierend auf Wettkampfdatum
- **Automatische Zuordnung** zu den erstellten TrainingWeeks

## Vorteile dieses Systems:
- ✅ **Wettkampf-orientiert**: Alles dreht sich um das Zieldatum
- ✅ **Flexible Pläne**: 8, 12, 16 Wochen Pläne möglich
- ✅ **Realitätsnah**: Wettkämpfe sind meist sonntags
- ✅ **Einheitlich**: Alle Wochen Montag-Sonntag