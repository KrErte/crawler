import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from '../../services/profile.service';

interface ExperienceEntry {
  company: string;
  role: string;
  startDate: string;
  endDate: string;
  description: string;
}

interface EducationEntry {
  institution: string;
  degree: string;
  field: string;
  startDate: string;
  endDate: string;
}

@Component({
  selector: 'app-cv-builder',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="max-w-3xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-white mb-6">CV Builder</h1>

      @if (message) {
        <div class="bg-green-900/30 border border-green-800 text-green-400 px-4 py-2 rounded-lg mb-4">{{ message }}</div>
      }
      @if (error) {
        <div class="bg-red-900/30 border border-red-800 text-red-400 px-4 py-2 rounded-lg mb-4">{{ error }}</div>
      }

      <!-- Personal Info -->
      <div class="card mb-6">
        <h2 class="text-lg font-semibold text-white mb-4">Personal Information</h2>
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1">Full Name</label>
              <input type="text" [(ngModel)]="data.fullName" class="w-full" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1">Email</label>
              <input type="email" [(ngModel)]="data.email" class="w-full" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm text-gray-400 mb-1">Phone</label>
              <input type="tel" [(ngModel)]="data.phone" class="w-full" />
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-1">LinkedIn URL</label>
              <input type="url" [(ngModel)]="data.linkedinUrl" class="w-full" />
            </div>
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1">Professional Summary</label>
            <textarea [(ngModel)]="data.summary" rows="3" class="w-full"></textarea>
          </div>
        </div>
      </div>

      <!-- Experience -->
      <div class="card mb-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-white">Experience</h2>
          <button (click)="addExperience()" class="btn-secondary text-sm">+ Add</button>
        </div>
        @for (exp of data.experience; track exp; let i = $index) {
          <div class="border border-dark-600 rounded-lg p-4 mb-3">
            <div class="flex items-center justify-between mb-3">
              <span class="text-sm text-gray-400">Experience #{{ i + 1 }}</span>
              <button (click)="removeExperience(i)" class="text-red-400 hover:text-red-300 text-sm">Remove</button>
            </div>
            <div class="grid grid-cols-2 gap-3 mb-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">Company</label>
                <input type="text" [(ngModel)]="exp.company" class="w-full text-sm" />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">Role</label>
                <input type="text" [(ngModel)]="exp.role" class="w-full text-sm" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3 mb-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">Start Date</label>
                <input type="month" [(ngModel)]="exp.startDate" class="w-full text-sm" />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">End Date</label>
                <input type="month" [(ngModel)]="exp.endDate" class="w-full text-sm" placeholder="Present" />
              </div>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">Description</label>
              <textarea [(ngModel)]="exp.description" rows="2" class="w-full text-sm"></textarea>
            </div>
          </div>
        }
      </div>

      <!-- Education -->
      <div class="card mb-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-white">Education</h2>
          <button (click)="addEducation()" class="btn-secondary text-sm">+ Add</button>
        </div>
        @for (edu of data.education; track edu; let i = $index) {
          <div class="border border-dark-600 rounded-lg p-4 mb-3">
            <div class="flex items-center justify-between mb-3">
              <span class="text-sm text-gray-400">Education #{{ i + 1 }}</span>
              <button (click)="removeEducation(i)" class="text-red-400 hover:text-red-300 text-sm">Remove</button>
            </div>
            <div class="grid grid-cols-2 gap-3 mb-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">Institution</label>
                <input type="text" [(ngModel)]="edu.institution" class="w-full text-sm" />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">Degree</label>
                <input type="text" [(ngModel)]="edu.degree" class="w-full text-sm" />
              </div>
            </div>
            <div class="grid grid-cols-3 gap-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">Field of Study</label>
                <input type="text" [(ngModel)]="edu.field" class="w-full text-sm" />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">Start Date</label>
                <input type="month" [(ngModel)]="edu.startDate" class="w-full text-sm" />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">End Date</label>
                <input type="month" [(ngModel)]="edu.endDate" class="w-full text-sm" />
              </div>
            </div>
          </div>
        }
      </div>

      <!-- Skills -->
      <div class="card mb-6">
        <h2 class="text-lg font-semibold text-white mb-4">Skills</h2>
        <div class="flex flex-wrap gap-2 mb-3">
          @for (skill of data.skills; track skill; let i = $index) {
            <span class="bg-accent/10 text-accent px-3 py-1 rounded-full text-sm flex items-center gap-1">
              {{ skill }}
              <button (click)="removeSkill(i)" class="hover:text-red-400 ml-1">&times;</button>
            </span>
          }
        </div>
        <div class="flex gap-2">
          <input type="text" [(ngModel)]="newSkill" placeholder="Add skill..."
            (keyup.enter)="addSkill()" class="flex-1" />
          <button (click)="addSkill()" class="btn-secondary text-sm">Add</button>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-3">
        <button (click)="previewPdf()" class="btn-secondary" [disabled]="generating">
          {{ generating ? 'Generating...' : 'Preview PDF' }}
        </button>
        <button (click)="generateAndSave()" class="btn-primary" [disabled]="generating">
          {{ generating ? 'Generating...' : 'Generate & Save as CV' }}
        </button>
      </div>
    </div>
  `
})
export class CvBuilderComponent {
  data = {
    fullName: '',
    email: '',
    phone: '',
    linkedinUrl: '',
    summary: '',
    experience: [] as ExperienceEntry[],
    education: [] as EducationEntry[],
    skills: [] as string[]
  };

  newSkill = '';
  generating = false;
  message = '';
  error = '';

  constructor(private profileService: ProfileService) {}

  addExperience() {
    this.data.experience.push({ company: '', role: '', startDate: '', endDate: '', description: '' });
  }

  removeExperience(index: number) {
    this.data.experience.splice(index, 1);
  }

  addEducation() {
    this.data.education.push({ institution: '', degree: '', field: '', startDate: '', endDate: '' });
  }

  removeEducation(index: number) {
    this.data.education.splice(index, 1);
  }

  addSkill() {
    const skill = this.newSkill.trim();
    if (skill && !this.data.skills.includes(skill)) {
      this.data.skills.push(skill);
      this.newSkill = '';
    }
  }

  removeSkill(index: number) {
    this.data.skills.splice(index, 1);
  }

  previewPdf() {
    this.generating = true;
    this.error = '';
    this.profileService.buildCv(this.data).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        window.URL.revokeObjectURL(url);
        this.generating = false;
      },
      error: () => { this.error = 'Failed to generate PDF'; this.generating = false; }
    });
  }

  generateAndSave() {
    this.generating = true;
    this.error = '';
    this.message = '';
    this.profileService.buildAndSaveCv(this.data).subscribe({
      next: () => {
        this.message = 'CV generated and saved to your profile!';
        this.generating = false;
      },
      error: () => { this.error = 'Failed to generate CV'; this.generating = false; }
    });
  }
}
