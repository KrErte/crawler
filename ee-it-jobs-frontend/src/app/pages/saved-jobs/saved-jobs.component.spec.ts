import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { SavedJobsComponent } from './saved-jobs.component';
import { SavedJobService } from '../../services/saved-job.service';

describe('SavedJobsComponent', () => {
  let component: SavedJobsComponent;
  let fixture: ComponentFixture<SavedJobsComponent>;
  let savedSpy: jasmine.SpyObj<SavedJobService>;

  const mockJobs = [
    { id: 1, title: 'Dev', company: 'Acme', location: 'Tallinn', url: '', source: 'cv.ee', datePosted: null, dateScraped: null, jobType: 'Full-time', workplaceType: 'Remote', department: null, salaryText: null, descriptionSnippet: null, fullDescription: null, skills: null, salaryMin: null, salaryMax: null, salaryCurrency: null },
    { id: 2, title: 'QA', company: 'Corp', location: 'Tartu', url: '', source: 'cv.ee', datePosted: null, dateScraped: null, jobType: 'Full-time', workplaceType: 'Onsite', department: null, salaryText: null, descriptionSnippet: null, fullDescription: null, skills: null, salaryMin: null, salaryMax: null, salaryCurrency: null }
  ];

  beforeEach(async () => {
    savedSpy = jasmine.createSpyObj('SavedJobService', ['getSavedJobs', 'unsaveJob', 'isSaved']);
    savedSpy.getSavedJobs.and.returnValue(of(mockJobs));
    savedSpy.isSaved.and.returnValue(true);
    savedSpy.unsaveJob.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [SavedJobsComponent, RouterTestingModule],
      providers: [
        { provide: SavedJobService, useValue: savedSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SavedJobsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load saved jobs on init', () => {
    fixture.detectChanges();
    expect(savedSpy.getSavedJobs).toHaveBeenCalled();
    expect(component.jobs.length).toBe(2);
    expect(component.loading).toBeFalse();
  });

  it('should show empty state when no saved jobs', () => {
    savedSpy.getSavedJobs.and.returnValue(of([]));
    fixture.detectChanges();
    expect(component.jobs.length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('No saved jobs yet');
  });

  it('should unsave job and remove from list', () => {
    fixture.detectChanges();
    component.onToggleSave(1);
    expect(savedSpy.unsaveJob).toHaveBeenCalledWith(1);
    expect(component.jobs.length).toBe(1);
    expect(component.jobs[0].id).toBe(2);
  });

  it('should start in loading state', () => {
    expect(component.loading).toBeTrue();
  });
});
