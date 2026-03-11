import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { Profile, CvAnalysis } from '../../models/profile.model';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [FormsModule, SkeletonComponent],
  template: `
    <div class="max-w-3xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-white mb-6">Profile</h1>
      @if (loading) {
        <div class="card space-y-4">
          <app-skeleton width="200px" height="1.5rem" extraClass="mb-2" />
          <div class="grid grid-cols-2 gap-4">
            <div>
              <app-skeleton width="80px" height="0.875rem" extraClass="mb-1" />
              <app-skeleton width="100%" height="2.5rem" />
            </div>
            <div>
              <app-skeleton width="80px" height="0.875rem" extraClass="mb-1" />
              <app-skeleton width="100%" height="2.5rem" />
            </div>
          </div>
          <div>
            <app-skeleton width="60px" height="0.875rem" extraClass="mb-1" />
            <app-skeleton width="100%" height="2.5rem" />
          </div>
          <div>
            <app-skeleton width="100px" height="0.875rem" extraClass="mb-1" />
            <app-skeleton width="100%" height="2.5rem" />
          </div>
          <div>
            <app-skeleton width="100px" height="0.875rem" extraClass="mb-1" />
            <app-skeleton width="100%" height="6rem" />
          </div>
          <app-skeleton width="130px" height="2.5rem" />
        </div>
      } @else if (profile) {
        <form (ngSubmit)="onSave()" class="space-y-6">
          @if (message) {
            <div class="bg-green-900/30 border border-green-800 text-green-400 px-4 py-2 rounded-lg">{{ message }}</div>
          }
          <div class="card space-y-4">
            <h2 class="text-lg font-semibold text-white">Personal Information</h2>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1">First Name</label>
                <input type="text" [(ngModel)]="profile.firstName" name="firstName" class="w-full" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1">Last Name</label>
                <input type="text" [(ngModel)]="profile.lastName" name="lastName" class="w-full" />
              </div>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1">Phone</label>
              <input type="tel" [(ngModel)]="profile.phone" name="phone" class="w-full" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1">LinkedIn URL</label>
              <input type="url" [(ngModel)]="profile.linkedinUrl" name="linkedinUrl" class="w-full" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1">Cover Letter</label>
              <textarea [(ngModel)]="profile.coverLetter" name="coverLetter" rows="5" class="w-full"></textarea>
            </div>
            <button type="submit" class="btn-primary" [disabled]="saving">
              {{ saving ? 'Saving...' : 'Save Profile' }}
            </button>
          </div>
        </form>

        <div class="card mt-6">
          <h2 class="text-lg font-semibold text-white mb-4">Email Alerts</h2>
          <div class="flex items-center gap-4 mb-4">
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" [(ngModel)]="profile.emailAlerts" (ngModelChange)="onAlertChange()"
                class="w-4 h-4 rounded" />
              <span class="text-gray-300">Receive daily job alerts via email</span>
            </label>
          </div>
          @if (profile.emailAlerts) {
            <div>
              <label class="block text-sm text-gray-400 mb-2">
                Minimum match threshold: {{ profile.alertThreshold }}%
              </label>
              <input type="range" min="30" max="95" step="5"
                [(ngModel)]="profile.alertThreshold" (ngModelChange)="onAlertChange()"
                class="w-full" />
              <div class="flex justify-between text-xs text-gray-600 mt-1">
                <span>30%</span><span>95%</span>
              </div>
            </div>
          }
        </div>

        <div class="card mt-6">
          <h2 class="text-lg font-semibold text-white mb-4">Import from LinkedIn</h2>
          <p class="text-gray-400 text-sm mb-3">Upload your LinkedIn PDF to import skills and experience.</p>
          <input type="file" accept=".pdf" (change)="onLinkedInImport($event)" class="hidden" #linkedinInput />
          <button (click)="linkedinInput.click()" class="btn-secondary text-sm" [disabled]="importingLinkedIn">
            {{ importingLinkedIn ? 'Importing...' : 'Import LinkedIn PDF' }}
          </button>
        </div>

        <div class="card mt-6">
          <h2 class="text-lg font-semibold text-white mb-4">CV / Resume</h2>
          @if (profile.hasCv) {
            <div class="flex items-center gap-4 mb-4">
              <span class="text-green-400">CV uploaded</span>
              @if (profile.skills && profile.skills.length > 0) {
                <span class="text-gray-500">{{ profile.skills.length }} skills detected</span>
              }
              @if (profile.yearsExperience) {
                <span class="text-gray-500">{{ profile.yearsExperience }}+ years exp.</span>
              }
            </div>
            @if (profile.skills && profile.skills.length > 0) {
              <div class="flex flex-wrap gap-2 mb-4">
                @for (skill of profile.skills; track skill) {
                  <span class="bg-accent/10 text-accent px-2 py-1 rounded text-sm">{{ skill }}</span>
                }
              </div>
            }
            <div class="flex gap-3">
              <button (click)="downloadCv()" class="btn-secondary text-sm">Download CV</button>
              <button (click)="deleteCv()" class="text-red-400 hover:text-red-300 text-sm">Delete CV</button>
            </div>
          } @else {
            <div class="border-2 border-dashed border-dark-600 rounded-lg p-8 text-center">
              <p class="text-gray-500 mb-4">Upload your CV (PDF) for AI-powered job matching</p>
              <input type="file" accept=".pdf" (change)="onFileSelect($event)"
                class="hidden" #fileInput />
              <button (click)="fileInput.click()" class="btn-primary" [disabled]="uploading">
                {{ uploading ? 'Uploading...' : 'Upload PDF' }}
              </button>
            </div>
          }
        </div>

        <!-- CV Analysis -->
        @if (cvAnalysis) {
          <div class="card mt-6">
            <h2 class="text-lg font-semibold text-white mb-4">CV Analysis</h2>
            <div class="flex items-center gap-6 mb-6">
              <!-- Completeness ring -->
              <div class="relative w-20 h-20 flex-shrink-0">
                <svg viewBox="0 0 36 36" class="w-20 h-20 transform -rotate-90">
                  <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    fill="none" stroke="#1e293b" stroke-width="3" />
                  <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    fill="none" stroke="#38bdf8" stroke-width="3"
                    [attr.stroke-dasharray]="cvAnalysis.completenessScore + ', 100'" />
                </svg>
                <span class="absolute inset-0 flex items-center justify-center text-white font-bold text-sm">
                  {{ cvAnalysis.completenessScore }}%
                </span>
              </div>
              <div>
                <p class="text-white font-medium">Completeness Score</p>
                <p class="text-gray-500 text-sm">{{ cvAnalysis.matchingJobs }} of {{ cvAnalysis.totalActiveJobs }} jobs match your skills</p>
                @if (cvAnalysis.roleLevel) {
                  <p class="text-gray-500 text-sm mt-1">Level: <span class="text-accent capitalize">{{ cvAnalysis.roleLevel }}</span>
                    @if (cvAnalysis.yearsExperience) {
                      <span> ({{ cvAnalysis.yearsExperience }}+ years)</span>
                    }
                  </p>
                }
              </div>
            </div>

            @if (cvAnalysis.missingInDemandSkills.length > 0) {
              <div class="mb-4">
                <h3 class="text-sm font-medium text-gray-400 mb-2">Missing In-Demand Skills</h3>
                <div class="flex flex-wrap gap-2">
                  @for (skill of cvAnalysis.missingInDemandSkills; track skill) {
                    <span class="bg-red-900/20 text-red-400 border border-red-800/30 px-2 py-1 rounded text-sm">{{ skill }}</span>
                  }
                </div>
              </div>
            }

            @if (cvAnalysis.suggestions.length > 0) {
              <div>
                <h3 class="text-sm font-medium text-gray-400 mb-2">Suggestions</h3>
                <ul class="space-y-2">
                  @for (suggestion of cvAnalysis.suggestions; track suggestion) {
                    <li class="flex items-start gap-2 text-sm text-gray-300">
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-4 h-4 text-accent flex-shrink-0 mt-0.5">
                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z" clip-rule="evenodd" />
                      </svg>
                      {{ suggestion }}
                    </li>
                  }
                </ul>
              </div>
            }
          </div>
        } @else if (profile.hasCv && cvAnalysisLoading) {
          <div class="card mt-6">
            <h2 class="text-lg font-semibold text-white mb-4">CV Analysis</h2>
            <div class="flex items-center gap-4">
              <app-skeleton width="80px" height="80px" />
              <div class="flex-1">
                <app-skeleton width="60%" height="1rem" extraClass="mb-2" />
                <app-skeleton width="40%" height="0.875rem" />
              </div>
            </div>
          </div>
        }

        <!-- Browser Notifications -->
        @if (notificationService.isSupported()) {
          <div class="card mt-6">
            <h2 class="text-lg font-semibold text-white mb-4">Browser Notifications</h2>
            <p class="text-gray-400 text-sm mb-3">Receive push notifications when new matching jobs are found.</p>
            @if (notificationService.isEnabled()) {
              <div class="flex items-center gap-4">
                <span class="text-green-400 text-sm">Notifications enabled</span>
                <button (click)="disableNotifications()" class="text-red-400 hover:text-red-300 text-sm">Disable</button>
              </div>
            } @else {
              @if (notificationService.permission() === 'denied') {
                <p class="text-yellow-400 text-sm">Notifications are blocked by your browser. Please update your browser settings to allow notifications.</p>
              } @else {
                <button (click)="enableNotifications()" class="btn-secondary text-sm">
                  Enable Browser Notifications
                </button>
              }
            }
          </div>
        }

        <!-- Two-Factor Authentication -->
        <div class="card mt-6">
          <h2 class="text-lg font-semibold text-white mb-4">Two-Factor Authentication</h2>
          @if (totpEnabled) {
            <div class="flex items-center gap-4">
              <span class="text-green-400">2FA is enabled</span>
              <button (click)="disable2fa()" class="text-red-400 hover:text-red-300 text-sm" [disabled]="twoFactorLoading">
                Disable 2FA
              </button>
            </div>
            @if (showDisable2faInput) {
              <div class="mt-3 flex items-center gap-3">
                <input type="text" [(ngModel)]="disable2faCode" placeholder="Enter 2FA code" class="w-40" maxlength="6" />
                <button (click)="confirmDisable2fa()" class="btn-secondary text-sm" [disabled]="twoFactorLoading">Confirm</button>
              </div>
            }
          } @else {
            @if (!qrCodeUri) {
              <p class="text-gray-400 text-sm mb-3">Add an extra layer of security to your account.</p>
              <button (click)="setup2fa()" class="btn-secondary text-sm" [disabled]="twoFactorLoading">
                {{ twoFactorLoading ? 'Setting up...' : 'Enable 2FA' }}
              </button>
            } @else {
              <p class="text-gray-400 text-sm mb-3">Scan this QR code with your authenticator app:</p>
              <img [src]="qrCodeUri" alt="2FA QR Code" class="w-48 h-48 mb-4 bg-white p-2 rounded" />
              <div class="flex items-center gap-3">
                <input type="text" [(ngModel)]="verify2faCode" placeholder="Enter code" class="w-40" maxlength="6" />
                <button (click)="verify2fa()" class="btn-primary text-sm" [disabled]="twoFactorLoading">Verify & Enable</button>
              </div>
              @if (twoFactorError) {
                <p class="text-red-400 text-sm mt-2">{{ twoFactorError }}</p>
              }
            }
          }
        </div>
      }
    </div>
  `
})
export class ProfileComponent implements OnInit {
  profile: Profile | null = null;
  loading = true;
  saving = false;
  uploading = false;
  importingLinkedIn = false;
  message = '';
  cvAnalysis: CvAnalysis | null = null;
  cvAnalysisLoading = false;

