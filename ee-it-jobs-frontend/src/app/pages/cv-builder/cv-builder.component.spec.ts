import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CvBuilderComponent } from './cv-builder.component';
import { ProfileService } from '../../services/profile.service';

describe('CvBuilderComponent', () => {
  let component: CvBuilderComponent;
  let fixture: ComponentFixture<CvBuilderComponent>;
  let profileSpy: jasmine.SpyObj<ProfileService>;

  beforeEach(async () => {
    profileSpy = jasmine.createSpyObj('ProfileService', ['buildCv', 'buildAndSaveCv']);

    await TestBed.configureTestingModule({
      imports: [CvBuilderComponent],
      providers: [
        { provide: ProfileService, useValue: profileSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CvBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start with empty data', () => {
    expect(component.data.fullName).toBe('');
    expect(component.data.experience.length).toBe(0);
    expect(component.data.education.length).toBe(0);
    expect(component.data.skills.length).toBe(0);
  });

  it('should add experience entry', () => {
    component.addExperience();
    expect(component.data.experience.length).toBe(1);
    expect(component.data.experience[0].company).toBe('');
  });

  it('should remove experience entry', () => {
    component.addExperience();
    component.addExperience();
    expect(component.data.experience.length).toBe(2);

    component.removeExperience(0);
    expect(component.data.experience.length).toBe(1);
  });

  it('should add education entry', () => {
    component.addEducation();
    expect(component.data.education.length).toBe(1);
    expect(component.data.education[0].institution).toBe('');
  });

  it('should remove education entry', () => {
    component.addEducation();
    component.addEducation();
    component.removeEducation(0);
    expect(component.data.education.length).toBe(1);
  });

  it('should add skill', () => {
    component.newSkill = 'TypeScript';
    component.addSkill();
    expect(component.data.skills).toContain('TypeScript');
    expect(component.newSkill).toBe('');
  });

  it('should not add duplicate skill', () => {
    component.newSkill = 'Java';
    component.addSkill();
    component.newSkill = 'Java';
    component.addSkill();
    expect(component.data.skills.filter(s => s === 'Java').length).toBe(1);
  });

  it('should not add empty skill', () => {
    component.newSkill = '   ';
    component.addSkill();
    expect(component.data.skills.length).toBe(0);
  });

  it('should remove skill', () => {
    component.data.skills = ['Java', 'Angular', 'Python'];
    component.removeSkill(1);
    expect(component.data.skills).toEqual(['Java', 'Python']);
  });

  it('should preview PDF', () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    profileSpy.buildCv.and.returnValue(of(blob));
    spyOn(window, 'open');
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
    spyOn(window.URL, 'revokeObjectURL');

    component.previewPdf();

    expect(profileSpy.buildCv).toHaveBeenCalledWith(component.data);
    expect(window.open).toHaveBeenCalledWith('blob:url', '_blank');
    expect(component.generating).toBeFalse();
  });

  it('should show error on preview failure', () => {
    profileSpy.buildCv.and.returnValue(throwError(() => new Error('fail')));

    component.previewPdf();

    expect(component.error).toBe('Failed to generate PDF');
    expect(component.generating).toBeFalse();
  });

  it('should generate and save CV', () => {
    profileSpy.buildAndSaveCv.and.returnValue(of({ hasCv: true } as any));

    component.generateAndSave();

    expect(profileSpy.buildAndSaveCv).toHaveBeenCalledWith(component.data);
    expect(component.message).toBe('CV generated and saved to your profile!');
    expect(component.generating).toBeFalse();
  });

  it('should show error on generate and save failure', () => {
    profileSpy.buildAndSaveCv.and.returnValue(throwError(() => new Error('fail')));

    component.generateAndSave();

    expect(component.error).toBe('Failed to generate CV');
  });
});
