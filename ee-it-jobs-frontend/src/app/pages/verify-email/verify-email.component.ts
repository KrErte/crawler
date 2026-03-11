import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="flex items-center justify-center min-h-screen -mt-16 px-4">
      <div class="card w-full max-w-md text-center">
        @if (loading) {
          <div class="text-gray-400">Verifying your email...</div>
        } @else if (success) {
          <div class="text-green-400 text-lg font-semibold mb-4">Email verified successfully!</div>
          <p class="text-gray-400 mb-4">Your email has been verified. You can now use all features.</p>
          <a routerLink="/login" class="btn-primary inline-block">Go to Login</a>
        } @else {
          <div class="text-red-400 text-lg font-semibold mb-4">Verification Failed</div>
          <p class="text-gray-400">{{ error }}</p>
          <a routerLink="/" class="text-accent hover:text-accent-hover mt-4 inline-block">Go Home</a>
        }
      </div>
    </div>
  `
})
export class VerifyEmailComponent implements OnInit {
  loading = true;
  success = false;
  error = '';

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.loading = false;
      this.error = 'Invalid verification link.';
      return;
    }
    this.http.get(`${environment.apiUrl}/api/auth/verify-email`, { params: { token } }).subscribe({
      next: () => { this.success = true; this.loading = false; },
      error: (err) => {
        this.error = err.error?.message || 'Invalid or expired verification link.';
        this.loading = false;
      }
    });
  }
}
