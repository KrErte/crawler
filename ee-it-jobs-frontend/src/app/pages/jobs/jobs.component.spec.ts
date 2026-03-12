import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { JobsComponent } from './jobs.component';
import { JobService } from '../../services/job.service';
import { MatchService } from '../../services/match.service';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth.service';
import { SavedJobService } from '../../services/saved-job.service';

describe('JobsComponent', () => {
  let component: JobsComponent;
  let fixture: ComponentFixture<JobsComponent>;
  let jobSpy: jasmine.SpyObj<JobService>;
  let matchSpy: jasmine.SpyObj<MatchService>;
  let profileSpy: jasmine.SpyObj<ProfileService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let savedSpy: jasmine.SpyObj<SavedJobService>;
  let router: Router;

  const mockJobPage = {
    content: [
      { id: 1, title: 'Dev', company: 'Acme', location: 'Tallinn', url: '', source: 'cv.ee', datePosted: null, dateScraped: null, jobType: 'Full-time', workplaceType: 'Remote', department: null, salaryText: null, descriptionSnippet: null, fullDescription: null, skills: ['Java'], salaryMin: null, salaryMax: null, salaryCurrency: null },
      { id: 2, title: 'QA', company: 'Corp', location: 'Tartu', url: '', source: 'cv.ee', datePosted: null, dateScraped: null, jobType: 'Full-time', workplaceType: 'Onsite', department: null, salaryText: null, descriptionSnippet: null, fullDescription: null, skills: null, salaryMin: null, salaryMax: null, salaryCurrency: null }
    ],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 21
  };

  beforeEach(async () => {
    jobSpy = jasmine.createSpyObj('JobService', ['getJobs']);
    matchSpy = jasmine.createSpyObj('MatchService', ['getMatchScores']);
    profileSpy = jasmine.createSpyObj('ProfileService', ['getProfile']);
    authSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'isAdmin']);
    savedSpy = jasmine.createSpyObj('SavedJobService', ['loadSavedJobIds', 'isSaved', 'saveJob', 'unsaveJob']);

    jobSpy.getJobs.and.returnValue(of(mockJobPage));
    matchSpy.getMatchScores.and.returnValue(of([]));
    savedSpy.loadSavedJobIds.and.returnValue(of([]));
    savedSpy.isSaved.and.returnValue(false);
    savedSpy.saveJob.and.returnValue(of(undefined));
    savedSpy.unsaveJob.and.returnValue(of(undefined));
    authSpy.isLoggedIn.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [JobsComponent, RouterTestingModule],
      providers: [
        { provide: JobService, useValue: jobSpy },
        { provide: MatchService, useValue: matchSpy },
        { provide: ProfileService, useValue: profileSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: SavedJobService, useValue: savedSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(JobsComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load jobs on init', () => {
    fixture.detectChanges();
    expect(jobSpy.getJobs).toHaveBeenCalled();
    expect(component.jobs.length).toBe(2);
    expect(component.totalElements).toBe(2);
  });

  it('should show skeletons while loading', () => {
    component.loading = true;
    component.jobs = [];
    fixture.detectChanges();
    expect(component.skeletonItems.length).toBe(6);
  });

  it('should update filters and reload', () => {
    fixture.detectChanges();
    const filters = { search: 'java', sortBy: 'dateScraped' };
    component.onFilterChange(filters);
    expect(component.currentPage).toBe(0);
    expect(jobSpy.getJobs).toHaveBeenCalled();
  });

  it('should toggle save for saved job', () => {
    savedSpy.isSaved.and.returnValue(true);
    component.onToggleSave(1);
    expect(savedSpy.unsaveJob).toHaveBeenCalledWith(1);
  });

  it('should toggle save for unsaved job', () => {
    savedSpy.isSaved.and.returnValue(false);
    component.onToggleSave(1);
    expect(savedSpy.saveJob).toHaveBeenCalledWith(1);
  });

  it('should toggle compare selection', () => {
    component.onToggleCompare(1);
    expect(component.selectedForCompare.has(1)).toBeTrue();
    component.onToggleCompare(1);
    expect(component.selectedForCompare.has(1)).toBeFalse();
  });

  it('should limit compare to 4 jobs', () => {
    component.onToggleCompare(1);
    component.onToggleCompare(2);
    component.onToggleCompare(3);
    component.onToggleCompare(4);
    component.onToggleCompare(5);
    expect(component.selectedForCompare.size).toBe(4);
    expect(component.selectedForCompare.has(5)).toBeFalse();
  });

  it('should navigate to compare page', () => {
    spyOn(router, 'navigate');
    component.selectedForCompare.add(1);
    component.selectedForCompare.add(2);
    component.goToCompare();
    expect(router.navigate).toHaveBeenCalledWith(['/compare'], { queryParams: { ids: jasmine.any(String) } });
  });

  it('should change page', () => {
    fixture.detectChanges();
    component.changePage(2);
    expect(component.currentPage).toBe(2);
    expect(jobSpy.getJobs).toHaveBeenCalled();
  });

  it('should return null match percentage for unknown job', () => {
    expect(component.getMatchPercentage(999)).toBeNull();
  });

  it('should return empty matched skills for unknown job', () => {
    expect(component.getMatchedSkills(999)).toEqual([]);
  });

  it('should toggle infinite scroll', () => {
    fixture.detectChanges();
    expect(component.infiniteScroll).toBeFalse();
    component.toggleInfiniteScroll();
    expect(component.infiniteScroll).toBeTrue();
  });

  it('should reset new jobs banner on refresh', () => {
    fixture.detectChanges();
    component.newJobsBanner = 5;
    component.refreshJobs();
    expect(component.newJobsBanner).toBe(0);
  });

  it('should load saved job ids when logged in', () => {
    authSpy.isLoggedIn.and.returnValue(true);
    profileSpy.getProfile.and.returnValue(of({ hasCv: true } as any));
    fixture.detectChanges();
    expect(savedSpy.loadSavedJobIds).toHaveBeenCalled();
  });
});
