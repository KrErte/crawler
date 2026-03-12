import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ForgotPasswordComponent } from './forgot-password.component';
import { environment } from '../../../environments/environment';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, HttpClientTestingModule, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should send forgot password request', () => {
    component.email = 'test@test.com';
    component.onSubmit();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/forgot-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.email).toBe('test@test.com');
    req.flush({});

    expect(component.sent).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should show success state after sending', () => {
    component.email = 'test@test.com';
    component.onSubmit();

    httpMock.expectOne(`${environment.apiUrl}/api/auth/forgot-password`).flush({});
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('password reset link has been sent');
  });

  it('should show error on failure', () => {
    component.email = 'test@test.com';
    component.onSubmit();

    httpMock.expectOne(`${environment.apiUrl}/api/auth/forgot-password`).error(new ProgressEvent('error'));

    expect(component.error).toBe('An error occurred. Please try again.');
    expect(component.loading).toBeFalse();
  });
});
