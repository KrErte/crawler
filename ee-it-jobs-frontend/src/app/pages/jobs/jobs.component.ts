import { Component, OnInit } from '@angular/core';
import { JobCardComponent } from '../../components/job-card/job-card.component';
import { JobFiltersComponent } from '../../components/job-filters/job-filters.component';
import { JobService } from '../../services/job.service';
import { MatchService } from '../../services/match.service';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth.service';
import { Job } from '../../models/job.model';
import { JobMatchScore } from '../../models/match.model';

@Component({
  selector: 'app-jobs',
  standalone: true,
  imports: [JobCardComponent, JobFiltersComponent],
  template: `
    <div class="max-w-7xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-white">IT Jobs in Estonia</h1>
        <span class="text-gray-500">{{ totalElements }} jobs found</span>
      </div>
      <app-job-filters (filterChange)="onFilterChange($event)" class="block mb-6" />
      @if (loading) {
        <div class="text-center py-12 text-gray-500">Loading jobs...</div>
      } @else if (jobs.length === 0) {
        <div class="text-center py-12 text-gray-500">No jobs found. Try adjusting your filters.</div>
      } @else {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (job of jobs; track job.id) {
            <app-job-card [job]="job"
              [matchPercentage]="getMatchPercentage(job.id)"
              [matchedSkills]="getMatchedSkills(job.id)" />
          }
        </div>
        @if (totalPages > 1) {
          <div class="flex items-center justify-center gap-4 mt-8">
            <button (click)="changePage(currentPage - 1)" [disabled]="currentPage === 0"
              class="btn-secondary" [class.opacity-50]="currentPage === 0">Previous</button>
            <span class="text-gray-400">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
            <button (click)="changePage(currentPage + 1)" [disabled]="currentPage >= totalPages - 1"
              class="btn-secondary" [class.opacity-50]="currentPage >= totalPages - 1">Next</button>
          </div>
        }
      }
    </div>
  `
})
export class JobsComponent implements OnInit {
  jobs: Job[] = [];
  loading = true;
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  private currentFilters: any = {};
  private matchScores = new Map<number, JobMatchScore>();
  private hasCv = false;

  constructor(
    private jobService: JobService,
    private matchService: MatchService,
    private profileService: ProfileService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.profileService.getProfile().subscribe({
        next: (profile) => { this.hasCv = profile.hasCv; },
        complete: () => this.loadJobs()
      });
    } else {
      this.loadJobs();
    }
  }

  onFilterChange(filters: any) {
    this.currentFilters = filters;
    this.currentPage = 0;
    this.loadJobs();
  }

  changePage(page: number) {
    this.currentPage = page;
    this.loadJobs();
  }

  getMatchPercentage(jobId: number): number | null {
    return this.matchScores.get(jobId)?.matchPercentage ?? null;
  }

  getMatchedSkills(jobId: number): string[] {
    return this.matchScores.get(jobId)?.matchedSkills ?? [];
  }

  private loadJobs() {
    this.loading = true;
    this.jobService.getJobs({
      ...this.currentFilters,
      page: this.currentPage,
      size: 21
    }).subscribe({
      next: (res) => {
        this.jobs = res.content;
        this.totalElements = res.totalElements;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.loadMatchScores();
      },
      error: () => { this.loading = false; }
    });
  }

  private loadMatchScores() {
    if (!this.hasCv || !this.authService.isLoggedIn() || this.jobs.length === 0) return;
    const jobIds = this.jobs.map(j => j.id);
    this.matchService.getMatchScores(jobIds).subscribe({
      next: (scores) => {
        this.matchScores.clear();
        for (const score of scores) {
          this.matchScores.set(score.jobId, score);
        }
      }
    });
  }
}
