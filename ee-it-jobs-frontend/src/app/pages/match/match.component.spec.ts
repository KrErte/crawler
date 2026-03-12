import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { MatchComponent } from './match.component';
import { MatchService } from '../../services/match.service';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth.service';

describe('MatchComponent', () => {
  let component: MatchComponent;
  let fixture: ComponentFixture<MatchComponent>;
  let matchSpy: jasmine.SpyObj<MatchService>;
  let profileSpy: jasmine.SpyObj<ProfileService>;
  let authSpy: jasmine.SpyObj<AuthService>;

  const mockResults = [
    { job: { id: 1, title: 'Dev', company: 'Acme', location: 'Tallinn', url: '', source: 'cv.ee', datePosted: null, dateScraped: null, jobType: 'Full-time', workplaceType: 'Remote', department: null, salaryText: null, descriptionSnippet: null, fullDescription: null, skills: ['Java'], salaryMin: null, salaryMax: null, salaryCurrency: null }, matchPercentage: 85, matchedSkills: ['Java'], matchExplanation: null }
  ];

  beforeEach(async () => {
    matchSpy = jasmine.createSpyObj('MatchService', ['matchJobs', 'matchFromProfile']);
    profileSpy = jasmine.createSpyObj('ProfileService', ['getProfile']);
    authSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn']);

    matchSpy.matchFromProfile.and.returnValue(of(mockResults));
    profileSpy.getProfile.and.returnValue(of({ hasCv: true } as any));
    authSpy.isLoggedIn.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [MatchComponent, RouterTestingModule],
      providers: [
        { provide: MatchService, useValue: matchSpy },
        { provide: ProfileService, useValue: profileSpy },
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MatchComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should check if user has CV on init', () => {
    fixture.detectChanges();
    expect(profileSpy.getProfile).toHaveBeenCalled();
  });

  it('should auto-match from profile when CV exists', () => {
    fixture.detectChanges();
    expect(matchSpy.matchFromProfile).toHaveBeenCalled();
    expect(component.results.length).toBe(1);
  });

  it('should match jobs with file upload', () => {
    matchSpy.matchJobs.and.returnValue(of(mockResults));
    fixture.detectChanges();

    const file = new File(['cv'], 'cv.pdf', { type: 'application/pdf' });
    component.onFileUpload({ target: { files: [file] } } as any);

    expect(matchSpy.matchJobs).toHaveBeenCalledWith(file, 20);
  });

  it('should display results with match percentages', () => {
    fixture.detectChanges();
    expect(component.results[0].matchPercentage).toBe(85);
    expect(component.results[0].matchedSkills).toContain('Java');
  });

  it('should return correct score color class', () => {
    expect(component.getScoreColor(90)).toContain('green');
    expect(component.getScoreColor(65)).toContain('yellow');
    expect(component.getScoreColor(40)).toContain('orange');
  });

  it('should toggle result details', () => {
    fixture.detectChanges();
    expect(component.expandedResult).toBeNull();
    component.toggleDetails(0);
    expect(component.expandedResult).toBe(0);
    component.toggleDetails(0);
    expect(component.expandedResult).toBeNull();
  });

  it('should not auto-match when no CV', () => {
    profileSpy.getProfile.and.returnValue(of({ hasCv: false } as any));
    fixture.detectChanges();
    expect(matchSpy.matchFromProfile).not.toHaveBeenCalled();
  });

  it('should not check profile when not logged in', () => {
    authSpy.isLoggedIn.and.returnValue(false);
    fixture.detectChanges();
    expect(profileSpy.getProfile).not.toHaveBeenCalled();
  });
});
