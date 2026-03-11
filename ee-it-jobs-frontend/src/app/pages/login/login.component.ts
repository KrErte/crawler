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
          <div>
            <label class="block text-sm text-gray-400 mb-1">Email</label>
            <input type="email" [(ngModel)]="email" name="email" autocomplete="username" class="w-full" required />
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1">Password</label>
            <input type="password" [(ngModel)]="password" name="password" autocomplete="current-password" class="w-full" required />
          </div>
          <button type="submit" class="btn-primary w-full" [disabled]="loading">
            {{ loading ? 'Logging in...' : 'Login' }}
          </button>
        </form>
        <p class="text-center text-gray-500 mt-4">
          Don't have an account? <a routerLink="/register" class="text-accent hover:text-accent-hover">Register</a>
        </p>
      </div>
    </div>
  `
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: () => { this.router.navigate(['/jobs']); },
      error: (err) => { this.error = err.error?.message || 'Login failed'; this.loading = false; }
    });
  }
}
