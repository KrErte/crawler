import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { JobCompareComponent } from './job-compare.component';
import { JobService } from '../../services/job.service';

describe('JobCompareComponent', () => {
  let component: JobCompareComponent;
  let fixture: ComponentFixture<JobCompareComponent>;
  let jobSpy: jasmine.SpyObj<JobService>;

  const mockJob1 = { id: 1, title: 'Dev', company: 'Acme', location: 'Tallinn', url: '', source: 'cv.ee', datePosted: null, dateScraped: null, jobType: 'Full-time', workplaceType: 'Remote', department: 'Engineering', salaryText: '3000 EUR', descriptionSnippet: null, fullDescription: null, skills: ['Java', 'Spring'], salaryMin: 3000, salaryMax: null, salaryCurrency: 'EUR' };
  const mockJob2 = { id: 2, title: 'QA', company: 'Corp', location: 'Tartu', url: '', source: 'indeed', datePosted: null, dateScraped: null, jobType: 'Part-time', workplaceType: 'Onsite', department: 'QA', salaryText: '2500 EUR', descriptionSnippet: null, fullDescription: null, skills: ['Selenium'], salaryMin: 2500, salaryMax: null, salaryCurrency: 'EUR' };

  beforeEach(async () => {
    jobSpy = jasmine.createSpyObj('JobService', ['getJob']);

    await TestBed.configureTestingModule({
      imports: [JobCompareComponent, RouterTestingModule],
      providers: [
        { provide: JobService, useValue: jobSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (key: string) => '1,2' } } } }
      ]
    }).compileComponents();

    jobSpy.getJob.and.callFake((id: number) => {
      if (id === 1) return of(mockJob1);
      return of(mockJob2);
    });

    fixture = TestBed.createComponent(JobCompareComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should parse job IDs from query params', () => {
    fixture.detectChanges();
    expect(jobSpy.getJob).toHaveBeenCalledTimes(2);
  });

  it('should load jobs for comparison', () => {
    fixture.detectChanges();
    expect(component.jobs.length).toBe(2);
    expect(component.jobs[0].title).toBe('Dev');
    expect(component.jobs[1].title).toBe('QA');
  });

  it('should display comparison table', () => {
    fixture.detectChanges();
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('Dev');
    expect(el.textContent).toContain('QA');
  });

  it('should handle empty IDs', async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [JobCompareComponent, RouterTestingModule],
      providers: [
        { provide: JobService, useValue: jobSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => '' } } } }
      ]
    }).compileComponents();

    const f = TestBed.createComponent(JobCompareComponent);
    f.detectChanges();
    expect(f.componentInstance.jobs.length).toBe(0);
  });

  it('should stop loading after jobs loaded', () => {
    fixture.detectChanges();
    expect(component.loading).toBeFalse();
  });
});
