import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { RecommendationsComponent } from './recommendations.component';
import { RecommendationService } from '../../services/recommendation.service';
import { SavedJobService } from '../../services/saved-job.service';

describe('RecommendationsComponent', () => {
  let component: RecommendationsComponent;
  let fixture: ComponentFixture<RecommendationsComponent>;
  let recSpy: jasmine.SpyObj<RecommendationService>;
  let savedSpy: jasmine.SpyObj<SavedJobService>;

  const mockJobs = [
    { id: 1, title: 'Dev', company: 'Acme', location: 'Tallinn', url: '', source: 'cv.ee', datePosted: null, dateScraped: null, jobType: 'Full-time', workplaceType: 'Remote', department: null, salaryText: null, descriptionSnippet: null, fullDescription: null, skills: null, salaryMin: null, salaryMax: null, salaryCurrency: null }
  ];

  beforeEach(async () => {
    recSpy = jasmine.createSpyObj('RecommendationService', ['getRecommendations']);
    savedSpy = jasmine.createSpyObj('SavedJobService', ['loadSavedJobIds', 'isSaved', 'saveJob', 'unsaveJob']);

    recSpy.getRecommendations.and.returnValue(of(mockJobs));
    savedSpy.loadSavedJobIds.and.returnValue(of([]));
    savedSpy.isSaved.and.returnValue(false);
    savedSpy.saveJob.and.returnValue(of(undefined));
    savedSpy.unsaveJob.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [RecommendationsComponent, RouterTestingModule, HttpClientTestingModule],
      providers: [
        { provide: RecommendationService, useValue: recSpy },
        { provide: SavedJobService, useValue: savedSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RecommendationsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load recommendations on init', () => {
    fixture.detectChanges();
    expect(recSpy.getRecommendations).toHaveBeenCalled();
    expect(component.jobs.length).toBe(1);
  });

  it('should show empty state', () => {
    recSpy.getRecommendations.and.returnValue(of([]));
    fixture.detectChanges();
    expect(component.jobs.length).toBe(0);
  });

  it('should load saved job ids on init', () => {
    fixture.detectChanges();
    expect(savedSpy.loadSavedJobIds).toHaveBeenCalled();
  });

  it('should toggle save job', () => {
    savedSpy.isSaved.and.returnValue(false);
    fixture.detectChanges();
    component.onToggleSave(1);
    expect(savedSpy.saveJob).toHaveBeenCalledWith(1);
  });

  it('should toggle unsave job', () => {
    savedSpy.isSaved.and.returnValue(true);
    fixture.detectChanges();
    component.onToggleSave(1);
    expect(savedSpy.unsaveJob).toHaveBeenCalledWith(1);
  });
});
