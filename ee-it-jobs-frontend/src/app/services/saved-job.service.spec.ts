import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SavedJobService } from './saved-job.service';
import { environment } from '../../environments/environment';

describe('SavedJobService', () => {
  let service: SavedJobService;
  let httpMock: HttpTestingController;
  const API = `${environment.apiUrl}/api/saved-jobs`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SavedJobService]
    });
    service = TestBed.inject(SavedJobService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get saved jobs', () => {
    const mockJobs = [{ id: 1, title: 'Dev', company: 'Acme' }, { id: 2, title: 'QA', company: 'Corp' }];

    service.getSavedJobs().subscribe(jobs => {
      expect(jobs.length).toBe(2);
      expect(jobs[0].title).toBe('Dev');
    });

    const req = httpMock.expectOne(API);
    expect(req.request.method).toBe('GET');
    req.flush(mockJobs);
  });

  it('should load saved job ids and populate savedIds set', () => {
    service.loadSavedJobIds().subscribe(ids => {
      expect(ids).toEqual([1, 3, 5]);
      expect(service.isSaved(1)).toBeTrue();
      expect(service.isSaved(3)).toBeTrue();
      expect(service.isSaved(2)).toBeFalse();
    });

    const req = httpMock.expectOne(`${API}/ids`);
    expect(req.request.method).toBe('GET');
    req.flush([1, 3, 5]);
  });

  it('should save job and add to savedIds', () => {
    service.saveJob(42).subscribe();

    expect(service.isSaved(42)).toBeTrue();

    const req = httpMock.expectOne(`${API}/42`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('should unsave job and remove from savedIds', () => {
    // First add it
    service.saveJob(42).subscribe();
    httpMock.expectOne(`${API}/42`).flush(null);

    expect(service.isSaved(42)).toBeTrue();

    // Then remove
    service.unsaveJob(42).subscribe();
    expect(service.isSaved(42)).toBeFalse();

    const req = httpMock.expectOne(`${API}/42`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should check isSaved returns false for unknown ids', () => {
    expect(service.isSaved(999)).toBeFalse();
  });
});
