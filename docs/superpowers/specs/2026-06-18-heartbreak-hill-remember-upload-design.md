# Heartbreak Hill — Upload merken (localStorage-Snapshot) — Design / Spec

- **Datum:** 2026-06-18
- **Autor:** Benedikt Lind (mit Claude Code)
- **Status:** Entwurf zur Abnahme
- **Kontext:** Erweiterung des [Heartbreak-Hill-Challenge](2026-06-16-heartbreak-hill-challenge-design.md)-Features und des darauf aufbauenden [Story-Bilds](2026-06-17-heartbreak-hill-share-image-design.md). Auf der öffentlichen, anonymen Seite `/heartbreak-hill` lädt ein Nutzer seine Aktivität (GPX) hoch und bekommt im Result-Panel sein Ergebnis (Rang, Zeit, Rückstand) plus das teilbare Story-Bild. **Problem:** Das Ergebnis lebt aktuell nur im flüchtigen Angular-Signal `result`. Beim Neuladen der Seite ist es weg — der Nutzer kann sein Bild später nicht erneut teilen und sieht nicht, was er hochgeladen hatte. Dieses Feature merkt sich den Upload im Browser, sodass das Ergebnis beim Wiederkommen automatisch wieder erscheint.

---

## 1 · Ziele & Erfolgskriterien

- **Wiedersehen ohne Re-Upload:** Kommt der Nutzer (gleiches Gerät/Browser) auf die Seite zurück, sieht er sein zuletzt hochgeladenes Ergebnis sofort wieder — inkl. teilbarem Story-Bild.
- **Null Hürde:** funktioniert anonym, kein Login, kein Server-Roundtrip — passend zur öffentlichen Seite.
- **Pro Aktivitätstyp:** Rad- und Lauf-Upload werden getrennt gemerkt, konsistent zu den getrennten Tabs/Bestenlisten.
- **Erfolgskriterium:** Nach einem Upload und einem Browser-Reload erscheint das Result-Panel mit korrektem Rang/Zeit und funktionierender Story-Bild-Vorschau automatisch wieder; ein Tab-Wechsel zeigt das jeweils passende gemerkte Ergebnis; ein „Verwerfen" entfernt es dauerhaft.

## 2 · Getroffene Entscheidungen (Brainstorming)

| Thema | Entscheidung |
|---|---|
| Speicherort | **`localStorage`** statt echtem Cookie. Kein Server-Roundtrip pro Request, mehr Kapazität, das Story-Bild wird ohnehin clientseitig gerendert. Ein Cookie brächte nur Overhead. |
| Persistenz-Tiefe | **Snapshot** — das vollständige `EffortResult` wird so gespeichert, wie es beim Upload war. Kein Live-Refetch des Rangs, **kein neuer Backend-Endpunkt**. Rang ist auf den Upload-Zeitpunkt eingefroren (für das Teilen des Bildes ausreichend). |
| Granularität | **Pro Aktivitätstyp getrennt** — je ein gemerktes Ergebnis für `RIDE` und `RUN`. |
| Anzeige beim Reload | **Auto-Reveal** — das Result-Panel klappt automatisch wieder auf (Formular bleibt daneben sichtbar). Dezenter Hinweis „gemerkt" + **Verwerfen**-Link. |
| Ablauf | **Kein** Ablaufdatum. Bleibt, bis der Nutzer verwirft oder durch einen neuen Upload desselben Typs überschreibt. |

## 3 · Architektur-Ansatz

