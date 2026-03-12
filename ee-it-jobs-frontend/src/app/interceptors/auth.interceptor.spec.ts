import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { environment } from '../../environments/environment';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authSpy: jasmine.SpyObj<AuthService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', ['logout']);
    toastSpy = jasmine.createSpyObj('ToastService', ['error', 'info']);
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should add Bearer header when token exists', () => {
    localStorage.setItem('accessToken', 'my-token');

    http.get(`${environment.apiUrl}/api/jobs`).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/jobs`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
    req.flush([]);
  });

  it('should not add Bearer header for auth endpoints', () => {
    localStorage.setItem('accessToken', 'my-token');

    http.get(`${environment.apiUrl}/api/auth/me`).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/me`);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should not add Bearer header when no token', () => {
    http.get(`${environment.apiUrl}/api/jobs`).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/jobs`);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('should logout on 401 for non-auth endpoint', () => {
    http.get(`${environment.apiUrl}/api/jobs`).subscribe({
      error: () => {}
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/jobs`);
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(authSpy.logout).toHaveBeenCalled();
    expect(toastSpy.error).toHaveBeenCalledWith('Session expired. Please log in again.');
  });

  it('should show toast on 500 error', () => {
    http.get(`${environment.apiUrl}/api/jobs`).subscribe({
      error: () => {}
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/jobs`);
    req.flush(null, { status: 500, statusText: 'Internal Server Error' });

    expect(toastSpy.error).toHaveBeenCalledWith('Server error. Please try again later.');
  });

  it('should retry on 429 and show info toast', () => {
    http.get(`${environment.apiUrl}/api/jobs`).subscribe({
      next: (data) => {
        expect(data).toEqual([{ id: 1 }]);
      },
      error: () => {}
    });

    // First request gets 429
    const req1 = httpMock.expectOne(`${environment.apiUrl}/api/jobs`);
    req1.flush(null, { status: 429, statusText: 'Too Many Requests' });

    expect(toastSpy.info).toHaveBeenCalledWith('Too many requests. Retrying...');
  });
});
