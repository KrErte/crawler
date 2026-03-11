import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SlicePipe } from '@angular/common';
import { ApplicationService } from '../../services/application.service';
import { Application } from '../../models/application.model';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';
import { Subject, debounceTime } from 'rxjs';

@Component({
  selector: 'app-applications',
  standalone: true,
  imports: [FormsModule, SlicePipe, SkeletonComponent],
  template: `
    <div class="max-w-5xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-white">My Applications</h1>
        <div class="flex gap-2">
          <button (click)="exportCsv()" class="btn-secondary text-sm">Export CSV</button>
          <button (click)="exportPdf()" class="btn-secondary text-sm">Export PDF</button>
        </div>
      </div>

      <div class="flex gap-2 mb-6 flex-wrap">
        @for (tab of tabs; track tab.value) {
          <button (click)="setTab(tab.value)"
            class="px-4 py-2 rounded-lg text-sm transition-colors"
            [class]="activeTab === tab.value ? 'bg-accent text-dark-900 font-semibold' : 'bg-dark-800 text-gray-400 hover:text-white'">
            {{ tab.label }}
          </button>
        }
      </div>

      @if (loading) {
        <div class="space-y-4">
          @for (i of [1,2,3]; track i) {
            <div class="card">
              <div class="flex items-start justify-between mb-3">
                <div>
                  <app-skeleton width="60%" height="1.25rem" extraClass="mb-2" />
                  <app-skeleton width="40%" height="1rem" />
                </div>
                <div class="flex items-center gap-3">
                  <app-skeleton width="100px" height="2rem" />
                  <app-skeleton width="60px" height="1rem" />
                </div>
              </div>
              <div class="flex items-center gap-4 mb-3">
                <app-skeleton width="80px" height="0.875rem" />
                <app-skeleton width="120px" height="0.875rem" />
                <app-skeleton width="60px" height="0.875rem" />
              </div>
              <app-skeleton width="100%" height="3rem" />
            </div>
          }
        </div>
      } @else if (applications.length === 0) {
        <div class="text-gray-500 text-center py-8">No applications found.</div>
      } @else {
        <div class="space-y-4">
          @for (app of applications; track app.id) {
            <div class="card">
              <div class="flex items-start justify-between mb-3">
                <div>
                  <h3 class="text-lg font-semibold text-white">{{ app.jobTitle }}</h3>
                  <div class="text-accent">{{ app.company }}</div>
                </div>
                <div class="flex items-center gap-3">
                  <select [(ngModel)]="app.status" (ngModelChange)="onStatusChange(app)"
                    class="text-sm py-1">
                    <option value="SUBMITTED">Submitted</option>
                    <option value="INTERVIEW">Interview</option>
                    <option value="OFFER">Offer</option>
                    <option value="REJECTED">Rejected</option>
                    <option value="GHOSTED">Ghosted</option>
                  </select>
                  <button (click)="deleteApp(app)" class="text-red-400 hover:text-red-300 text-sm">Delete</button>
                </div>
              </div>
              <div class="flex items-center gap-4 text-sm text-gray-500 mb-3">
                <span>{{ app.source }}</span>
                <span>Applied {{ app.appliedAt | slice:0:10 }}</span>
                <a [href]="app.jobUrl" target="_blank" class="text-accent hover:text-accent-hover">View job</a>
              </div>
              <textarea [(ngModel)]="app.notes" (ngModelChange)="onNotesChange(app)"
                placeholder="Add notes..." rows="2"
                class="w-full text-sm bg-dark-900 border-dark-700"></textarea>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class ApplicationsComponent implements OnInit {
  applications: Application[] = [];
  loading = true;
  activeTab = '';

  tabs = [
    { label: 'All', value: '' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'Interview', value: 'INTERVIEW' },
    { label: 'Offer', value: 'OFFER' },
    { label: 'Rejected', value: 'REJECTED' },
    { label: 'Ghosted', value: 'GHOSTED' }
  ];

  private notesSubjects = new Map<number, Subject<Application>>();

  constructor(private appService: ApplicationService) {}

  ngOnInit() { this.loadApps(); }

  setTab(tab: string) {
    this.activeTab = tab;
    this.loadApps();
  }

  private loadApps() {
    this.loading = true;
    this.appService.getApplications(this.activeTab || undefined).subscribe({
      next: (apps) => { this.applications = apps; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  onStatusChange(app: Application) {
    this.appService.updateApplication(app.id, { status: app.status }).subscribe();
  }

  onNotesChange(app: Application) {
    if (!this.notesSubjects.has(app.id)) {
      const subject = new Subject<Application>();
      subject.pipe(debounceTime(600)).subscribe(a => {
        this.appService.updateApplication(a.id, { notes: a.notes }).subscribe();
      });
      this.notesSubjects.set(app.id, subject);
    }
    this.notesSubjects.get(app.id)!.next(app);
  }

  deleteApp(app: Application) {
    this.appService.deleteApplication(app.id).subscribe(() => {
      this.applications = this.applications.filter(a => a.id !== app.id);
    });
  }

  exportCsv() {
    this.appService.exportApplications('csv').subscribe(blob => {
      this.downloadBlob(blob, 'applications.csv');
    });
  }

  exportPdf() {
    this.appService.exportApplications('pdf').subscribe(blob => {
      this.downloadBlob(blob, 'applications.pdf');
    });
  }

  private downloadBlob(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
