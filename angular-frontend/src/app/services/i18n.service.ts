import { Injectable } from '@angular/core';
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

  constructor() {
    this.currentLanguage = this.resolveInitialLanguage();
  }

  getLanguage(): Language {
    return this.currentLanguage;
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
  }

  t(key: string, params?: Record<string, string | number>): string {
    const value = this.lookup(this.currentLanguage, key) ?? this.lookup(DEFAULT_LANGUAGE, key);
    if (typeof value !== 'string') {
      return key;
    }

    if (!params) {
      return value;
    }

    return value.replace(/\{(\w+)\}/g, (_match, token: string) => `${params[token] ?? `{${token}}`}`);
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
}