Reine **Frontend-Erweiterung** der bestehenden Komponente `HeartbreakHill`. **Kein Backend-Touch**, keine Migration, keine neuen Endpunkte, kein Model-Feld (das `EffortResult` wird unverändert übernommen — es enthält bereits den ungenutzten `editToken`, der mitgespeichert wird und für einen späteren „Effort claimen"-Funnel nützlich bleibt).

Die Persistenzlogik kommt in eine **reine Util-Datei** mit Spec — analog zum bestehenden Projektmuster (`heartbreak-hill.util.ts`, `share-image.util.ts` mit zugehörigen `*.util.spec.ts`). Das hält die Komponente schlank und macht die Logik isoliert testbar.

Begründung Snapshot statt Live-Refetch: Der Hauptzweck ist das erneute Teilen des Story-Bildes, das clientseitig aus genau diesen Werten gerendert wird. Ein exakt aktueller Rang ist dafür nicht nötig und würde einen neuen öffentlichen Endpunkt (`GET …/efforts/{id}` mit Rang-Neuberechnung) erfordern. Bewusst vermieden (YAGNI).

## 4 · Datenmodell (localStorage)

- **Key:** `pacr.heartbreak-hill.efforts.v1` (versioniert — bei künftiger Schema-Änderung `.v2`, alte Daten werden dann einfach ignoriert).
- **Wert (JSON):**
  ```ts
  interface StoredEffort {
    result: EffortResult;   // vollständiger Upload-Snapshot (inkl. editToken)
    displayName: string;    // für die Anzeige / Vorausfüllen beim Neu-Upload
    savedAt: string;        // ISO-Zeitstempel (clientseitig gesetzt)
  }
  type StoredEfforts = Partial<Record<ActivityType, StoredEffort>>;  // 'RIDE' | 'RUN' getrennt
  ```

## 5 · Util-Modul (neu)

`frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.ts` — reine Funktionen, alle `localStorage`-Zugriffe in `try/catch`:

- `loadStoredEffort(type: ActivityType): StoredEffort | null` — liest den Key, parsed JSON, gibt den Eintrag für den Typ zurück. Bei fehlendem/korruptem JSON oder fehlgeschlagener Validierung → `null` (kein Wurf).
- `saveStoredEffort(type, result, displayName): void` — merged in das bestehende Objekt (überschreibt nur den jeweiligen Typ), setzt `savedAt`. Schreibfehler (z. B. Quota) werden still verschluckt.
- `clearStoredEffort(type): void` — entfernt nur den Eintrag dieses Typs; ist danach kein Typ mehr vorhanden, wird der Key gelöscht.
- **Validierung beim Laden:** grob prüfen, dass `result` ein Objekt mit numerischem `effortId` und `rank` ist; sonst verwerfen (schützt vor altem Schema nach einem Deploy).

## 6 · Komponenten-Änderungen (`heartbreak-hill.ts`)

- **Neue Signals:** `restored = signal(false)` (zeigt an, dass das aktuelle Ergebnis aus dem Speicher stammt → steuert den Hinweis), `restoredAt = signal<string | null>(null)`.
- **`restoreForCurrentTab()` (neu, private):** liest `loadStoredEffort(activeTab())`. Bei Treffer → `result.set(stored.result)`, `displayName.set(stored.displayName)`, `restored.set(true)`, `restoredAt.set(stored.savedAt)`, `shareTemplate.set('A')`, `void renderShare()`. Kein Treffer → `result.set(null)`, `restored.set(false)`, `sharePreviewUrl.set(null)`.
- **`ngOnInit`:** im `next`-Callback von `getChallenge()` — **nach** dem Setzen von `challenge` (das `renderShare()` braucht die Polyline) — zusätzlich `restoreForCurrentTab()` aufrufen.
- **`submit()` success:** nach `result.set(res)` zusätzlich `saveStoredEffort(activeTab(), res, displayName().trim())` und `restored.set(false)` (frischer Upload zeigt keinen „gemerkt"-Hinweis).
- **`selectTab(type)`:** der bisherige `result.set(null)`-Zweig wird durch `restoreForCurrentTab()` ersetzt (nachdem `activeTab` gesetzt und der Leaderboard angestoßen wurde). So zeigt jeder Tab sein eigenes gemerktes Ergebnis.
- **`discardStored()` (neu):** `clearStoredEffort(activeTab())`, `result.set(null)`, `sharePreviewUrl.set(null)`, `restored.set(false)`. Bindet den Verwerfen-Link.

## 7 · Template-Änderung (`heartbreak-hill.html`)

Im Result-Panel ([heartbreak-hill.html:128](../../../frontend/src/app/components/heartbreak-hill/heartbreak-hill.html)) ein dezenter Block, der **nur bei `restored()`** erscheint — z. B. direkt unter dem `RESULT_LABEL`:

- Text `HEARTBREAK_HILL.RESTORED_HINT` („Von deinem letzten Upload gemerkt"), optional mit Datum aus `restoredAt()` via Angular `date`-Pipe.
- Ein als Link gestylter Button `HEARTBREAK_HILL.RESTORED_DISCARD` („Verwerfen") → `(click)="discardStored()"`.

Sonst keine strukturellen Änderungen: Das Upload-Formular steht ohnehin permanent links daneben (`.two-col`), das Result-Panel rechts erscheint allein über `@if (result())`. Neuer Upload überschreibt den Snapshot automatisch.

## 8 · i18n (en.json + de.json)

Neue Keys unter `HEARTBREAK_HILL`:
- `RESTORED_HINT` — EN „Saved from your last upload" / DE „Von deinem letzten Upload gemerkt"
- `RESTORED_DISCARD` — EN „Discard" / DE „Verwerfen"

## 9 · Edge Cases

- **`localStorage` nicht verfügbar** (Private Mode, deaktiviert, Quota voll) → alle Zugriffe in `try/catch`, Feature degradiert still zum bisherigen Verhalten (kein Merken), kein Crash.
- **Korrupter/alter Eintrag** (Schema-Drift nach Deploy) → Validierung in `loadStoredEffort` schlägt fehl → `null`, normaler Empty-State.
- **Reihenfolge beim Reload** → Wiederherstellung erst nach Challenge-Load, da `renderShare()` die Challenge-Polyline benötigt.
- **Beide Typen gemerkt** → Tab-Wechsel zeigt jeweils den passenden Snapshot; ist für einen Typ nichts gemerkt, zeigt der Tab den normalen Upload-Zustand (kein Panel).
- **Frischer Upload vs. gemerkt** → nach `submit()` ist `restored=false`, der „gemerkt"-Hinweis bleibt aus.
- **Rang veraltet** → bewusst akzeptiert (Snapshot). Kein UI-Versprechen von Live-Aktualität.

## 10 · Tests

- **Frontend (Vitest):** `heartbreak-storage.util.spec.ts` — save→load Roundtrip; Typ-Trennung (RIDE überschreibt RUN nicht); `clear` entfernt nur den Typ bzw. den ganzen Key; korruptes JSON / fehlende Felder → `null` statt Wurf; Verhalten bei fehlendem `localStorage` (gemockt). Das Canvas-Pixelergebnis wird nicht getestet (jsdom, wie bei share-image bereits dokumentiert).
- **Manuell:** Upload → Reload → Panel + Vorschau wieder da; Tab-Wechsel RIDE/RUN; Verwerfen; zweiter Upload überschreibt.

## 11 · Version & Changelog

- Neues Feature → **minor**-Bump via `./version-bump.sh minor` (pom.xml + package.json im selben Commit).
- `CHANGELOG.md` unter `[Unreleased] / Added`: Heartbreak-Hill-Challenge merkt sich den Upload pro Aktivitätstyp im Browser (localStorage); Ergebnis und teilbares Story-Bild erscheinen beim erneuten Besuch automatisch wieder.

## 12 · Betroffene Dateien

**Neu**
- `frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.ts`
- `frontend/src/app/components/heartbreak-hill/heartbreak-storage.util.spec.ts`

**Geändert**
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.ts` — `restored`/`restoredAt`-Signals, `restoreForCurrentTab()`, `discardStored()`, Anpassung von `ngOnInit`, `submit()`, `selectTab()`
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.html` — Restored-Hinweis + Verwerfen-Link im Result-Panel
- `frontend/src/app/components/heartbreak-hill/heartbreak-hill.scss` — Styles für den dezenten Hinweis
- `frontend/src/assets/i18n/en.json`, `de.json` — `RESTORED_HINT`, `RESTORED_DISCARD`
- `pom.xml`, `frontend/package.json`, `CHANGELOG.md` — Version + Changelog

## 13 · Nicht im Scope (YAGNI)

- Echter Cookie / Server-Session.
- Live-Refetch des Rangs, neuer Backend-Endpunkt.
- Geräteübergreifendes Merken (anderes Gerät → kein Snapshot; dafür existiert der spätere Signup-/Claim-Funnel via `editToken`).
- Ablaufdatum / automatisches Aufräumen nach dem Event.
- Mehrere Versuche pro Typ in einer Historie (nur der letzte Upload je Typ wird gemerkt).
