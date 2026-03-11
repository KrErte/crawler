import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="flex items-center justify-center min-h-screen -mt-16 px-4">
      <div class="card w-full max-w-md">
        <h2 class="text-2xl font-bold text-white mb-6 text-center">Login</h2>
        @if (error) {
          <div class="bg-red-900/30 border border-red-800 text-red-400 px-4 py-2 rounded-lg mb-4">{{ error }}</div>
        }
        <form (ngSubmit)="onSubmit()" class="space-y-4">
          @if (!needsTwoFactor) {
            <div>
              <label class="block text-sm text-gray-400 mb-1">Email</label>
              <input type="email" [(ngModel)]="email" name="email" autocomplete="username" class="w-full" required />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1">Password</label>
              <input type="password" [(ngModel)]="password" name="password" autocomplete="current-password" class="w-full" required />
            </div>
          } @else {
            <div>
              <label class="block text-sm text-gray-400 mb-1">Two-Factor Code</label>
              <p class="text-gray-500 text-xs mb-2">Enter the 6-digit code from your authenticator app</p>
              <input type="text" [(ngModel)]="totpCode" name="totpCode" class="w-full text-center text-2xl tracking-widest"
                maxlength="6" autocomplete="one-time-code" required />
            </div>
          }
          <button type="submit" class="btn-primary w-full" [disabled]="loading">
            {{ loading ? 'Logging in...' : (needsTwoFactor ? 'Verify' : 'Login') }}
          </button>
        </form>
        @if (needsTwoFactor) {
          <button (click)="needsTwoFactor = false; error = ''" class="text-gray-500 hover:text-gray-400 text-sm mt-3 block text-center w-full">
            &larr; Back to login
          </button>
        }
        <div class="text-center mt-4 space-y-2">
          <p class="text-gray-500">
            <a routerLink="/forgot-password" class="text-accent hover:text-accent-hover">Forgot password?</a>
          </p>
          <p class="text-gray-500">
            Don't have an account? <a routerLink="/register" class="text-accent hover:text-accent-hover">Register</a>
          </p>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  email = '';
  password = '';
  totpCode = '';
  error = '';
  loading = false;
  needsTwoFactor = false;

  constructor(private auth: AuthService, private router: Router) {}

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.auth.login({
      email: this.email,
      password: this.password,
      totpCode: this.needsTwoFactor ? this.totpCode : undefined
    }).subscribe({
      next: (res) => {
        if (res.requiresTwoFactor) {
          this.needsTwoFactor = true;
          this.loading = false;
        } else {
          this.router.navigate(['/jobs']);
        }
      },
      error: (err) => {
        this.error = err.error?.message || 'Login failed';
        this.loading = false;
      }
    });
  }
}
