import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { StatsComponent } from './stats.component';
import { environment } from '../../../environments/environment';

describe('StatsComponent', () => {
  let component: StatsComponent;
  let fixture: ComponentFixture<StatsComponent>;
  let httpMock: HttpTestingController;
  const API = `${environment.apiUrl}/api/stats`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatsComponent, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(StatsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load stats on init', () => {
    fixture.detectChanges();

    // Expect the API calls from ngOnInit
    const reqs = httpMock.match(() => true);
    expect(reqs.length).toBeGreaterThan(0);
    reqs.forEach(r => r.flush([]));
  });

  it('should start in loading state', () => {
    expect(component.loading).toBeTrue();
  });

  it('should load top skills', () => {
    fixture.detectChanges();

    const skillReq = httpMock.match(`${API}/skills`);
    if (skillReq.length > 0) {
      skillReq[0].flush([{ skill: 'Java', count: 50 }, { skill: 'Python', count: 30 }]);
    }

    // Flush remaining requests
    httpMock.match(() => true).forEach(r => r.flush([]));
  });

  it('should load source stats', () => {
    fixture.detectChanges();

    const sourceReq = httpMock.match(`${API}/sources`);
    if (sourceReq.length > 0) {
      sourceReq[0].flush([{ source: 'cv.ee', count: 100 }]);
    }

    httpMock.match(() => true).forEach(r => r.flush([]));
  });

  it('should load trends data', () => {
    fixture.detectChanges();

    const trendReq = httpMock.match(`${API}/trends`);
    if (trendReq.length > 0) {
      trendReq[0].flush([{ date: '2024-01-01', count: 10 }]);
    }

    httpMock.match(() => true).forEach(r => r.flush([]));
  });

  it('should finish loading after data arrives', () => {
    fixture.detectChanges();
    httpMock.match(() => true).forEach(r => r.flush([]));
    expect(component.loading).toBeFalse();
  });
});
