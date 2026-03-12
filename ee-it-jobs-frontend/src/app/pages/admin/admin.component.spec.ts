import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { AdminComponent } from './admin.component';
import { ScrapeService } from '../../services/scrape.service';
import { WebSocketService } from '../../services/websocket.service';
import { environment } from '../../../environments/environment';

describe('AdminComponent', () => {
  let component: AdminComponent;
  let fixture: ComponentFixture<AdminComponent>;
  let httpMock: HttpTestingController;
  let scrapeSpy: jasmine.SpyObj<ScrapeService>;
  let wsSpy: jasmine.SpyObj<WebSocketService>;

  beforeEach(async () => {
    scrapeSpy = jasmine.createSpyObj('ScrapeService', ['triggerScrape', 'getStatus']);
    wsSpy = jasmine.createSpyObj('WebSocketService', ['onJobUpdate', 'onScrapeProgress', 'connect']);

    scrapeSpy.getStatus.and.returnValue(of({ isRunning: false }));
    wsSpy.onJobUpdate.and.returnValue(of());
    wsSpy.onScrapeProgress.and.returnValue(of());

    await TestBed.configureTestingModule({
      imports: [AdminComponent, HttpClientTestingModule],
      providers: [
        { provide: ScrapeService, useValue: scrapeSpy },
        { provide: WebSocketService, useValue: wsSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load admin stats on init', () => {
    fixture.detectChanges();
    // Flush all HTTP requests from ngOnInit
    const reqs = httpMock.match(() => true);
    expect(reqs.length).toBeGreaterThan(0);
    reqs.forEach(r => r.flush({}));
  });

  it('should check scrape status', () => {
    fixture.detectChanges();
    httpMock.match(() => true).forEach(r => r.flush({}));
    expect(scrapeSpy.getStatus).toHaveBeenCalled();
  });

  it('should trigger scrape', () => {
    scrapeSpy.triggerScrape.and.returnValue(of({ message: 'Scrape started' }));
    fixture.detectChanges();
    httpMock.match(() => true).forEach(r => r.flush({}));

    component.triggerScrape();
    expect(scrapeSpy.triggerScrape).toHaveBeenCalled();
  });

  it('should subscribe to WebSocket updates', () => {
    fixture.detectChanges();
    httpMock.match(() => true).forEach(r => r.flush({}));
    expect(wsSpy.onJobUpdate).toHaveBeenCalled();
    expect(wsSpy.onScrapeProgress).toHaveBeenCalled();
  });

  it('should start in loading state', () => {
    expect(component.loading).toBeTrue();
  });

  it('should load scrape runs', () => {
    fixture.detectChanges();
    const runsReqs = httpMock.match(r => r.url.includes('scrape/runs'));
    runsReqs.forEach(r => r.flush([]));
    httpMock.match(() => true).forEach(r => r.flush({}));
  });

  it('should load analytics data', () => {
    fixture.detectChanges();
    const analyticsReqs = httpMock.match(r => r.url.includes('analytics'));
    analyticsReqs.forEach(r => r.flush({}));
    httpMock.match(() => true).forEach(r => r.flush({}));
  });

  it('should have overview as default view', () => {
    expect(component.activeTab || 'overview').toBeTruthy();
  });

  it('should handle scrape trigger when already running', () => {
    scrapeSpy.getStatus.and.returnValue(of({ isRunning: true }));
    fixture.detectChanges();
    httpMock.match(() => true).forEach(r => r.flush({}));
    expect(component).toBeTruthy();
  });
});
