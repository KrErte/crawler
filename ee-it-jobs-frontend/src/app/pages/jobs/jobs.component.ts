import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { Router } from '@angular/router';
import { JobCardComponent } from '../../components/job-card/job-card.component';
import { JobFiltersComponent } from '../../components/job-filters/job-filters.component';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';
import { JobService } from '../../services/job.service';
import { MatchService } from '../../services/match.service';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth.service';
import { SavedJobService } from '../../services/saved-job.service';
import { Job } from '../../models/job.model';
import { JobMatchScore } from '../../models/match.model';

@Component({
  selector: 'app-jobs',
  standalone: true,
  imports: [JobCardComponent, JobFiltersComponent, SkeletonComponent],
  template: `
    <div class="max-w-7xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-white">IT Jobs in Estonia</h1>
        <div class="flex items-center gap-3">
          <button (click)="toggleInfiniteScroll()"
            class="text-xs px-3 py-1 rounded transition-colors"
            [class]="infiniteScroll ? 'bg-accent text-dark-900 font-semibold' : 'bg-dark-700 text-gray-400 hover:text-white'">
            {{ infiniteScroll ? 'Infinite Scroll ON' : 'Infinite Scroll' }}
          </button>
          <span class="text-gray-500">{{ totalElements }} jobs found</span>
        </div>
      </div>
      <app-job-filters (filterChange)="onFilterChange($event)" [showMatchSort]="hasCv" class="block mb-6" />

      @if (loading && jobs.length === 0) {
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
        <div class="text-center py-12 text-gray-500">No jobs found. Try adjusting your filters.</div>
      } @else {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (job of jobs; track job.id) {
            <app-job-card [job]="job"
              [matchPercentage]="getMatchPercentage(job.id)"
              [matchedSkills]="getMatchedSkills(job.id)"
              [showSave]="isLoggedIn"
              [isSaved]="savedJobService.isSaved(job.id)"
              [showCompare]="true"
              [isSelected]="selectedForCompare.has(job.id)"
              (toggleSave)="onToggleSave($event)"
              (toggleCompare)="onToggleCompare($event)" />
          }
        </div>

        @if (infiniteScroll) {
          <div #scrollSentinel class="h-1"></div>
          @if (loadingMore) {
            <div class="text-center py-6 text-gray-500">Loading more...</div>
          }
        } @else if (totalPages > 1) {
          <div class="flex items-center justify-center gap-4 mt-8">
            <button (click)="changePage(currentPage - 1)" [disabled]="currentPage === 0"
              class="btn-secondary" [class.opacity-50]="currentPage === 0">Previous</button>
            <span class="text-gray-400">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
            <button (click)="changePage(currentPage + 1)" [disabled]="currentPage >= totalPages - 1"
              class="btn-secondary" [class.opacity-50]="currentPage >= totalPages - 1">Next</button>
          </div>
        }
      }

      <!-- Compare floating button -->
      @if (selectedForCompare.size >= 2) {
        <button (click)="goToCompare()"
          class="fixed bottom-6 right-6 btn-primary shadow-lg text-lg px-6 py-3 rounded-full z-40">
          Compare ({{ selectedForCompare.size }})
        </button>
      }

      <!-- WebSocket new jobs banner -->
      @if (newJobsBanner > 0) {
        <div aria-live="polite" role="status">
          <button (click)="refreshJobs()"
            class="fixed top-20 left-1/2 -translate-x-1/2 bg-accent text-dark-900 font-semibold px-6 py-2 rounded-full shadow-lg z-40 hover:bg-accent-hover transition-colors min-h-[44px]">
            {{ newJobsBanner }} new jobs found - click to refresh
          </button>
        </div>
      }
    </div>
  `
})
export class JobsComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('scrollSentinel') scrollSentinel?: ElementRef;

  jobs: Job[] = [];
  loading = true;
  loadingMore = false;
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  isLoggedIn = false;
  infiniteScroll = false;
  newJobsBanner = 0;
  selectedForCompare = new Set<number>();
  skeletonItems = [1, 2, 3, 4, 5, 6];

  private currentFilters: any = {};
  private matchScores = new Map<number, JobMatchScore>();
  hasCv = false;
  private sortByMatch = false;
  private observer?: IntersectionObserver;

  constructor(
    private jobService: JobService,
    private matchService: MatchService,
    private profileService: ProfileService,
    private authService: AuthService,
    public savedJobService: SavedJobService,
    private router: Router
  ) {}

  ngOnInit() {
    this.isLoggedIn = this.authService.isLoggedIn();
    if (this.isLoggedIn) {
      this.savedJobService.loadSavedJobIds().subscribe();
      this.profileService.getProfile().subscribe({
        next: (profile) => { this.hasCv = profile.hasCv; },
        complete: () => this.loadJobs()
      });
    } else {
      this.loadJobs();
    }
  }

  ngAfterViewInit() {
    this.setupObserver();
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }

  toggleInfiniteScroll() {
    this.infiniteScroll = !this.infiniteScroll;
    if (this.infiniteScroll) {
      this.currentPage = 0;
      this.loadJobs();
      setTimeout(() => this.setupObserver(), 100);
    } else {
      this.observer?.disconnect();
      this.currentPage = 0;
      this.loadJobs();
    }
  }

  onFilterChange(filters: any) {
    this.sortByMatch = filters.sortBy === 'match';
    if (this.sortByMatch) {
      filters = { ...filters, sortBy: 'dateScraped' };
    }
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

  onToggleSave(jobId: number) {
    if (this.savedJobService.isSaved(jobId)) {
      this.savedJobService.unsaveJob(jobId).subscribe();
    } else {
      this.savedJobService.saveJob(jobId).subscribe();
    }
  }

  onToggleCompare(jobId: number) {
    if (this.selectedForCompare.has(jobId)) {
      this.selectedForCompare.delete(jobId);
    } else if (this.selectedForCompare.size < 4) {
      this.selectedForCompare.add(jobId);
    }
  }

  goToCompare() {
    const ids = Array.from(this.selectedForCompare).join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }

  refreshJobs() {
    this.newJobsBanner = 0;
    this.currentPage = 0;
    this.loadJobs();
  }

  private loadJobs() {
    if (this.infiniteScroll && this.currentPage > 0) {
      this.loadingMore = true;
    } else {
      this.loading = true;
    }

    this.jobService.getJobs({
      ...this.currentFilters,
      page: this.currentPage,
      size: 21
    }).subscribe({
      next: (res) => {
        if (this.infiniteScroll && this.currentPage > 0) {
          this.jobs = [...this.jobs, ...res.content];
        } else {
          this.jobs = res.content;
        }
        this.totalElements = res.totalElements;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.loadingMore = false;
        this.loadMatchScores();
      },
      error: () => { this.loading = false; this.loadingMore = false; }
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
        if (this.sortByMatch) {
          this.sortJobsByMatch();
        }
      }
    });
  }

  private sortJobsByMatch() {
    this.jobs = [...this.jobs].sort((a, b) => {
      const scoreA = this.matchScores.get(a.id)?.matchPercentage ?? 0;
      const scoreB = this.matchScores.get(b.id)?.matchPercentage ?? 0;
      return scoreB - scoreA;
    });
  }

  private setupObserver() {
    this.observer?.disconnect();
    if (!this.infiniteScroll) return;
    this.observer = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && !this.loadingMore && !this.loading && this.currentPage < this.totalPages - 1) {
        this.currentPage++;
        this.loadJobs();
      }
    });
    setTimeout(() => {
      if (this.scrollSentinel?.nativeElement) {
        this.observer?.observe(this.scrollSentinel.nativeElement);
      }
    }, 200);
  }
}
