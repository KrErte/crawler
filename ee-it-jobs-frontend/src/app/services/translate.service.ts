import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class TranslateService {
  private translations: Record<string, string> = {};
  private _currentLang = signal<string>('en');
  currentLang = this._currentLang.asReadonly();

  constructor(private http: HttpClient) {
    const saved = localStorage.getItem('lang') || 'en';
    this.setLanguage(saved);
  }

  setLanguage(lang: string) {
    this._currentLang.set(lang);
    localStorage.setItem('lang', lang);
    this.http.get<Record<string, string>>(`/assets/i18n/${lang}.json`).subscribe({
      next: (data) => { this.translations = data; },
      error: () => {
        // Fallback to English
        if (lang !== 'en') {
          this.http.get<Record<string, string>>('/assets/i18n/en.json').subscribe(data => {
            this.translations = data;
          });
        }
      }
    });
  }

  t(key: string): string {
    return this.translations[key] || key;
  }

  toggleLanguage() {
    this.setLanguage(this._currentLang() === 'en' ? 'et' : 'en');
  }
}
