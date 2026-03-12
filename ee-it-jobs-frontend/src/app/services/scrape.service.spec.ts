import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ScrapeService } from './scrape.service';
import { environment } from '../../environments/environment';

describe('ScrapeService', () => {
  let service: ScrapeService;
  let httpMock: HttpTestingController;
  const API = `${environment.apiUrl}/api/scrape`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ScrapeService]
    });
    service = TestBed.inject(ScrapeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should trigger scrape', () => {
    const mockResponse = { message: 'Scrape started', totalScrapers: 22 };

    service.triggerScrape().subscribe(res => {
      expect(res.message).toBe('Scrape started');
    });

    const req = httpMock.expectOne(`${API}/trigger`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should get scrape status', () => {
    const mockStatus = { isRunning: true, progress: 50, currentScraper: 'cv.ee' };

    service.getStatus().subscribe(status => {
      expect(status.isRunning).toBeTrue();
      expect(status.progress).toBe(50);
    });

    const req = httpMock.expectOne(`${API}/status`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStatus);
  });
});
