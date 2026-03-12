import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { NotificationService } from '../../services/notification.service';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let profileSpy: jasmine.SpyObj<ProfileService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let notifSpy: jasmine.SpyObj<NotificationService>;

  const mockProfile = {
    firstName: 'John', lastName: 'Doe', email: 'john@test.com', phone: '123',
    linkedinUrl: '', coverLetter: '', skills: ['Java'], preferences: null,
    cvRawText: '', yearsExperience: 3, roleLevel: 'MID', hasCv: false,
    emailAlerts: false, alertThreshold: 70
  };

  beforeEach(async () => {
    profileSpy = jasmine.createSpyObj('ProfileService', [
      'getProfile', 'updateProfile', 'uploadCv', 'downloadCv', 'deleteCv',
      'importLinkedIn', 'getCvAnalysis'
    ]);
    authSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'user', 'setup2fa', 'verify2fa', 'disable2fa']);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'info']);
    notifSpy = jasmine.createSpyObj('NotificationService', ['requestPermission', 'disable', 'showNotification']);
    Object.defineProperty(notifSpy, 'isSupported', { get: () => jasmine.createSpy().and.returnValue(true) });
    Object.defineProperty(notifSpy, 'isEnabled', { get: () => jasmine.createSpy().and.returnValue(false) });
    Object.defineProperty(notifSpy, 'permission', { get: () => jasmine.createSpy().and.returnValue('default') });

    profileSpy.getProfile.and.returnValue(of(mockProfile));
    profileSpy.getCvAnalysis.and.returnValue(of({ completenessScore: 80, detectedSkills: [], missingInDemandSkills: [], suggestions: [], totalActiveJobs: 100, matchingJobs: 30, yearsExperience: null, roleLevel: null }));

    await TestBed.configureTestingModule({
      imports: [ProfileComponent, HttpClientTestingModule, RouterTestingModule],
      providers: [
        { provide: ProfileService, useValue: profileSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: NotificationService, useValue: notifSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load profile on init', () => {
    fixture.detectChanges();
    expect(profileSpy.getProfile).toHaveBeenCalled();
  });

  it('should save profile', () => {
    profileSpy.updateProfile.and.returnValue(of(mockProfile));
    fixture.detectChanges();
    component.saveProfile();
    expect(profileSpy.updateProfile).toHaveBeenCalled();
  });

  it('should upload CV', () => {
    profileSpy.uploadCv.and.returnValue(of({ ...mockProfile, hasCv: true }));
    fixture.detectChanges();

    const file = new File(['cv'], 'cv.pdf', { type: 'application/pdf' });
    const event = { target: { files: [file] } };
    component.onCvUpload(event as any);

    expect(profileSpy.uploadCv).toHaveBeenCalledWith(file);
  });

  it('should delete CV', () => {
    profileSpy.deleteCv.and.returnValue(of(undefined));
    fixture.detectChanges();
    component.deleteCv();
    expect(profileSpy.deleteCv).toHaveBeenCalled();
  });

  it('should download CV', () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    profileSpy.downloadCv.and.returnValue(of(blob));
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
    spyOn(window.URL, 'revokeObjectURL');
    fixture.detectChanges();

    component.downloadCv();
    expect(profileSpy.downloadCv).toHaveBeenCalled();
  });

  it('should show success toast on profile save', () => {
    profileSpy.updateProfile.and.returnValue(of(mockProfile));
    fixture.detectChanges();
    component.saveProfile();
    expect(toastSpy.success).toHaveBeenCalled();
  });

  it('should show error toast on save failure', () => {
    profileSpy.updateProfile.and.returnValue(throwError(() => new Error('fail')));
    fixture.detectChanges();
    component.saveProfile();
    expect(toastSpy.error).toHaveBeenCalled();
  });
});
