import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatchService } from './match.service';
import { environment } from '../../environments/environment';

describe('MatchService', () => {
  let service: MatchService;
  let httpMock: HttpTestingController;
  const API = `${environment.apiUrl}/api/match`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MatchService]
    });
    service = TestBed.inject(MatchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should match jobs with file upload', () => {
    const file = new File(['cv'], 'cv.pdf', { type: 'application/pdf' });
    const mockResults = [{ job: { id: 1, title: 'Dev' }, matchPercentage: 85, matchedSkills: ['Java'], matchExplanation: null }];

    service.matchJobs(file, 10).subscribe(results => {
      expect(results.length).toBe(1);
      expect(results[0].matchPercentage).toBe(85);
    });

    const req = httpMock.expectOne(`${API}?topN=10`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(mockResults);
  });

  it('should match from profile', () => {
    const mockResults = [{ job: { id: 2, title: 'Senior Dev' }, matchPercentage: 90, matchedSkills: ['Angular', 'Java'], matchExplanation: null }];

    service.matchFromProfile(5).subscribe(results => {
      expect(results.length).toBe(1);
      expect(results[0].matchPercentage).toBe(90);
    });

    const req = httpMock.expectOne(`${API}/profile?topN=5`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(mockResults);
  });

  it('should get batch match scores', () => {
    const jobIds = [1, 2, 3];
    const mockScores = [
      { jobId: 1, matchPercentage: 80, matchedSkills: ['Java'] },
      { jobId: 2, matchPercentage: 60, matchedSkills: [] },
      { jobId: 3, matchPercentage: 95, matchedSkills: ['Angular', 'TypeScript'] }
    ];

    service.getMatchScores(jobIds).subscribe(scores => {
      expect(scores.length).toBe(3);
      expect(scores[2].matchPercentage).toBe(95);
    });

    const req = httpMock.expectOne(`${API}/scores`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual([1, 2, 3]);
    req.flush(mockScores);
  });
});
