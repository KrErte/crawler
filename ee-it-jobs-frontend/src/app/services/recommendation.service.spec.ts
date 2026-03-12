import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RecommendationService } from './recommendation.service';
import { environment } from '../../environments/environment';

describe('RecommendationService', () => {
  let service: RecommendationService;
  let httpMock: HttpTestingController;
  const API = `${environment.apiUrl}/api/recommendations`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RecommendationService]
    });
    service = TestBed.inject(RecommendationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get recommendations with default limit', () => {
    const mockJobs = [{ id: 1, title: 'Dev', company: 'Acme' }];

    service.getRecommendations().subscribe(jobs => {
      expect(jobs.length).toBe(1);
      expect(jobs[0].title).toBe('Dev');
    });

    const req = httpMock.expectOne(`${API}?limit=10`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('limit')).toBe('10');
    req.flush(mockJobs);
  });

  it('should get recommendations with custom limit', () => {
    service.getRecommendations(5).subscribe(jobs => {
      expect(jobs.length).toBe(0);
    });

    const req = httpMock.expectOne(`${API}?limit=5`);
    expect(req.request.params.get('limit')).toBe('5');
    req.flush([]);
  });
});
