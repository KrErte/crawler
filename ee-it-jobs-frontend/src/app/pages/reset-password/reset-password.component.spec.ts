import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { ResetPasswordComponent } from './reset-password.component';
import { environment } from '../../../environments/environment';

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let httpMock: HttpTestingController;

  function setup(token: string | null) {
    TestBed.configureTestingModule({
      imports: [ResetPasswordComponent, HttpClientTestingModule, RouterTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: (key: string) => token } } }
        }
      ]
    });

    fixture = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    if (httpMock) httpMock.verify();
  });

  it('should create', () => {
    setup('valid-token');
    expect(component).toBeTruthy();
  });

  it('should extract token from query params', () => {
    setup('my-token');
    expect(component.error).toBe('');
  });

  it('should show error when no token', () => {
    setup(null);
    expect(component.error).toBe('Invalid reset link. Please request a new one.');
  });

  it('should show error when passwords do not match', () => {
    setup('valid-token');
    component.password = 'pass123';
    component.confirmPassword = 'different';
    component.onSubmit();
    expect(component.error).toBe('Passwords do not match.');
  });

  it('should submit reset password request', () => {
    setup('valid-token');
    component.password = 'newpass123';
    component.confirmPassword = 'newpass123';
    component.onSubmit();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/reset-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.token).toBe('valid-token');
    expect(req.request.body.newPassword).toBe('newpass123');
    req.flush({});

    expect(component.success).toBeTrue();
  });

  it('should show error on reset failure', () => {
    setup('expired-token');
    component.password = 'newpass';
    component.confirmPassword = 'newpass';
    component.onSubmit();

    httpMock.expectOne(`${environment.apiUrl}/api/auth/reset-password`)
      .flush({ message: 'Token expired' }, { status: 400, statusText: 'Bad Request' });

    expect(component.error).toBe('Token expired');
    expect(component.loading).toBeFalse();
  });
});
