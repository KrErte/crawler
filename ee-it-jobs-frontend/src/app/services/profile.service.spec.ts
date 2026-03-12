import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProfileService } from './profile.service';
import { environment } from '../../environments/environment';

describe('ProfileService', () => {
  let service: ProfileService;
  let httpMock: HttpTestingController;
  const API = `${environment.apiUrl}/api/profile`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProfileService]
    });
    service = TestBed.inject(ProfileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get profile', () => {
    const mockProfile = { firstName: 'John', lastName: 'Doe', email: 'john@test.com', hasCv: false, skills: [], emailAlerts: false, alertThreshold: 70 };

    service.getProfile().subscribe(profile => {
      expect(profile.firstName).toBe('John');
      expect(profile.email).toBe('john@test.com');
    });

    const req = httpMock.expectOne(API);
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });

  it('should update profile', () => {
    const updateReq = { firstName: 'Jane', lastName: 'Doe', phone: '123', linkedinUrl: '', coverLetter: '' };
    const mockResponse = { ...updateReq, email: 'jane@test.com', hasCv: false, skills: [], emailAlerts: false, alertThreshold: 70 };

    service.updateProfile(updateReq).subscribe(profile => {
      expect(profile.firstName).toBe('Jane');
    });

    const req = httpMock.expectOne(API);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.firstName).toBe('Jane');
    req.flush(mockResponse);
  });

  it('should upload CV with FormData', () => {
    const file = new File(['cv content'], 'cv.pdf', { type: 'application/pdf' });
    const mockResponse = { firstName: 'John', hasCv: true };

    service.uploadCv(file).subscribe(profile => {
      expect(profile.hasCv).toBeTrue();
    });

    const req = httpMock.expectOne(`${API}/cv`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(mockResponse);
  });

  it('should download CV as blob', () => {
    const blob = new Blob(['pdf content'], { type: 'application/pdf' });

    service.downloadCv().subscribe(result => {
      expect(result.size).toBeGreaterThan(0);
    });

    const req = httpMock.expectOne(`${API}/cv`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);
  });

  it('should delete CV', () => {
    service.deleteCv().subscribe();

    const req = httpMock.expectOne(`${API}/cv`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should import LinkedIn profile', () => {
    const file = new File(['linkedin data'], 'profile.zip', { type: 'application/zip' });

    service.importLinkedIn(file).subscribe(profile => {
      expect(profile).toBeTruthy();
    });

    const req = httpMock.expectOne(`${API}/import-linkedin`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush({ firstName: 'LinkedIn', lastName: 'User' });
  });

  it('should build CV and return blob', () => {
    const cvData = { name: 'John', experience: [] };
    const blob = new Blob(['pdf'], { type: 'application/pdf' });

    service.buildCv(cvData).subscribe(result => {
      expect(result.size).toBeGreaterThan(0);
    });

    const req = httpMock.expectOne(`${API}/cv/build`);
    expect(req.request.method).toBe('POST');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);
  });

  it('should build and save CV', () => {
    const cvData = { name: 'John', experience: [] };

    service.buildAndSaveCv(cvData).subscribe(profile => {
      expect(profile.hasCv).toBeTrue();
    });

    const req = httpMock.expectOne(`${API}/cv/build-and-save`);
    expect(req.request.method).toBe('POST');
    req.flush({ hasCv: true, firstName: 'John' });
  });

  it('should get CV analysis', () => {
    const mockAnalysis = { completenessScore: 85, detectedSkills: ['Java', 'Angular'], missingInDemandSkills: ['React'], suggestions: ['Add projects'], totalActiveJobs: 100, matchingJobs: 30 };

    service.getCvAnalysis().subscribe(analysis => {
      expect(analysis.completenessScore).toBe(85);
      expect(analysis.detectedSkills).toContain('Java');
    });

    const req = httpMock.expectOne(`${API}/cv-analysis`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAnalysis);
  });
});
