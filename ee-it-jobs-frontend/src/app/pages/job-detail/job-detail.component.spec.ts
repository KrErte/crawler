import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { JobDetailComponent } from './job-detail.component';
import { JobService } from '../../services/job.service';
import { ApplicationService } from '../../services/application.service';
import { AuthService } from '../../services/auth.service';

describe('JobDetailComponent', () => {
  let component: JobDetailComponent;
  let fixture: ComponentFixture<JobDetailComponent>;
  let jobSpy: jasmine.SpyObj<JobService>;
  let appSpy: jasmine.SpyObj<ApplicationService>;
  let authSpy: jasmine.SpyObj<AuthService>;

  const mockJob = {
    id: 1, title: 'Senior Dev', company: 'Acme', location: 'Tallinn', url: 'http://example.com',
    source: 'cv.ee', datePosted: null, dateScraped: '2024-01-01', jobType: 'Full-time',
    workplaceType: 'REMOTE', department: 'Engineering', salaryText: '3000-5000 EUR',
    descriptionSnippet: 'Great job', fullDescription: 'Full description here',
    skills: ['Java', 'Spring'], salaryMin: 3000, salaryMax: 5000, salaryCurrency: 'EUR'
  };

  beforeEach(async () => {
    jobSpy = jasmine.createSpyObj('JobService', ['getJob', 'translateJob']);
    appSpy = jasmine.createSpyObj('ApplicationService', ['checkExists', 'createApplication']);
    authSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn']);

    jobSpy.getJob.and.returnValue(of(mockJob));
    appSpy.checkExists.and.returnValue(of({ exists: false }));
    authSpy.isLoggedIn.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [JobDetailComponent, RouterTestingModule],
      providers: [
        { provide: JobService, useValue: jobSpy },
        { provide: ApplicationService, useValue: appSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' } } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(JobDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load job by id on init', () => {
    fixture.detectChanges();
    expect(jobSpy.getJob).toHaveBeenCalledWith(1);
    expect(component.job?.title).toBe('Senior Dev');
    expect(component.loading).toBeFalse();
  });

  it('should show skeleton while loading', () => {
    expect(component.loading).toBeTrue();
  });

  it('should display job details', () => {
    fixture.detectChanges();
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('Senior Dev');
    expect(el.textContent).toContain('Acme');
  });

  it('should check application exists for logged-in user', () => {
    authSpy.isLoggedIn.and.returnValue(true);
    fixture.detectChanges();
    expect(appSpy.checkExists).toHaveBeenCalledWith(1);
  });

  it('should track application', () => {
    appSpy.createApplication.and.returnValue(of({ id: 1 } as any));
    fixture.detectChanges();
    component.trackApplication();
    expect(appSpy.createApplication).toHaveBeenCalledWith({ jobId: 1 });
    expect(component.applied).toBeTrue();
  });

  it('should show duplicate warning when already applied', () => {
    fixture.detectChanges();
    component.applied = true;
    component.trackApplication();
    expect(component.showDuplicateWarning).toBeTrue();
  });

  it('should translate description', () => {
    jobSpy.translateJob.and.returnValue(of({ title: 'Translated', description: 'Tõlgitud', detectedLang: 'en' }));
    fixture.detectChanges();
    component.translateDescription();
    expect(jobSpy.translateJob).toHaveBeenCalled();
    expect(component.translatedDesc).toBe('Tõlgitud');
    expect(component.showTranslation).toBeTrue();
  });

  it('should toggle translation visibility', () => {
    fixture.detectChanges();
    component.translatedDesc = 'Existing translation';
    component.showTranslation = true;
    component.translateDescription();
    expect(component.showTranslation).toBeFalse();
  });

  it('should return correct workplace class', () => {
    fixture.detectChanges();
    expect(component.workplaceClass).toContain('green');
    component.job!.workplaceType = 'HYBRID';
    expect(component.workplaceClass).toContain('orange');
    component.job!.workplaceType = 'ONSITE';
    expect(component.workplaceClass).toContain('pink');
  });
});
