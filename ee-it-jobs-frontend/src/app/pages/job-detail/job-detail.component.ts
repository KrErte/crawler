import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { JobService } from '../../services/job.service';
import { ApplicationService } from '../../services/application.service';
import { AuthService } from '../../services/auth.service';
import { Job } from '../../models/job.model';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [RouterLink, SkeletonComponent],
  template: `
    @if (loading) {
      <div class="max-w-4xl mx-auto px-4 py-8">
        <app-skeleton width="100px" height="1rem" extraClass="mb-4" />
        <div class="card">
          <app-skeleton width="70%" height="2rem" extraClass="mb-3" />
          <app-skeleton width="40%" height="1.25rem" extraClass="mb-4" />
          <div class="flex gap-4 mb-6">
            <app-skeleton width="100px" height="1rem" />
            <app-skeleton width="80px" height="1rem" />
            <app-skeleton width="120px" height="1rem" />
          </div>
          <div class="flex gap-3 mb-6">
            <app-skeleton width="150px" height="2.5rem" />
            <app-skeleton width="180px" height="2.5rem" />
          </div>
          <div class="border-t border-dark-700 pt-6 space-y-3">
            <app-skeleton width="100%" height="1rem" />
            <app-skeleton width="100%" height="1rem" />
            <app-skeleton width="100%" height="1rem" />
            <app-skeleton width="80%" height="1rem" />
            <app-skeleton width="100%" height="1rem" />
            <app-skeleton width="60%" height="1rem" />
          </div>
        </div>
      </div>
    } @else if (job) {
      <div class="max-w-4xl mx-auto px-4 py-8">
        <a routerLink="/jobs" class="text-accent hover:text-accent-hover mb-4 inline-block">&larr; Back to jobs</a>
        <div class="card">
          <div class="flex items-start justify-between mb-4">
            <div>
              <h1 class="text-2xl font-bold text-white mb-2">{{ job.title }}</h1>
              <div class="text-accent text-lg font-medium">{{ job.company }}</div>
            </div>
            <span class="text-xs px-3 py-1 rounded-full" [class]="workplaceClass">{{ job.workplaceType }}</span>
          </div>
          <div class="flex flex-wrap gap-4 text-sm text-gray-400 mb-6">
            <span>{{ job.location || 'Estonia' }}</span>
            @if (job.department) { <span>{{ job.department }}</span> }
            @if (job.salaryText) { <span class="text-green-400">{{ job.salaryText }}</span> }
            <span>{{ job.source }}</span>
            <span>{{ job.dateScraped }}</span>
          </div>
          <div class="flex gap-3 mb-6">
            <a [href]="job.url" target="_blank" rel="noopener" class="btn-primary">Apply on Source</a>
            @if (auth.isLoggedIn()) {
              <button (click)="trackApplication()" class="btn-secondary" [disabled]="applied">
                {{ applied ? 'Tracked' : 'Track Application' }}
              </button>
            }
            <button (click)="shareJob()" class="btn-secondary relative">
              Share Job
              @if (showCopied) {
                <span class="absolute -top-8 left-1/2 -translate-x-1/2 bg-green-900/80 text-green-400 text-xs px-2 py-1 rounded whitespace-nowrap">
                  Link copied!
                </span>
              }
            </button>
          </div>
          @if (job.fullDescription) {
            <div class="border-t border-dark-700 pt-6">
              <h3 class="text-lg font-semibold text-white mb-3">Description</h3>
              <div class="text-gray-300 whitespace-pre-wrap">{{ job.fullDescription }}</div>
            </div>
          } @else if (job.descriptionSnippet) {
            <div class="border-t border-dark-700 pt-6">
              <p class="text-gray-400">{{ job.descriptionSnippet }}</p>
            </div>
          }
        </div>
      </div>
    }

    <!-- Duplicate warning modal -->
    @if (showDuplicateWarning) {
      <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" (click)="showDuplicateWarning = false">
        <div class="card max-w-md mx-4" (click)="$event.stopPropagation()">
          <h3 class="text-lg font-semibold text-white mb-3">Already Tracked</h3>
          <p class="text-gray-400 mb-4">You have already tracked this job in your applications.</p>
          <div class="flex gap-3">
            <a routerLink="/applications" class="btn-primary text-sm">View Applications</a>
            <button (click)="showDuplicateWarning = false" class="btn-secondary text-sm">Close</button>
          </div>
        </div>
      </div>
    }
  `
})
export class JobDetailComponent implements OnInit {
  job: Job | null = null;
  loading = true;
  applied = false;
  showCopied = false;
  showDuplicateWarning = false;

  constructor(
    private route: ActivatedRoute,
    private jobService: JobService,
    private appService: ApplicationService,
    public auth: AuthService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.jobService.getJob(id).subscribe({
      next: (job) => {
        this.job = job;
        this.loading = false;
        if (this.auth.isLoggedIn()) {
          this.appService.checkExists(id).subscribe({
            next: (res) => { this.applied = res.exists; }
          });
        }
      },
      error: () => { this.loading = false; }
    });
  }

  get workplaceClass(): string {
    switch (this.job?.workplaceType) {
      case 'REMOTE': return 'bg-green-900/50 text-green-400 border border-green-800';
      case 'HYBRID': return 'bg-orange-900/50 text-orange-400 border border-orange-800';
      case 'ONSITE': return 'bg-pink-900/50 text-pink-400 border border-pink-800';
      default: return 'bg-dark-700 text-gray-400 border border-dark-600';
    }
  }

  trackApplication() {
    if (!this.job) return;
    if (this.applied) {
      this.showDuplicateWarning = true;
      return;
    }
    this.appService.createApplication({ jobId: this.job.id }).subscribe({
      next: () => { this.applied = true; },
      error: (err) => {
        if (err.status === 409) {
          this.applied = true;
          this.showDuplicateWarning = true;
        }
      }
    });
  }

  shareJob() {
    if (!this.job) return;
    const url = `${window.location.origin}/jobs/${this.job.id}`;
    if (navigator.share) {
      navigator.share({ title: this.job.title, text: `${this.job.title} at ${this.job.company}`, url });
    } else {
      navigator.clipboard.writeText(url).then(() => {
        this.showCopied = true;
        setTimeout(() => this.showCopied = false, 2000);
      });
    }
  }
}
