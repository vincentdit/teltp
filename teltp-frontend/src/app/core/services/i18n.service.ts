import { Injectable, effect, signal } from '@angular/core';

const DICT = {
  en: {
    catalogue: 'Catalogue',
    home: 'Home',
    results: 'My Results',
    certificates: 'My Certificates',
    notifications: 'Notices',
    schedule: 'Schedule',
    forum: 'Forum',
    administration: 'Administration',
    reports: 'Reports',
    signIn: 'Sign in',
    createAccount: 'Create account',
    signOut: 'Sign out',
    dashboard: 'Dashboard',
    account: 'Account menu',
    skip: 'Skip to content',
    language: 'Kiswahili',
  },
  sw: {
    catalogue: 'Orodha',
    home: 'Mwanzo',
    results: 'Matokeo yangu',
    certificates: 'Vyeti vyangu',
    notifications: 'Taarifa',
    schedule: 'Ratiba',
    forum: 'Majadiliano',
    administration: 'Usimamizi',
    reports: 'Ripoti',
    signIn: 'Ingia',
    createAccount: 'Fungua akaunti',
    signOut: 'Toka',
    dashboard: 'Dashibodi',
    account: 'Menyu ya akaunti',
    skip: 'Ruka hadi maudhui',
    language: 'English',
  },
} as const;

export type Lang = keyof typeof DICT;
export type I18nKey = keyof typeof DICT.en;

@Injectable({ providedIn: 'root' })
export class I18nService {
  readonly lang = signal<Lang>(localStorage.getItem('teltp.lang') === 'sw' ? 'sw' : 'en');

  constructor() {
    effect(() => {
      const lang = this.lang();
      document.documentElement.lang = lang === 'sw' ? 'sw' : 'en';
      localStorage.setItem('teltp.lang', lang);
    });
  }

  t(key: I18nKey): string {
    return DICT[this.lang()][key];
  }

  toggle(): void {
    this.lang.update((l) => (l === 'en' ? 'sw' : 'en'));
  }
}
