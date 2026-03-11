import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatchService } from '../../services/match.service';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth.service';
import { MatchResult } from '../../models/match.model';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';

@Component({
  selector: 'app-match',
  standalone: true,
  imports: [RouterLink, SkeletonComponent],
  template: `
    <div class="max-w-5xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-white mb-6">CV Job Matching</h1>

      @if (loading && !results) {
        <div class="space-y-4">
          @for (i of [1,2,3]; track i) {
            <div class="card flex items-start gap-6">
              <div class="min-w-[80px] text-center">
                <app-skeleton width="60px" height="2rem" extraClass="mx-auto mb-1" />
                <app-skeleton width="40px" height="0.75rem" extraClass="mx-auto" />
              </div>
              <div class="flex-1">
                <app-skeleton width="60%" height="1.25rem" extraClass="mb-2" />
                <app-skeleton width="40%" height="1rem" extraClass="mb-1" />
                <app-skeleton width="30%" height="0.875rem" extraClass="mb-3" />
                <div class="flex gap-1">
                  <app-skeleton width="60px" height="1.25rem" />
                  <app-skeleton width="50px" height="1.25rem" />
                  <app-skeleton width="70px" height="1.25rem" />
                </div>
              </div>
              <app-skeleton width="80px" height="2.25rem" />
            </div>
          }
        </div>
      } @else if (!results) {
        <div class="card text-center py-12">
          @if (hasCv) {
            <p class="text-gray-400 mb-6">Match jobs using your saved CV or upload a different one</p>
            <div class="flex items-center justify-center gap-4">
              <button (click)="matchFromProfile()" class="btn-primary text-lg px-8 py-3" [disabled]="loading">
                {{ loading ? 'Analyzing CV...' : 'Match with Saved CV' }}
              </button>
              <input type="file" accept=".pdf" (change)="onFileSelect($event)" class="hidden" #fileInput />
              <button (click)="fileInput.click()" class="btn-secondary text-lg px-6 py-3" [disabled]="loading">
                Upload Different CV
              </button>
            </div>
          } @else {
            <p class="text-gray-400 mb-6">Upload your CV to find the best matching jobs</p>
            <input type="file" accept=".pdf" (change)="onFileSelect($event)" class="hidden" #fileInput />
            <button (click)="fileInput.click()" class="btn-primary text-lg px-8 py-3" [disabled]="loading">
              {{ loading ? 'Analyzing CV...' : 'Upload CV & Match' }}
            </button>
          }
          @if (loading) {
            <p class="text-gray-500 mt-4">Scanning all active jobs against your CV...</p>
          }
        </div>
      } @else {
        <div class="flex items-center justify-between mb-6">
          <span class="text-gray-400">{{ results.length }} matches found</span>
          <button (click)="results = null" class="btn-secondary text-sm">New Match</button>
        </div>
        <div class="space-y-4">
          @for (match of results; track match.job.id; let i = $index) {
            <div class="card flex items-start gap-6">
              <div class="text-center min-w-[80px]">
                <div class="text-2xl font-bold" [class]="getScoreColor(match.matchPercentage)">
                  {{ match.matchPercentage }}%
                </div>
                <div class="text-xs text-gray-500">match</div>
              </div>
              <div class="flex-1">
                <a [routerLink]="['/jobs', match.job.id]" class="text-lg font-semibold text-white hover:text-accent transition-colors">
                  {{ match.job.title }}
                </a>
                <div class="text-accent">{{ match.job.company }}</div>
                <div class="text-sm text-gray-500 mb-2">{{ match.job.location || 'Estonia' }}</div>
                @if (match.matchedSkills.length > 0) {
                  <div class="flex flex-wrap gap-1">
                    @for (skill of match.matchedSkills; track skill) {
                      <span class="bg-accent/10 text-accent px-2 py-0.5 rounded text-xs">{{ skill }}</span>
                    }
                  </div>
                }
                @if (match.matchExplanation) {
                  <button (click)="toggleExplanation(match.job.id)" class="text-xs text-gray-500 hover:text-gray-400 mt-2">
                    {{ expandedExplanations.has(match.job.id) ? 'Hide details' : 'Show details' }}
                  </button>
                  @if (expandedExplanations.has(match.job.id)) {
                    <div class="mt-2 text-xs text-gray-500 bg-dark-900 rounded p-2 space-y-1">
                      @for (part of parseExplanation(match.matchExplanation); track part.label) {
                        <div class="flex items-center gap-2">
                          <span class="w-2 h-2 rounded-full" [class]="part.score >= 60 ? 'bg-green-500' : part.score >= 30 ? 'bg-yellow-500' : 'bg-red-500'"></span>
                          <span>{{ part.label }}: {{ part.detail }}</span>
                        </div>
                      }
                    </div>
                  }
                }
              </div>
              <a [href]="match.job.url" target="_blank" rel="noopener" class="btn-secondary text-sm whitespace-nowrap">Apply</a>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class MatchComponent implements OnInit {
  results: MatchResult[] | null = null;
  loading = false;
  hasCv = false;

  constructor(
    private matchService: MatchService,
    private profileService: ProfileService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.profileService.getProfile().subscribe({
        next: (profile) => {
          this.hasCv = profile.hasCv;
          if (this.hasCv) {
            this.matchFromProfile();
          }
        }
      });
    }
  }

  matchFromProfile() {
    this.loading = true;
    this.matchService.matchFromProfile().subscribe({
      next: (results) => { this.results = results; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.loading = true;
    this.matchService.matchJobs(input.files[0]).subscribe({
      next: (results) => { this.results = results; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  expandedExplanations = new Set<number>();

  toggleExplanation(jobId: number) {
    if (this.expandedExplanations.has(jobId)) {
      this.expandedExplanations.delete(jobId);
    } else {
      this.expandedExplanations.add(jobId);
    }
  }

  parseExplanation(explanation: string): { label: string; detail: string; score: number }[] {
    return explanation.split(' | ').map(part => {
      const colonIdx = part.indexOf(':');
      const label = part.substring(0, colonIdx).trim();
      const detail = part.substring(colonIdx + 1).trim();
      const scoreMatch = detail.match(/\[(\d+)%\]/);
      const score = scoreMatch ? parseInt(scoreMatch[1]) : 50;
      return { label, detail, score };
    });
  }

  getScoreColor(score: number): string {
    if (score >= 70) return 'text-green-400';
    if (score >= 40) return 'text-yellow-400';
    return 'text-orange-400';
  }
}
