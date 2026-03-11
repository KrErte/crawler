import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="flex items-center justify-center min-h-screen -mt-16 px-4">
      <div class="card w-full max-w-md">
        <h2 class="text-2xl font-bold text-white mb-6 text-center">Forgot Password</h2>
        @if (sent) {
          <div class="bg-green-900/30 border border-green-800 text-green-400 px-4 py-3 rounded-lg text-center">
            If an account with that email exists, a password reset link has been sent.
          </div>
          <p class="text-center text-gray-500 mt-4">
            <a routerLink="/login" class="text-accent hover:text-accent-hover">Back to Login</a>
          </p>
        } @else {
          <p class="text-gray-400 text-sm mb-4">Enter your email address and we'll send you a link to reset your password.</p>
          @if (error) {
            <div class="bg-red-900/30 border border-red-800 text-red-400 px-4 py-2 rounded-lg mb-4">{{ error }}</div>
          }
          <form (ngSubmit)="onSubmit()" class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1">Email</label>
              <input type="email" [(ngModel)]="email" name="email" class="w-full" required />
            </div>
            <button type="submit" class="btn-primary w-full" [disabled]="loading">
              {{ loading ? 'Sending...' : 'Send Reset Link' }}
            </button>
          </form>
          <p class="text-center text-gray-500 mt-4">
            <a routerLink="/login" class="text-accent hover:text-accent-hover">Back to Login</a>
          </p>
        }
      </div>
    </div>
  `
})
export class ForgotPasswordComponent {
  email = '';
  loading = false;
  sent = false;
  error = '';

  constructor(private http: HttpClient) {}

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.http.post(`${environment.apiUrl}/api/auth/forgot-password`, { email: this.email }).subscribe({
      next: () => { this.sent = true; this.loading = false; },
      error: () => { this.error = 'An error occurred. Please try again.'; this.loading = false; }
    });
  }
}
