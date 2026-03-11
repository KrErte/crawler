import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="flex items-center justify-center min-h-screen -mt-16 px-4">
      <div class="card w-full max-w-md">
        <h2 class="text-2xl font-bold text-white mb-6 text-center">Reset Password</h2>
        @if (success) {
          <div class="bg-green-900/30 border border-green-800 text-green-400 px-4 py-3 rounded-lg text-center">
            Your password has been reset successfully.
          </div>
          <p class="text-center mt-4">
            <a routerLink="/login" class="text-accent hover:text-accent-hover">Go to Login</a>
          </p>
        } @else {
          @if (error) {
            <div class="bg-red-900/30 border border-red-800 text-red-400 px-4 py-2 rounded-lg mb-4">{{ error }}</div>
          }
          <form (ngSubmit)="onSubmit()" class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1">New Password</label>
              <input type="password" [(ngModel)]="password" name="password" class="w-full" required minlength="6" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1">Confirm Password</label>
              <input type="password" [(ngModel)]="confirmPassword" name="confirmPassword" class="w-full" required />
            </div>
            <button type="submit" class="btn-primary w-full" [disabled]="loading">
              {{ loading ? 'Resetting...' : 'Reset Password' }}
            </button>
          </form>
        }
      </div>
    </div>
  `
})
export class ResetPasswordComponent implements OnInit {
  password = '';
  confirmPassword = '';
  loading = false;
  success = false;
  error = '';
  private token = '';

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.error = 'Invalid reset link. Please request a new one.';
    }
  }

  onSubmit() {
    if (this.password !== this.confirmPassword) {
      this.error = 'Passwords do not match.';
      return;
    }
    this.loading = true;
    this.error = '';
    this.http.post(`${environment.apiUrl}/api/auth/reset-password`, {
      token: this.token, newPassword: this.password
    }).subscribe({
      next: () => { this.success = true; this.loading = false; },
      error: (err) => {
        this.error = err.error?.message || 'Invalid or expired reset token.';
        this.loading = false;
      }
    });
  }
}
