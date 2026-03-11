import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { JobCardComponent } from './job-card.component';
import { Job } from '../../models/job.model';

describe('JobCardComponent', () => {
  let component: JobCardComponent;
  let fixture: ComponentFixture<JobCardComponent>;

  const mockJob: Job = {
    id: 1,
    title: 'Senior Java Developer',
    company: 'TestCo',
    location: 'Tallinn',
    url: 'https://example.com/job/1',
    source: 'cvkeskus',
    datePosted: '2025-01-15',
    dateScraped: '2025-01-16',
    jobType: 'FULL_TIME',
    workplaceType: 'REMOTE',
    department: null,
    salaryText: '4000-6000 EUR',
    descriptionSnippet: 'Looking for experienced Java developer',
    fullDescription: null,
    skills: ['Java', 'Spring', 'PostgreSQL'],
    salaryMin: 4000,
    salaryMax: 6000,
    salaryCurrency: 'EUR'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobCardComponent, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(JobCardComponent);
    component = fixture.componentInstance;
    component.job = mockJob;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display job title', () => {
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('Senior Java Developer');
  });

  it('should display company name', () => {
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('TestCo');
  });

  it('should display location', () => {
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('Tallinn');
  });

  it('should display salary text', () => {
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('4000-6000 EUR');
  });

  it('should display source and date', () => {
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('cvkeskus');
    expect(el.textContent).toContain('2025-01-16');
  });

  it('should display workplace type badge', () => {
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('REMOTE');
  });

  it('should return correct workplaceClass for REMOTE', () => {
    expect(component.workplaceClass).toContain('bg-green-900');
  });

  it('should return correct workplaceClass for HYBRID', () => {
    component.job = { ...mockJob, workplaceType: 'HYBRID' };
    expect(component.workplaceClass).toContain('bg-orange-900');
  });

  it('should return correct workplaceClass for ONSITE', () => {
    component.job = { ...mockJob, workplaceType: 'ONSITE' };
    expect(component.workplaceClass).toContain('bg-pink-900');
  });

  it('should emit toggleSave on save button click', () => {
    component.showSave = true;
    fixture.detectChanges();

    spyOn(component.toggleSave, 'emit');
    const event = new Event('click');
    component.onToggleSave(event);

    expect(component.toggleSave.emit).toHaveBeenCalledWith(1);
  });

  it('should emit toggleCompare on compare change', () => {
    component.showCompare = true;
    fixture.detectChanges();

    spyOn(component.toggleCompare, 'emit');
    const event = new Event('change');
    component.onToggleCompare(event);

    expect(component.toggleCompare.emit).toHaveBeenCalledWith(1);
  });

  it('should show match badge when matchPercentage is set', () => {
    component.matchPercentage = 85;
    fixture.detectChanges();
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('85% match');
  });

  it('should return correct matchBadgeClass for high match', () => {
    component.matchPercentage = 85;
    expect(component.matchBadgeClass).toContain('bg-green-500');
  });

  it('should return correct matchBadgeClass for medium match', () => {
    component.matchPercentage = 55;
    expect(component.matchBadgeClass).toContain('bg-yellow-500');
  });

  it('should return correct matchBadgeClass for low match', () => {
    component.matchPercentage = 25;
    expect(component.matchBadgeClass).toContain('bg-orange-500');
  });

  it('should display matched skills', () => {
    component.matchedSkills = ['Java', 'Spring'];
    fixture.detectChanges();
    const el = fixture.nativeElement;
    expect(el.textContent).toContain('Java');
    expect(el.textContent).toContain('Spring');
  });
});
