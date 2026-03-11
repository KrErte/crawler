import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UserDto } from '../models/auth.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = `${environment.apiUrl}/api/auth`;
  private currentUser = signal<UserDto | null>(null);

  user = this.currentUser.asReadonly();
  isLoggedIn = computed(() => !!this.currentUser());
  isAdmin = computed(() => this.currentUser()?.isAdmin === true);

  constructor(private http: HttpClient, private router: Router) {
    this.loadFromStorage();
  }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/login`, req).pipe(
      tap(res => {
        if (!res.requiresTwoFactor) {
          this.handleAuth(res);
        }
      })
    );
  }

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register`, req).pipe(
      tap(res => this.handleAuth(res))
    );
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<AuthResponse>(`${this.API}/refresh`, { refreshToken }).pipe(
      tap(res => this.handleAuth(res))
    );
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  // 2FA methods
  setup2fa(): Observable<any> {
    return this.http.post<any>(`${this.API}/2fa/setup`, {});
  }

  verify2fa(code: string): Observable<any> {
    return this.http.post<any>(`${this.API}/2fa/verify`, { code });
  }

  disable2fa(code: string): Observable<any> {
    return this.http.post<any>(`${this.API}/2fa/disable`, { code });
  }

  private handleAuth(res: AuthResponse): void {
    if (res.accessToken) {
      localStorage.setItem('accessToken', res.accessToken);
      localStorage.setItem('refreshToken', res.refreshToken);
      this.currentUser.set(res.user);
    }
  }

  private loadFromStorage(): void {
    const token = localStorage.getItem('accessToken');
    if (token) {
      this.http.get<UserDto>(`${this.API}/me`).subscribe({
        next: user => this.currentUser.set(user),
        error: () => this.logout()
      });
    }
  }
}
