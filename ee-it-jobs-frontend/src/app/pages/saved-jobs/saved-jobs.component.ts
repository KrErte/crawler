import { Component, OnInit } from '@angular/core';
import { JobCardComponent } from '../../components/job-card/job-card.component';
import { SavedJobService } from '../../services/saved-job.service';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';
import { Job } from '../../models/job.model';

@Component({
  selector: 'app-saved-jobs',
  standalone: true,
  imports: [JobCardComponent, SkeletonComponent],
  template: `
    <div class="max-w-7xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-white mb-6">Saved Jobs</h1>
      @if (loading) {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (i of [1,2,3,4,5,6]; track i) {
            <div class="card">
              <div class="flex items-start justify-between mb-3">
                <app-skeleton width="70%" height="1.5rem" />
                <app-skeleton width="60px" height="1.5rem" />
              </div>
              <app-skeleton width="40%" height="1rem" extraClass="mb-1" />
              <app-skeleton width="30%" height="0.875rem" extraClass="mb-3" />
              <app-skeleton width="50%" height="0.875rem" extraClass="mb-2" />
              <app-skeleton width="100%" height="2.5rem" extraClass="mb-3" />
              <div class="flex justify-between">
                <app-skeleton width="80px" height="0.75rem" />
                <app-skeleton width="80px" height="0.75rem" />
              </div>
            </div>
          }
        </div>
      } @else if (jobs.length === 0) {
        <div class="text-center py-12 text-gray-500">No saved jobs yet. Browse jobs and click the bookmark icon to save them.</div>
      } @else {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (job of jobs; track job.id) {
            <app-job-card [job]="job" [showSave]="true" [isSaved]="true"
              (toggleSave)="onToggleSave($event)" />
          }
        </div>
      }
    </div>
  `
})
export class SavedJobsComponent implements OnInit {
  jobs: Job[] = [];
  loading = true;

  constructor(private savedJobService: SavedJobService) {}

  ngOnInit() {
    this.loadSavedJobs();
  }

  onToggleSave(jobId: number) {
    this.savedJobService.unsaveJob(jobId).subscribe(() => {
      this.jobs = this.jobs.filter(j => j.id !== jobId);
    });
  }

  private loadSavedJobs() {
    this.loading = true;
    this.savedJobService.getSavedJobs().subscribe({
      next: (jobs) => { this.jobs = jobs; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
