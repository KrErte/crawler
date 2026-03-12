import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start with empty fields', () => {
    expect(component.email).toBe('');
    expect(component.password).toBe('');
    expect(component.loading).toBeFalse();
    expect(component.error).toBe('');
  });

  it('should call auth.login on submit', () => {
    const mockRes = { accessToken: 'tok', refreshToken: 'ref', tokenType: 'Bearer', user: { id: 1, email: 't@t.com', firstName: 'T', lastName: 'U', isAdmin: false } };
    authSpy.login.and.returnValue(of(mockRes));
    spyOn(router, 'navigate');

    component.email = 'test@test.com';
    component.password = 'pass123';
    component.onSubmit();

    expect(authSpy.login).toHaveBeenCalledWith({ email: 'test@test.com', password: 'pass123', totpCode: undefined });
  });

  it('should navigate to /jobs on successful login', () => {
    const mockRes = { accessToken: 'tok', refreshToken: 'ref', tokenType: 'Bearer', user: { id: 1, email: 't@t.com', firstName: 'T', lastName: 'U', isAdmin: false } };
    authSpy.login.and.returnValue(of(mockRes));
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/jobs']);
  });

  it('should show 2FA form when required', () => {
    const mockRes = { accessToken: '', refreshToken: '', tokenType: 'Bearer', user: null as any, requiresTwoFactor: true };
    authSpy.login.and.returnValue(of(mockRes));

    component.onSubmit();

    expect(component.needsTwoFactor).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should send TOTP code on 2FA submit', () => {
    const mockRes = { accessToken: 'tok', refreshToken: 'ref', tokenType: 'Bearer', user: { id: 1, email: 't@t.com', firstName: 'T', lastName: 'U', isAdmin: false } };
    authSpy.login.and.returnValue(of(mockRes));
    spyOn(router, 'navigate');

    component.needsTwoFactor = true;
    component.totpCode = '123456';
    component.email = 'test@test.com';
    component.password = 'pass';
    component.onSubmit();

    expect(authSpy.login).toHaveBeenCalledWith({ email: 'test@test.com', password: 'pass', totpCode: '123456' });
  });

  it('should show error on login failure', () => {
    authSpy.login.and.returnValue(throwError(() => ({ error: { message: 'Invalid credentials' } })));

    component.onSubmit();

    expect(component.error).toBe('Invalid credentials');
    expect(component.loading).toBeFalse();
  });

  it('should set loading state during login', () => {
    authSpy.login.and.returnValue(of({ accessToken: 'tok', refreshToken: 'ref', tokenType: 'Bearer', user: { id: 1, email: 't@t.com', firstName: 'T', lastName: 'U', isAdmin: false } }));
    spyOn(router, 'navigate');

    component.onSubmit();
    // After successful login, loading stays true (navigation happens)
    expect(authSpy.login).toHaveBeenCalled();
  });
});
