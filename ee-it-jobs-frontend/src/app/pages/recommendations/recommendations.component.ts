import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RecommendationService } from '../../services/recommendation.service';
import { SavedJobService } from '../../services/saved-job.service';
import { AuthService } from '../../services/auth.service';
import { JobCardComponent } from '../../components/job-card/job-card.component';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { Job } from '../../models/job.model';

@Component({
  selector: 'app-recommendations',
  standalone: true,
  imports: [JobCardComponent, SkeletonComponent, TranslatePipe],
  template: `
    <div class="max-w-7xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-white">{{ 'recommendations.title' | translate }}</h1>
          <p class="text-gray-500 text-sm mt-1">{{ 'recommendations.basedOn' | translate }}</p>
        </div>
      </div>

      @if (loading) {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (i of skeletonItems; track i) {
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
        <div class="card text-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 mx-auto text-gray-600 mb-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456zM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 00-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 001.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 001.423 1.423l1.183.394-1.183.394a2.25 2.25 0 00-1.423 1.423z" />
          </svg>
          <p class="text-gray-500">{{ 'recommendations.empty' | translate }}</p>
        </div>
      } @else {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (job of jobs; track job.id) {
            <app-job-card [job]="job"
              [showSave]="true"
              [isSaved]="savedJobService.isSaved(job.id)"
              (toggleSave)="onToggleSave($event)" />
          }
        </div>
      }
    </div>
  `
})
export class RecommendationsComponent implements OnInit {
  jobs: Job[] = [];
  loading = true;
  skeletonItems = [1, 2, 3, 4, 5, 6];

  constructor(
    private recommendationService: RecommendationService,
    public savedJobService: SavedJobService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.savedJobService.loadSavedJobIds().subscribe();
    this.recommendationService.getRecommendations(12).subscribe({
      next: (jobs) => {
        this.jobs = jobs;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onToggleSave(jobId: number) {
    if (this.savedJobService.isSaved(jobId)) {
      this.savedJobService.unsaveJob(jobId).subscribe();
    } else {
      this.savedJobService.saveJob(jobId).subscribe();
    }
  }
}
