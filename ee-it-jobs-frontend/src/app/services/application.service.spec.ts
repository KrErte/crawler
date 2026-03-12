import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApplicationService } from './application.service';
import { environment } from '../../environments/environment';

describe('ApplicationService', () => {
  let service: ApplicationService;
  let httpMock: HttpTestingController;
  const API = `${environment.apiUrl}/api/applications`;

  const mockApp = { id: 1, jobId: 10, jobTitle: 'Dev', company: 'Acme', jobUrl: 'http://example.com', source: 'cv.ee', status: 'APPLIED', notes: '', appliedAt: '2024-01-01', updatedAt: '2024-01-01' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApplicationService]
    });
    service = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all applications', () => {
    service.getApplications().subscribe(apps => {
      expect(apps.length).toBe(1);
      expect(apps[0].jobTitle).toBe('Dev');
    });

    const req = httpMock.expectOne(API);
    expect(req.request.method).toBe('GET');
    req.flush([mockApp]);
  });

  it('should get applications filtered by status', () => {
    service.getApplications('APPLIED').subscribe(apps => {
      expect(apps.length).toBe(1);
    });

    const req = httpMock.expectOne(`${API}?status=APPLIED`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('status')).toBe('APPLIED');
    req.flush([mockApp]);
  });

  it('should create application', () => {
    const createReq = { jobId: 10, notes: 'Interested' };

    service.createApplication(createReq).subscribe(app => {
      expect(app.jobId).toBe(10);
    });

    const req = httpMock.expectOne(API);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.jobId).toBe(10);
    req.flush(mockApp);
  });

  it('should update application', () => {
    const updateReq = { status: 'INTERVIEW', notes: 'Scheduled' };

    service.updateApplication(1, updateReq).subscribe(app => {
      expect(app.id).toBe(1);
    });

    const req = httpMock.expectOne(`${API}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.status).toBe('INTERVIEW');
    req.flush({ ...mockApp, status: 'INTERVIEW' });
  });

  it('should delete application', () => {
    service.deleteApplication(1).subscribe();

    const req = httpMock.expectOne(`${API}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should export applications as blob', () => {
    const blob = new Blob(['csv data'], { type: 'text/csv' });

    service.exportApplications('csv').subscribe(result => {
      expect(result.size).toBeGreaterThan(0);
    });

    const req = httpMock.expectOne(`${API}/export?format=csv`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);
  });

  it('should check if application exists for job', () => {
    service.checkExists(10).subscribe(result => {
      expect(result.exists).toBeTrue();
    });

    const req = httpMock.expectOne(`${API}/check/10`);
    expect(req.request.method).toBe('GET');
    req.flush({ exists: true });
  });
});
