export type Language = 'de' | 'en';

export const SUPPORTED_LANGUAGES: readonly Language[] = ['de', 'en'];
export const DEFAULT_LANGUAGE: Language = 'de';
export const LANGUAGE_STORAGE_KEY = 'smart_trainingsplan.language';

export const TRANSLATIONS: Record<Language, Record<string, unknown>> = {
  de: {
    common: {
      appName: 'Smart Trainingsplan',
      profile: 'Profil',
      loading: 'Laden...'
    },
    nav: {
      dashboard: 'Dashboard',
      races: 'Rennen',
      plans: 'Plaene',
      training: 'Training',
      log: 'Log',
      body: 'Koerper',
      bodyData: 'Koerperdaten',
      comparison: 'Vergleich',
      admin: 'Admin',
      usersAdmin: 'Benutzerverwaltung',
      racesAdmin: 'Rennverwaltung',
      plansAdmin: 'Trainingsplaene',
      logout: 'Abmelden',
      menuOpen: 'Menue oeffnen',
      menuClose: 'Menue schliessen',
      language: 'Sprache'
    },
    landing: {
      kicker: 'Performance Training neu gedacht',
      title: 'Trainiere smarter fuer dein naechstes Rennen.',
      subtitle:
        'PACR hilft dir, Wettkampfziele zu strukturieren, Plaene hochzuladen und deinen Trainingsfortschritt jede Woche sichtbar zu machen.',
      startPlanning: 'Planung starten',
      viewOverview: 'Uebersicht ansehen',
      featureRaceTitle: 'Nach Renndatum planen',
      featureRaceText: 'Erstelle Wettkaempfe und verknuepfe passende Plaene fuer einen klaren Weg zum Renntag.',
      featureTrackTitle: 'Abschluss verfolgen',
      featureTrackText: 'Logge deine woechentlichen Einheiten und erkenne Konsistenztrends auf einen Blick.',
      featureAllTitle: 'Alles an einem Ort',
      featureAllText: 'Wechsle nahtlos zwischen Rennen, Wochenplan und Profil.'
    },
    login: {
      title: 'Anmelden',
      username: 'Benutzername',
      password: 'Passwort',
      requiredUsername: 'Benutzername ist erforderlich',
      requiredPassword: 'Passwort ist erforderlich',
      submit: 'Anmelden',
      noAccount: 'Noch kein Konto?',
      register: 'Registrieren',
      invalidCredentials: 'Ungueltige Anmeldedaten',
      failed: 'Anmeldung fehlgeschlagen',
      unavailable: 'Anmeldung derzeit nicht moeglich.'
    },
    register: {
      title: 'Registrieren',
      email: 'E-Mail',
      requiredEmail: 'E-Mail ist erforderlich',
      invalidEmail: 'Ungueltige E-Mail-Adresse',
      minUsername: 'Mindestens 3 Zeichen',
      minPassword: 'Mindestens 6 Zeichen',
      next: 'Weiter',
      verificationCode: 'Verifizierungscode',
      codeRequired: 'Code ist erforderlich',
      codeInvalid: 'Der Code muss aus genau 6 Ziffern bestehen',
      complete: 'Registrierung abschliessen',
      back: 'Zurueck',
      hasAccount: 'Bereits ein Konto?',
      verificationHint: 'Gib den 6-stelligen Code ein, den wir an {email} gesendet haben.',
      verificationSent: 'Wir haben einen 6-stelligen Code an deine E-Mail gesendet.',
      duplicateUser: 'Benutzername oder E-Mail bereits vergeben',
      failed: 'Registrierung fehlgeschlagen',
      verificationFailed: 'Code-Verifizierung fehlgeschlagen',
      login: 'Anmelden'
    },
    accountStatus: {
      emailPending: 'Registrierung erfolgreich. Bitte bestaetige zuerst deine E-Mail-Adresse.',
      adminPending: 'Dein Konto wartet auf Freigabe durch einen Admin.',
      blocked: 'Dein Konto ist blockiert. Bitte kontaktiere den Support.',
      inactive: 'Dein Konto ist inaktiv. Bitte kontaktiere den Support.',
      active: 'Dein Konto ist aktiv.'
    }
  },
  en: {
    common: {
      appName: 'Smart Training Plan',
      profile: 'Profile',
      loading: 'Loading...'
    },
    nav: {
      dashboard: 'Dashboard',
      races: 'Races',
      plans: 'Plans',
      training: 'Training',
      log: 'Log',
      body: 'Body',
      bodyData: 'Body Data',
      comparison: 'Comparison',
      admin: 'Admin',
      usersAdmin: 'User Management',
      racesAdmin: 'Race Management',
      plansAdmin: 'Training Plans',
      logout: 'Logout',
      menuOpen: 'Open menu',
      menuClose: 'Close menu',
      language: 'Language'
    },
    landing: {
      kicker: 'Performance Training Reimagined',
      title: 'Train smarter for your next race.',
      subtitle:
        'PACR helps you structure competition goals, upload plans, and keep your training progress visible every week.',
      startPlanning: 'Start Planning',
      viewOverview: 'View Overview',
      featureRaceTitle: 'Plan by race date',
      featureRaceText: 'Create competitions and attach tailored plans for a clear path to race day.',
      featureTrackTitle: 'Track completion',
      featureTrackText: 'Log your weekly sessions and spot consistency trends at a glance.',
      featureAllTitle: 'Everything in one place',
      featureAllText: 'Switch seamlessly between races, schedule overview, and profile settings.'
    },
    login: {
      title: 'Sign in',
      username: 'Username',
      password: 'Password',
      requiredUsername: 'Username is required',
      requiredPassword: 'Password is required',
      submit: 'Sign in',
      noAccount: "Don't have an account?",
      register: 'Register',
      invalidCredentials: 'Invalid credentials',
      failed: 'Login failed',
      unavailable: 'Login is currently unavailable.'
    },
    register: {
      title: 'Register',
      email: 'Email',
      requiredEmail: 'Email is required',
      invalidEmail: 'Invalid email address',
      minUsername: 'At least 3 characters',
      minPassword: 'At least 6 characters',
      next: 'Continue',
      verificationCode: 'Verification code',
      codeRequired: 'Code is required',
      codeInvalid: 'Code must be exactly 6 digits',
      complete: 'Complete registration',
      back: 'Back',
      hasAccount: 'Already have an account?',
      verificationHint: 'Enter the 6-digit code we sent to {email}.',
      verificationSent: 'We sent a 6-digit code to your email.',
      duplicateUser: 'Username or email already in use',
      failed: 'Registration failed',
      verificationFailed: 'Code verification failed',
      login: 'Sign in'
    },
    accountStatus: {
      emailPending: 'Registration successful. Please verify your email first.',
      adminPending: 'Your account is awaiting admin approval.',
      blocked: 'Your account is blocked. Please contact support.',
      inactive: 'Your account is inactive. Please contact support.',
      active: 'Your account is active.'
    }
  }
};
