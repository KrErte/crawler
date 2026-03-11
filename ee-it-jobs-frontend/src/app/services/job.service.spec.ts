import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JobService } from './job.service';
import { environment } from '../../environments/environment';

describe('JobService', () => {
  let service: JobService;
  let httpMock: HttpTestingController;
  const API = `${environment.apiUrl}/api/jobs`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [JobService]
    });
    service = TestBed.inject(JobService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch jobs with params', () => {
    const mockPage = {
      content: [{ id: 1, title: 'Java Dev', company: 'TestCo' }],
      totalElements: 1, totalPages: 1, number: 0, size: 20
    };

    service.getJobs({ search: 'java', page: 0, size: 20 }).subscribe(result => {
      expect(result.content.length).toBe(1);
      expect(result.content[0].title).toBe('Java Dev');
      expect(result.totalElements).toBe(1);
    });

    const req = httpMock.expectOne(r => r.url === API && r.params.get('search') === 'java');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(mockPage);
  });

  it('should handle array params (skills)', () => {
    service.getJobs({ skills: ['Java', 'Python'], page: 0, size: 10 }).subscribe();

    const req = httpMock.expectOne(r => r.url === API);
    expect(req.request.params.getAll('skills')).toEqual(['Java', 'Python']);
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
  });

  it('should skip undefined/null/empty params', () => {
    service.getJobs({ search: '', company: undefined, page: 0 }).subscribe();

    const req = httpMock.expectOne(r => r.url === API);
    expect(req.request.params.has('search')).toBeFalse();
    expect(req.request.params.has('company')).toBeFalse();
    expect(req.request.params.get('page')).toBe('0');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('should fetch single job by id', () => {
    const mockJob = { id: 42, title: 'Python Dev', company: 'TechCo' };

    service.getJob(42).subscribe(result => {
      expect(result.id).toBe(42);
      expect(result.title).toBe('Python Dev');
    });

    const req = httpMock.expectOne(`${API}/42`);
    expect(req.request.method).toBe('GET');
    req.flush(mockJob);
  });

  it('should fetch filters', () => {
    const mockFilters = {
      companies: ['Company A', 'Company B'],
      sources: ['cvkeskus', 'linkedin'],
      jobTypes: ['FULL_TIME'],
      workplaceTypes: ['REMOTE', 'HYBRID']
    };

    service.getFilters().subscribe(result => {
      expect(result.companies.length).toBe(2);
      expect(result.sources).toContain('cvkeskus');
    });

    const req = httpMock.expectOne(`${API}/filters`);
    expect(req.request.method).toBe('GET');
    req.flush(mockFilters);
  });

  it('should fetch suggestions', () => {
    service.getSuggestions('jav').subscribe(result => {
      expect(result).toEqual(['Java Developer', 'JavaScript']);
    });

    const req = httpMock.expectOne(r => r.url === `${API}/suggest` && r.params.get('q') === 'jav');
    expect(req.request.method).toBe('GET');
    req.flush(['Java Developer', 'JavaScript']);
  });
});
