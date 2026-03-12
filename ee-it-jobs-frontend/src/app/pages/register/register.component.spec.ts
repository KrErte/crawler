import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../services/auth.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call auth.register on submit', () => {
    const mockRes = { accessToken: 'tok', refreshToken: 'ref', tokenType: 'Bearer', user: { id: 1, email: 'new@test.com', firstName: 'New', lastName: 'User', isAdmin: false } };
    authSpy.register.and.returnValue(of(mockRes));
    spyOn(router, 'navigate');

    component.email = 'new@test.com';
    component.password = 'pass123';
    component.firstName = 'New';
    component.lastName = 'User';
    component.onSubmit();

    expect(authSpy.register).toHaveBeenCalledWith({
      email: 'new@test.com', password: 'pass123', firstName: 'New', lastName: 'User'
    });
  });

  it('should navigate to /jobs on success', () => {
    const mockRes = { accessToken: 'tok', refreshToken: 'ref', tokenType: 'Bearer', user: { id: 1, email: 'new@test.com', firstName: 'New', lastName: 'User', isAdmin: false } };
    authSpy.register.and.returnValue(of(mockRes));
    spyOn(router, 'navigate');

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/jobs']);
    expect(component.verificationSent).toBeTrue();
  });

  it('should show error on failure', () => {
    authSpy.register.and.returnValue(throwError(() => ({ error: { message: 'Email taken' } })));

    component.onSubmit();

    expect(component.error).toBe('Email taken');
    expect(component.loading).toBeFalse();
  });

  it('should show default error message when none provided', () => {
    authSpy.register.and.returnValue(throwError(() => ({ error: {} })));

    component.onSubmit();

    expect(component.error).toBe('Registration failed');
  });
});
