import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { LandingComponent } from './landing.component';
import { environment } from '../../../environments/environment';

describe('LandingComponent', () => {
  let component: LandingComponent;
  let fixture: ComponentFixture<LandingComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingComponent, HttpClientTestingModule, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(LandingComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to 500 active jobs', () => {
    expect(component.activeJobs).toBe(500);
  });

  it('should update active jobs from API', () => {
    fixture.detectChanges(); // triggers ngOnInit

    const req = httpMock.expectOne(`${environment.apiUrl}/api/stats/overview`);
    req.flush({ activeJobs: 1234 });

    expect(component.activeJobs).toBe(1234);
  });

  it('should keep default on API error', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/stats/overview`);
    req.error(new ProgressEvent('error'));

    expect(component.activeJobs).toBe(500);
  });

  it('should contain navigation links', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/api/stats/overview`).flush({ activeJobs: 100 });

    const el = fixture.nativeElement;
    expect(el.textContent).toContain('Browse Jobs');
    expect(el.textContent).toContain('How It Works');
  });
});
