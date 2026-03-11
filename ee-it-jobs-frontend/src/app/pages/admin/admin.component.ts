import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SlicePipe } from '@angular/common';
import { WebSocketService } from '../../services/websocket.service';
import { environment } from '../../../environments/environment';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [SlicePipe],
  template: `
    <div class="max-w-5xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-white mb-6">Admin Dashboard</h1>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div class="card text-center">
          <div class="text-3xl font-bold text-accent">{{ stats?.totalJobs || 0 }}</div>
          <div class="text-gray-500 text-sm">Total Jobs</div>
        </div>
        <div class="card text-center">
          <div class="text-3xl font-bold text-green-400">{{ stats?.activeJobs || 0 }}</div>
          <div class="text-gray-500 text-sm">Active Jobs</div>
        </div>
        <div class="card text-center">
          <div class="text-3xl font-bold text-yellow-400">{{ stats?.sources?.length || 0 }}</div>
          <div class="text-gray-500 text-sm">Sources</div>
        </div>
      </div>

      <div class="card mb-8">
        <h2 class="text-lg font-semibold text-white mb-4">Scraper Control</h2>
        <div class="flex items-center gap-4 mb-4">
          <button (click)="triggerScrape()" class="btn-primary" [disabled]="scrapeRunning">
            {{ scrapeRunning ? 'Scraping...' : 'Trigger Scrape' }}
          </button>
          <span class="text-sm text-gray-400">
            @if (scrapeStatus?.isRunning) {
              Status: Running
            } @else if (scrapeStatus?.lastRun) {
              Last run: {{ scrapeStatus.lastRun.status }} ({{ scrapeStatus.lastRun.totalNewJobs }} new jobs)
            }
          </span>
        </div>
        <!-- Real-time scrape progress -->
        @if (scrapeProgress.length > 0) {
          <div class="border-t border-dark-700 pt-4">
            <h3 class="text-sm font-semibold text-gray-400 mb-2">Live Progress</h3>
            <div class="space-y-1 max-h-40 overflow-y-auto">
              @for (progress of scrapeProgress; track progress.source) {
                <div class="flex items-center gap-3 text-sm">
                  <span class="text-gray-300 w-32 truncate">{{ progress.source }}</span>
                  <span class="text-accent">{{ progress.jobsFound }} jobs</span>
                </div>
              }
            </div>
          </div>
        }
      </div>

      <div class="card">
        <h2 class="text-lg font-semibold text-white mb-4">Recent Scrape Runs</h2>
        @if (runs.length === 0) {
          <div class="text-gray-500">No scrape runs found.</div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="text-gray-400 text-left border-b border-dark-700">
                  <th class="pb-2">ID</th>
                  <th class="pb-2">Started</th>
                  <th class="pb-2">Status</th>
                  <th class="pb-2">Total</th>
                  <th class="pb-2">New</th>
                  <th class="pb-2">Errors</th>
                </tr>
              </thead>
              <tbody>
                @for (run of runs; track run.id) {
                  <tr class="border-b border-dark-800 text-gray-300">
                    <td class="py-2">{{ run.id }}</td>
                    <td>{{ run.startedAt | slice:0:16 }}</td>
                    <td>
                      <span class="px-2 py-0.5 rounded text-xs"
                        [class]="run.status === 'COMPLETED' ? 'bg-green-900/50 text-green-400' : run.status === 'RUNNING' ? 'bg-yellow-900/50 text-yellow-400' : 'bg-red-900/50 text-red-400'">
                        {{ run.status }}
                      </span>
                    </td>
                    <td>{{ run.totalJobs }}</td>
                    <td>{{ run.totalNewJobs }}</td>
                    <td>{{ run.totalErrors }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `
})
export class AdminComponent implements OnInit, OnDestroy {
  stats: any = null;
  scrapeStatus: any = null;
  scrapeRunning = false;
  runs: any[] = [];
  scrapeProgress: { source: string; jobsFound: number }[] = [];

  private api = environment.apiUrl;
  private wsSub?: Subscription;
  private progressSub?: Subscription;

  constructor(private http: HttpClient, private wsService: WebSocketService) {}

  ngOnInit() {
    this.loadStats();
    this.loadScrapeStatus();
    this.loadRuns();

    this.wsService.connect();
    this.wsSub = this.wsService.onJobUpdate().subscribe(() => {
      this.loadStats();
      this.loadScrapeStatus();
      this.loadRuns();
      this.scrapeRunning = false;
      this.scrapeProgress = [];
    });
    this.progressSub = this.wsService.onScrapeProgress().subscribe(progress => {
      const existing = this.scrapeProgress.find(p => p.source === progress.source);
      if (existing) {
        existing.jobsFound = progress.jobsFound;
      } else {
        this.scrapeProgress = [...this.scrapeProgress, progress];
      }
    });
  }

  ngOnDestroy() {
    this.wsSub?.unsubscribe();
    this.progressSub?.unsubscribe();
  }

  loadStats() {
    this.http.get<any>(`${this.api}/api/jobs/stats`).subscribe(s => this.stats = s);
  }

  loadScrapeStatus() {
    this.http.get<any>(`${this.api}/api/scrape/status`).subscribe(s => {
      this.scrapeStatus = s;
      this.scrapeRunning = s?.isRunning || false;
    });
  }

  loadRuns() {
    this.http.get<any>(`${this.api}/api/scrape/runs`).subscribe(res => {
      this.runs = res.content || [];
    });
  }

  triggerScrape() {
    this.scrapeRunning = true;
    this.scrapeProgress = [];
    this.http.post<any>(`${this.api}/api/scrape/trigger`, null).subscribe({
      next: () => {
        setTimeout(() => {
          this.loadScrapeStatus();
          this.loadRuns();
        }, 2000);
      },
      error: () => { this.scrapeRunning = false; }
    });
  }
}
