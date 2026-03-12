import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { VerifyEmailComponent } from './verify-email.component';
import { environment } from '../../../environments/environment';

describe('VerifyEmailComponent', () => {
  let component: VerifyEmailComponent;
  let fixture: ComponentFixture<VerifyEmailComponent>;
  let httpMock: HttpTestingController;

  function setup(token: string | null) {
    TestBed.configureTestingModule({
      imports: [VerifyEmailComponent, HttpClientTestingModule, RouterTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: (key: string) => token } } }
        }
      ]
    });

    fixture = TestBed.createComponent(VerifyEmailComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    if (httpMock) httpMock.verify();
  });

  it('should create', () => {
    setup('valid-token');
    httpMock.expectOne(`${environment.apiUrl}/api/auth/verify-email?token=valid-token`).flush({});
    expect(component).toBeTruthy();
  });

  it('should verify email with token', () => {
    setup('my-token');

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/verify-email?token=my-token`);
    expect(req.request.method).toBe('GET');
    req.flush({});

    expect(component.success).toBeTrue();
    expect(component.loading).toBeFalse();
  });

  it('should show success message after verification', () => {
    setup('my-token');
    httpMock.expectOne(`${environment.apiUrl}/api/auth/verify-email?token=my-token`).flush({});
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Email verified successfully');
  });

  it('should show error on verification failure', () => {
    setup('bad-token');
    httpMock.expectOne(`${environment.apiUrl}/api/auth/verify-email?token=bad-token`)
      .flush({ message: 'Token expired' }, { status: 400, statusText: 'Bad Request' });

    expect(component.success).toBeFalse();
    expect(component.error).toBe('Token expired');
  });

  it('should show error when no token provided', () => {
    setup(null);
    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Invalid verification link.');
  });
});
