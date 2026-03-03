import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import {
  DEFAULT_LANGUAGE,
  LANGUAGE_STORAGE_KEY,
  Language,
  SUPPORTED_LANGUAGES,
  TRANSLATIONS
} from '../i18n/translations';

@Injectable({
  providedIn: 'root'
})
export class I18nService {
  private currentLanguage: Language;
  private readonly languageChangedSubject = new Subject<Language>();
  private readonly deToEnMap = new Map<string, string>();
  private readonly enToDeMap = new Map<string, string>();
  private readonly deToEnGlossary = this.buildGlossaryMap({
    'speichern': 'save',
    'speichern...': 'saving...',
    'abbrechen': 'cancel',
    'bearbeiten': 'edit',
    'loeschen': 'delete',
    'schliessen': 'close',
    'laden': 'load',
    'laden...': 'loading...',
    'wettkampf': 'race',
    'wettkaempfe': 'races',
    'rennen': 'race',
    'trainingsplan': 'training plan',
    'trainingsplaene': 'training plans',
    'training': 'training',
    'trainings': 'trainings',
    'hochladen': 'upload',
    'hochgeladen': 'uploaded',
    'daten importieren': 'import data',
    'profil': 'profile',
    'profilbild': 'profile image',
    'verbunden': 'connected',
    'trennen': 'disconnect',
    'mit strava verbinden': 'connect with strava',
    'letzte 30 tage': 'last 30 days',
    'keine aktivitaeten': 'no activities',
    'keine': 'no',
    'neu': 'new',
    'name': 'name',
    'beschreibung': 'description',
    'datum': 'date',
    'typ': 'type',
    'ort': 'location',
    'aktionen': 'actions',
    'benutzer': 'user',
    'benutzername': 'username',
    'vorname': 'first name',
    'nachname': 'last name',
    'email': 'email',
    'e-mail': 'email',
    'status': 'status',
    'freigabe': 'approval',
    'blockiert': 'blocked',
    'inaktiv': 'inactive',
    'aktiv': 'active',
    'anmelden': 'register',
    'abmelden': 'unregister',
    'angemeldet': 'registered',
    'teilnahme geplant': 'participation planned',
    'zielzeit': 'target time',
    'platzierung': 'ranking',
    'ranking': 'ranking',
    'vergleich': 'comparison',
    'koerperdaten': 'body data',
    'messungen': 'measurements',
    'tempobereiche': 'pace zones',
    'strecke': 'distance',
    'berechnen': 'calculate',
    'referenz': 'reference',
    'schwellentempo': 'threshold pace',
    'grundinformationen': 'basic information',
    'detaillierte anweisungen': 'detailed instructions',
    'aufwaermen': 'warm-up',
    'abkuehlen': 'cool-down',
    'benoetigte ausruestung': 'required equipment',
    'tipps & hinweise': 'tips & notes',
    'schwierigkeitsgrad': 'difficulty',
    'trainingsbewertung': 'training rating'
  });
  private readonly enToDeGlossary = this.buildGlossaryMap({
    'save': 'speichern',
    'saving...': 'speichern...',
    'cancel': 'abbrechen',
    'edit': 'bearbeiten',
    'delete': 'loeschen',
    'close': 'schliessen',
    'load': 'laden',
    'loading...': 'laden...',
    'race': 'wettkampf',
    'races': 'wettkaempfe',
    'training plan': 'trainingsplan',
    'training plans': 'trainingsplaene',
    'training': 'training',
    'upload': 'hochladen',
    'uploaded': 'hochgeladen',
    'import data': 'daten importieren',
    'profile': 'profil',
    'profile image': 'profilbild',
    'connected': 'verbunden',
    'disconnect': 'trennen',
    'connect with strava': 'mit strava verbinden',
    'last 30 days': 'letzte 30 tage',
    'no activities': 'keine aktivitaeten',
    'new': 'neu',
    'name': 'name',
    'description': 'beschreibung',
    'date': 'datum',
    'type': 'typ',
    'location': 'ort',
    'actions': 'aktionen',
    'user': 'benutzer',
    'username': 'benutzername',
    'first name': 'vorname',
    'last name': 'nachname',
    'email': 'e-mail',
    'status': 'status',
    'approval': 'freigabe',
    'blocked': 'blockiert',
    'inactive': 'inaktiv',
    'active': 'aktiv',
    'register': 'anmelden',
    'unregister': 'abmelden',
    'registered': 'angemeldet',
    'participation planned': 'teilnahme geplant',
    'target time': 'zielzeit',
    'ranking': 'platzierung',
    'comparison': 'vergleich',
    'body data': 'koerperdaten',
    'measurements': 'messungen',
    'pace zones': 'tempobereiche',
    'distance': 'strecke',
    'calculate': 'berechnen',
    'reference': 'referenz',
    'threshold pace': 'schwellentempo',
    'basic information': 'grundinformationen',
    'detailed instructions': 'detaillierte anweisungen',
    'warm-up': 'aufwaermen',
    'cool-down': 'abkuehlen',
    'required equipment': 'benoetigte ausruestung',
    'tips & notes': 'tipps & hinweise',
    'difficulty': 'schwierigkeitsgrad',
    'training rating': 'trainingsbewertung'
  });

