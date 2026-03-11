import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="flex items-center justify-center min-h-screen -mt-16 px-4">
      <div class="card w-full max-w-md">
        <h2 class="text-2xl font-bold text-white mb-6 text-center">Create Account</h2>
        @if (error) {
          <div class="bg-red-900/30 border border-red-800 text-red-400 px-4 py-2 rounded-lg mb-4">{{ error }}</div>
        }
        <form (ngSubmit)="onSubmit()" class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1">First Name</label>
              <input type="text" [(ngModel)]="firstName" name="firstName" autocomplete="given-name" class="w-full" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1">Last Name</label>
              <input type="text" [(ngModel)]="lastName" name="lastName" autocomplete="family-name" class="w-full" />
            </div>
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1">Email</label>
            <input type="email" [(ngModel)]="email" name="email" autocomplete="username" class="w-full" required />
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1">Password</label>
            <input type="password" [(ngModel)]="password" name="password" autocomplete="new-password" class="w-full" required minlength="6" />
          </div>
          <button type="submit" class="btn-primary w-full" [disabled]="loading">
            {{ loading ? 'Creating account...' : 'Register' }}
          </button>
        </form>
        <p class="text-center text-gray-500 mt-4">
          Already have an account? <a routerLink="/login" class="text-accent hover:text-accent-hover">Login</a>
        </p>
      </div>
    </div>
  `
})
export class RegisterComponent {
  email = '';
  password = '';
  firstName = '';
  lastName = '';
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.auth.register({
      email: this.email, password: this.password,
      firstName: this.firstName, lastName: this.lastName
    }).subscribe({
      next: () => { this.router.navigate(['/jobs']); },
      error: (err) => { this.error = err.error?.message || 'Registration failed'; this.loading = false; }
    });
  }
}
