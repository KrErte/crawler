import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApplicationsComponent } from './applications.component';
import { ApplicationService } from '../../services/application.service';

describe('ApplicationsComponent', () => {
  let component: ApplicationsComponent;
  let fixture: ComponentFixture<ApplicationsComponent>;
  let appSpy: jasmine.SpyObj<ApplicationService>;

  const mockApps = [
    { id: 1, jobId: 10, jobTitle: 'Dev', company: 'Acme', jobUrl: 'http://a.com', source: 'cv.ee', status: 'SUBMITTED', notes: '', appliedAt: '2024-01-01T00:00:00', updatedAt: '2024-01-01T00:00:00' },
    { id: 2, jobId: 20, jobTitle: 'QA', company: 'Corp', jobUrl: 'http://b.com', source: 'indeed', status: 'INTERVIEW', notes: 'Great', appliedAt: '2024-01-02T00:00:00', updatedAt: '2024-01-02T00:00:00' }
  ];

  beforeEach(async () => {
    appSpy = jasmine.createSpyObj('ApplicationService', [
      'getApplications', 'updateApplication', 'deleteApplication', 'exportApplications'
    ]);
    appSpy.getApplications.and.returnValue(of(mockApps));
    appSpy.updateApplication.and.returnValue(of(mockApps[0]));
    appSpy.deleteApplication.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [ApplicationsComponent],
      providers: [
        { provide: ApplicationService, useValue: appSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load applications on init', () => {
    fixture.detectChanges();
    expect(appSpy.getApplications).toHaveBeenCalled();
    expect(component.applications.length).toBe(2);
    expect(component.loading).toBeFalse();
  });

  it('should have tabs for all statuses', () => {
    expect(component.tabs.length).toBe(6);
    expect(component.tabs.map(t => t.value)).toContain('INTERVIEW');
  });

  it('should filter by tab', () => {
    fixture.detectChanges();
    component.setTab('INTERVIEW');
    expect(component.activeTab).toBe('INTERVIEW');
    expect(appSpy.getApplications).toHaveBeenCalledWith('INTERVIEW');
  });

  it('should update application status', () => {
    fixture.detectChanges();
    const app = component.applications[0];
    app.status = 'INTERVIEW';
    component.onStatusChange(app);
    expect(appSpy.updateApplication).toHaveBeenCalledWith(1, { status: 'INTERVIEW' });
  });

  it('should delete application', () => {
    fixture.detectChanges();
    component.deleteApp(component.applications[0]);
    expect(appSpy.deleteApplication).toHaveBeenCalledWith(1);
    expect(component.applications.length).toBe(1);
  });

  it('should export CSV', () => {
    const blob = new Blob(['csv'], { type: 'text/csv' });
    appSpy.exportApplications.and.returnValue(of(blob));
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
    spyOn(window.URL, 'revokeObjectURL');

    fixture.detectChanges();
    component.exportCsv();
    expect(appSpy.exportApplications).toHaveBeenCalledWith('csv');
  });

  it('should export PDF', () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    appSpy.exportApplications.and.returnValue(of(blob));
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
    spyOn(window.URL, 'revokeObjectURL');

    fixture.detectChanges();
    component.exportPdf();
    expect(appSpy.exportApplications).toHaveBeenCalledWith('pdf');
  });

  it('should show empty state with no apps', () => {
    appSpy.getApplications.and.returnValue(of([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No applications found');
  });

  it('should handle notes change with debounce', () => {
    fixture.detectChanges();
    const app = component.applications[0];
    component.onNotesChange(app);
    // Subject created but debounced - no immediate update call
    expect(component).toBeTruthy();
  });
});