  constructor() {
    this.currentLanguage = this.resolveInitialLanguage();
    this.buildLiteralMaps(TRANSLATIONS.de, TRANSLATIONS.en);
  }

  getLanguage(): Language {
    return this.currentLanguage;
  }

  get languageChanges$(): Observable<Language> {
    return this.languageChangedSubject.asObservable();
  }

  getSupportedLanguages(): readonly Language[] {
    return SUPPORTED_LANGUAGES;
  }

  setLanguage(language: string): void {
    if (!this.isSupported(language)) {
      return;
    }

    this.currentLanguage = language;
    localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    this.languageChangedSubject.next(language);
  }

  t(key: string, params?: Record<string, string | number | null | undefined>): string {
    const value = this.lookup(this.currentLanguage, key) ?? this.lookup(DEFAULT_LANGUAGE, key);
    if (typeof value !== 'string') {
      return key;
    }

    if (!params) {
      return value;
    }

    return value.replace(/\{(\w+)\}/g, (_match, token: string) => `${params[token] ?? `{${token}}`}`);
  }

  translateLiteral(text: string): string {
    const trimmed = text.trim();
    if (!trimmed) {
      return text;
    }

    const map = this.currentLanguage === 'en' ? this.deToEnMap : this.enToDeMap;
    const translated = map.get(trimmed);
    if (translated) {
      return text.replace(trimmed, translated);
    }

    const glossary = this.currentLanguage === 'en' ? this.deToEnGlossary : this.enToDeGlossary;
    const fallback = this.translateByGlossary(trimmed, glossary);
    if (fallback === trimmed) {
      return text;
    }

    return text.replace(trimmed, fallback);
  }

  private resolveInitialLanguage(): Language {
    const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (this.isSupported(stored)) {
      return stored;
    }

    const browserLanguage = navigator.language?.slice(0, 2);
    if (this.isSupported(browserLanguage)) {
      return browserLanguage;
    }

    return DEFAULT_LANGUAGE;
  }

  private lookup(language: Language, key: string): unknown {
    return key.split('.').reduce<unknown>((current, segment) => {
      if (!current || typeof current !== 'object') {
        return undefined;
      }
      return (current as Record<string, unknown>)[segment];
    }, TRANSLATIONS[language]);
  }

  private isSupported(language: string | null | undefined): language is Language {
    return !!language && SUPPORTED_LANGUAGES.includes(language as Language);
  }

  private buildLiteralMaps(deTree: unknown, enTree: unknown): void {
    if (typeof deTree === 'string' && typeof enTree === 'string') {
      this.registerLiteralPair(deTree, enTree);
      return;
    }

    if (!deTree || !enTree || typeof deTree !== 'object' || typeof enTree !== 'object') {
      return;
    }

    const deRecord = deTree as Record<string, unknown>;
    const enRecord = enTree as Record<string, unknown>;
    for (const key of Object.keys(deRecord)) {
      if (!(key in enRecord)) {
        continue;
      }
      this.buildLiteralMaps(deRecord[key], enRecord[key]);
    }
  }

  private registerLiteralPair(de: string, en: string): void {
    const deKey = this.normalize(de);
    const enKey = this.normalize(en);
    if (!deKey || !enKey) {
      return;
    }

    if (!this.deToEnMap.has(deKey)) {
      this.deToEnMap.set(deKey, en);
    }
    if (!this.enToDeMap.has(enKey)) {
      this.enToDeMap.set(enKey, de);
    }
  }

  private normalize(value: string): string {
    return value.replace(/\s+/g, ' ').trim();
  }

  private buildGlossaryMap(entries: Record<string, string>): Array<[RegExp, string]> {
    return Object.entries(entries)
      .sort((a, b) => b[0].length - a[0].length)
      .map(([source, target]) => {
        const escaped = source.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        return [new RegExp(`\\b${escaped}\\b`, 'gi'), target];
      });
  }

  private translateByGlossary(text: string, glossary: Array<[RegExp, string]>): string {
    let translated = text;
    for (const [pattern, replacement] of glossary) {
      translated = translated.replace(pattern, replacement);
    }
    return translated;
  }
}
