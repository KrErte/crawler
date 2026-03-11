import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TranslateService } from '../../services/translate.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, TranslatePipe],
  template: `
    <nav class="fixed top-0 left-0 right-0 z-50 bg-dark-800 border-b border-dark-700">
      <div class="max-w-7xl mx-auto px-4 flex items-center justify-between h-16">
        <a routerLink="/" class="text-xl font-bold text-accent">EE IT Jobs</a>

        <!-- Desktop nav -->
        <div class="hidden md:flex items-center gap-6">
          <a routerLink="/jobs" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.jobs' | translate }}</a>
          <a routerLink="/stats" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.stats' | translate }}</a>
          @if (auth.isLoggedIn()) {
            <a routerLink="/match" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.match' | translate }}</a>
            <a routerLink="/saved" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.saved' | translate }}</a>
            <a routerLink="/recommendations" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.recommendations' | translate }}</a>
            <a routerLink="/applications" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.applications' | translate }}</a>
            <a routerLink="/cv-builder" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.cvBuilder' | translate }}</a>
            <a routerLink="/profile" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.profile' | translate }}</a>
            @if (auth.isAdmin()) {
              <a routerLink="/admin" routerLinkActive="text-accent" class="text-gray-300 hover:text-white transition-colors">{{ 'nav.admin' | translate }}</a>
            }
            <button (click)="auth.logout()" class="text-gray-400 hover:text-red-400 transition-colors">{{ 'nav.logout' | translate }}</button>
          } @else {
            <a routerLink="/login" class="btn-secondary text-sm">{{ 'nav.login' | translate }}</a>
            <a routerLink="/register" class="btn-primary text-sm">{{ 'nav.register' | translate }}</a>
          }
          <button (click)="toggleTheme()"
            class="p-1.5 rounded bg-dark-700 text-gray-400 hover:text-white transition-colors" title="Toggle theme">
            @if (isDark) {
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4">
                <path d="M12 2.25a.75.75 0 01.75.75v2.25a.75.75 0 01-1.5 0V3a.75.75 0 01.75-.75zM7.5 12a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0zM18.894 6.166a.75.75 0 00-1.06-1.06l-1.591 1.59a.75.75 0 101.06 1.061l1.591-1.59zM21.75 12a.75.75 0 01-.75.75h-2.25a.75.75 0 010-1.5H21a.75.75 0 01.75.75zM17.834 18.894a.75.75 0 001.06-1.06l-1.59-1.591a.75.75 0 10-1.061 1.06l1.59 1.591zM12 18a.75.75 0 01.75.75V21a.75.75 0 01-1.5 0v-2.25A.75.75 0 0112 18zM7.758 17.303a.75.75 0 00-1.061-1.06l-1.591 1.59a.75.75 0 001.06 1.061l1.591-1.59zM6 12a.75.75 0 01-.75.75H3a.75.75 0 010-1.5h2.25A.75.75 0 016 12zM6.697 7.757a.75.75 0 001.06-1.06l-1.59-1.591a.75.75 0 00-1.061 1.06l1.59 1.591z" />
              </svg>
            } @else {
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4">
                <path fill-rule="evenodd" d="M9.528 1.718a.75.75 0 01.162.819A8.97 8.97 0 009 6a9 9 0 009 9 8.97 8.97 0 003.463-.69.75.75 0 01.981.98 10.503 10.503 0 01-9.694 6.46c-5.799 0-10.5-4.701-10.5-10.5 0-4.368 2.667-8.112 6.46-9.694a.75.75 0 01.818.162z" clip-rule="evenodd" />
              </svg>
            }
          </button>
          <button (click)="translate.toggleLanguage()"
            class="text-xs px-2 py-1 rounded bg-dark-700 text-gray-400 hover:text-white transition-colors">
            {{ translate.currentLang() === 'en' ? 'ET' : 'EN' }}
          </button>
        </div>

        <!-- Mobile hamburger + controls -->
        <div class="flex items-center gap-2 md:hidden">
          <button (click)="toggleTheme()"
            class="p-1.5 rounded bg-dark-700 text-gray-400 hover:text-white transition-colors">
            @if (isDark) {
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4">
                <path d="M12 2.25a.75.75 0 01.75.75v2.25a.75.75 0 01-1.5 0V3a.75.75 0 01.75-.75zM7.5 12a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0z" />
              </svg>
            } @else {
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4">
                <path fill-rule="evenodd" d="M9.528 1.718a.75.75 0 01.162.819A8.97 8.97 0 009 6a9 9 0 009 9 8.97 8.97 0 003.463-.69.75.75 0 01.981.98 10.503 10.503 0 01-9.694 6.46c-5.799 0-10.5-4.701-10.5-10.5 0-4.368 2.667-8.112 6.46-9.694a.75.75 0 01.818.162z" clip-rule="evenodd" />
              </svg>
            }
          </button>
          <button (click)="translate.toggleLanguage()"
            class="text-xs px-2 py-1 rounded bg-dark-700 text-gray-400 hover:text-white transition-colors">
            {{ translate.currentLang() === 'en' ? 'ET' : 'EN' }}
          </button>
          <button (click)="mobileMenuOpen = !mobileMenuOpen" class="p-2 text-gray-400 hover:text-white">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-6 h-6">
              @if (mobileMenuOpen) {
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              } @else {
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
              }
            </svg>
          </button>
        </div>
      </div>

      <!-- Mobile menu panel -->
      @if (mobileMenuOpen) {
        <div class="md:hidden bg-dark-800 border-t border-dark-700 px-4 py-3 space-y-2">
          <a routerLink="/jobs" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
            class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.jobs' | translate }}</a>
          <a routerLink="/stats" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
            class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.stats' | translate }}</a>
          @if (auth.isLoggedIn()) {
            <a routerLink="/match" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
              class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.match' | translate }}</a>
            <a routerLink="/saved" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
              class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.saved' | translate }}</a>
            <a routerLink="/recommendations" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
              class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.recommendations' | translate }}</a>
            <a routerLink="/applications" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
              class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.applications' | translate }}</a>
            <a routerLink="/cv-builder" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
              class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.cvBuilder' | translate }}</a>
            <a routerLink="/profile" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
              class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.profile' | translate }}</a>
            @if (auth.isAdmin()) {
              <a routerLink="/admin" routerLinkActive="text-accent" (click)="mobileMenuOpen = false"
                class="block py-2 text-gray-300 hover:text-white transition-colors">{{ 'nav.admin' | translate }}</a>
            }
            <button (click)="auth.logout(); mobileMenuOpen = false"
              class="block w-full text-left py-2 text-gray-400 hover:text-red-400 transition-colors">{{ 'nav.logout' | translate }}</button>
          } @else {
            <a routerLink="/login" (click)="mobileMenuOpen = false"
              class="block py-2 text-accent hover:text-accent-hover transition-colors">{{ 'nav.login' | translate }}</a>
            <a routerLink="/register" (click)="mobileMenuOpen = false"
              class="block py-2 text-accent hover:text-accent-hover transition-colors">{{ 'nav.register' | translate }}</a>
          }
        </div>
      }
    </nav>
  `
})
export class NavbarComponent {
  mobileMenuOpen = false;
  isDark = true;

  constructor(public auth: AuthService, public translate: TranslateService) {
    this.isDark = document.documentElement.classList.contains('dark');
  }

  toggleTheme() {
    this.isDark = !this.isDark;
    if (this.isDark) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }
}
