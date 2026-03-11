import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JobFiltersComponent } from './job-filters.component';
import { environment } from '../../../environments/environment';

describe('JobFiltersComponent', () => {
  let component: JobFiltersComponent;
  let fixture: ComponentFixture<JobFiltersComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobFiltersComponent, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(JobFiltersComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    // Flush the getFilters call from ngOnInit
    const filterReq = httpMock.expectOne(`${environment.apiUrl}/api/jobs/filters`);
    filterReq.flush({
      companies: ['Company A', 'Company B'],
      sources: ['cvkeskus', 'linkedin'],
      jobTypes: ['FULL_TIME'],
      workplaceTypes: ['REMOTE', 'HYBRID']
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load filters on init', () => {
    expect(component.filters).toBeTruthy();
    expect(component.filters!.companies.length).toBe(2);
    expect(component.filters!.sources).toContain('cvkeskus');
  });

  it('should emit filterChange on company select', fakeAsync(() => {
    spyOn(component.filterChange, 'emit');

    component.company = 'Company A';
    component.emitChange();
    tick();

    expect(component.filterChange.emit).toHaveBeenCalledWith(jasmine.objectContaining({
      company: 'Company A'
    }));
  }));

  it('should emit filterChange on workplace change', fakeAsync(() => {
    spyOn(component.filterChange, 'emit');

    component.workplaceType = 'REMOTE';
    component.emitChange();
    tick();

    expect(component.filterChange.emit).toHaveBeenCalledWith(jasmine.objectContaining({
      workplaceType: 'REMOTE'
    }));
  }));

  it('should toggle skills', () => {
    expect(component.selectedSkills.has('Java')).toBeFalse();

    component.toggleSkill('Java');
    expect(component.selectedSkills.has('Java')).toBeTrue();

    component.toggleSkill('Java');
    expect(component.selectedSkills.has('Java')).toBeFalse();
  });

  it('should include skills in emitted filter', fakeAsync(() => {
    spyOn(component.filterChange, 'emit');

    component.toggleSkill('Java');
    component.toggleSkill('Python');
    component.emitChange();
    tick();

    const emitted = (component.filterChange.emit as jasmine.Spy).calls.mostRecent().args[0];
    expect(emitted.skills).toContain('Java');
    expect(emitted.skills).toContain('Python');
  }));

  it('should toggle advanced filters visibility', () => {
    expect(component.showAdvanced).toBeFalse();
    component.showAdvanced = !component.showAdvanced;
    expect(component.showAdvanced).toBeTrue();
  });

  it('should select suggestion and emit change', fakeAsync(() => {
    spyOn(component.filterChange, 'emit');

    component.selectSuggestion('Java Developer');
    tick();

    expect(component.search).toBe('Java Developer');
    expect(component.showSuggestions).toBeFalse();
    expect(component.filterChange.emit).toHaveBeenCalled();
  }));

  it('should emit default sortBy as dateScraped', fakeAsync(() => {
    spyOn(component.filterChange, 'emit');

    component.emitChange();
    tick();

    expect(component.filterChange.emit).toHaveBeenCalledWith(jasmine.objectContaining({
      sortBy: 'dateScraped',
      sortDir: 'DESC'
    }));
  }));
});
