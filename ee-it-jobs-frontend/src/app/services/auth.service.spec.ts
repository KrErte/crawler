import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    localStorage.clear();

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // Flush the /me call from constructor's loadFromStorage
    const meReqs = httpMock.match(`${environment.apiUrl}/api/auth/me`);
    meReqs.forEach(r => r.flush(null, { status: 401, statusText: 'Unauthorized' }));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should login and store tokens', () => {
    const mockResponse = {
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      tokenType: 'Bearer',
      user: { id: 1, email: 'test@test.com', firstName: 'Test', lastName: 'User', isAdmin: false }
    };

    service.login({ email: 'test@test.com', password: 'pass123' }).subscribe(res => {
      expect(res.accessToken).toBe('test-access');
      expect(localStorage.getItem('accessToken')).toBe('test-access');
      expect(localStorage.getItem('refreshToken')).toBe('test-refresh');
      expect(service.isLoggedIn()).toBeTrue();
      expect(service.user()?.email).toBe('test@test.com');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.email).toBe('test@test.com');
    req.flush(mockResponse);
  });

  it('should not store tokens when 2FA is required', () => {
    const mockResponse = {
      accessToken: '',
      refreshToken: '',
      tokenType: 'Bearer',
      user: null,
      requiresTwoFactor: true
    };

    service.login({ email: 'test@test.com', password: 'pass123' }).subscribe(res => {
      expect(res.requiresTwoFactor).toBeTrue();
      expect(localStorage.getItem('accessToken')).toBeNull();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
    req.flush(mockResponse);
  });

  it('should register and store tokens', () => {
    const mockResponse = {
      accessToken: 'new-access',
      refreshToken: 'new-refresh',
      tokenType: 'Bearer',
      user: { id: 2, email: 'new@test.com', firstName: 'New', lastName: 'User', isAdmin: false }
    };

    service.register({ email: 'new@test.com', password: 'pass123', firstName: 'New', lastName: 'User' }).subscribe(res => {
      expect(res.accessToken).toBe('new-access');
      expect(service.user()?.email).toBe('new@test.com');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should logout and clear state', () => {
    localStorage.setItem('accessToken', 'token');
    localStorage.setItem('refreshToken', 'refresh');

    service.logout();

    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should return token from getToken', () => {
    localStorage.setItem('accessToken', 'stored-token');
    expect(service.getToken()).toBe('stored-token');
  });

  it('should return null when no token', () => {
    expect(service.getToken()).toBeNull();
  });

  it('should refresh token', () => {
    localStorage.setItem('refreshToken', 'old-refresh');

    const mockResponse = {
      accessToken: 'refreshed-access',
      refreshToken: 'refreshed-refresh',
      tokenType: 'Bearer',
      user: { id: 1, email: 'test@test.com', firstName: 'Test', lastName: 'User', isAdmin: false }
    };

    service.refresh().subscribe(res => {
      expect(res.accessToken).toBe('refreshed-access');
      expect(localStorage.getItem('accessToken')).toBe('refreshed-access');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/refresh`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });
});