  // 2FA
  totpEnabled = false;
  qrCodeUri = '';
  verify2faCode = '';
  disable2faCode = '';
  twoFactorLoading = false;
  twoFactorError = '';
  showDisable2faInput = false;

  constructor(
    private profileService: ProfileService,
    private authService: AuthService,
    public notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.profileService.getProfile().subscribe({
      next: (p) => {
        this.profile = p;
        this.totpEnabled = (p as any).totpEnabled || false;
        this.loading = false;
        if (p.hasCv) {
          this.loadCvAnalysis();
        }
      },
      error: () => { this.loading = false; }
    });
  }

  private loadCvAnalysis() {
    this.cvAnalysisLoading = true;
    this.profileService.getCvAnalysis().subscribe({
      next: (analysis) => {
        this.cvAnalysis = analysis;
        this.cvAnalysisLoading = false;
      },
      error: () => { this.cvAnalysisLoading = false; }
    });
  }

  onSave() {
    if (!this.profile) return;
    this.saving = true;
    this.message = '';
    this.profileService.updateProfile({
      firstName: this.profile.firstName,
      lastName: this.profile.lastName,
      phone: this.profile.phone,
      linkedinUrl: this.profile.linkedinUrl,
      coverLetter: this.profile.coverLetter,
      emailAlerts: this.profile.emailAlerts,
      alertThreshold: this.profile.alertThreshold
    }).subscribe({
      next: (p) => { this.profile = p; this.saving = false; this.message = 'Profile saved!'; },
      error: () => { this.saving = false; }
    });
  }

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.uploading = true;
    this.profileService.uploadCv(input.files[0]).subscribe({
      next: (p) => { this.profile = p; this.uploading = false; this.message = 'CV uploaded and analyzed!'; this.loadCvAnalysis(); },
      error: () => { this.uploading = false; }
    });
  }

  downloadCv() {
    this.profileService.downloadCv().subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'cv.pdf';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  onLinkedInImport(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.importingLinkedIn = true;
    this.profileService.importLinkedIn(input.files[0]).subscribe({
      next: (p) => { this.profile = p; this.importingLinkedIn = false; this.message = 'LinkedIn data imported!'; },
      error: () => { this.importingLinkedIn = false; }
    });
  }

  onAlertChange() {
    if (!this.profile) return;
    this.profileService.updateProfile({
      firstName: this.profile.firstName,
      lastName: this.profile.lastName,
      phone: this.profile.phone,
      linkedinUrl: this.profile.linkedinUrl,
      coverLetter: this.profile.coverLetter,
      emailAlerts: this.profile.emailAlerts,
      alertThreshold: this.profile.alertThreshold
    }).subscribe();
  }

  deleteCv() {
    this.profileService.deleteCv().subscribe(() => {
      if (this.profile) {
        this.profile.hasCv = false;
        this.profile.skills = [];
        this.profile.yearsExperience = null;
        this.profile.roleLevel = null;
      }
    });
  }

  async enableNotifications() {
    await this.notificationService.requestPermission();
  }

  disableNotifications() {
    this.notificationService.disable();
  }

  setup2fa() {
    this.twoFactorLoading = true;
    this.authService.setup2fa().subscribe({
      next: (res: any) => {
        this.qrCodeUri = res.qrCodeDataUri;
        this.twoFactorLoading = false;
      },
      error: () => { this.twoFactorLoading = false; }
    });
  }

  verify2fa() {
    this.twoFactorLoading = true;
    this.twoFactorError = '';
    this.authService.verify2fa(this.verify2faCode).subscribe({
      next: () => {
        this.totpEnabled = true;
        this.qrCodeUri = '';
        this.verify2faCode = '';
        this.twoFactorLoading = false;
        this.message = '2FA enabled successfully!';
      },
      error: () => {
        this.twoFactorError = 'Invalid code. Please try again.';
        this.twoFactorLoading = false;
      }
    });
  }

  disable2fa() {
    this.showDisable2faInput = true;
  }

  confirmDisable2fa() {
    this.twoFactorLoading = true;
    this.authService.disable2fa(this.disable2faCode).subscribe({
      next: () => {
        this.totpEnabled = false;
        this.showDisable2faInput = false;
        this.disable2faCode = '';
        this.twoFactorLoading = false;
        this.message = '2FA disabled.';
      },
      error: () => { this.twoFactorLoading = false; }
    });
  }
}
