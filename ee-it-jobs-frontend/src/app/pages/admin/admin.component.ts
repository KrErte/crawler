import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SlicePipe, DecimalPipe } from '@angular/common';
import { WebSocketService } from '../../services/websocket.service';
import { environment } from '../../../environments/environment';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [SlicePipe, DecimalPipe],
  template: `
    <div class="max-w-6xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-white mb-6">Admin Dashboard</h1>

      <!-- Overview Cards -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div class="card text-center">
          <div class="text-3xl font-bold text-accent">{{ overview?.activeJobs || 0 }}</div>
          <div class="text-gray-500 text-sm">Active Jobs</div>
        </div>
        <div class="card text-center">
          <div class="text-3xl font-bold text-red-400">{{ overview?.expiredJobs || 0 }}</div>
          <div class="text-gray-500 text-sm">Expired Jobs</div>
        </div>
        <div class="card text-center">
          <div class="text-3xl font-bold text-purple-400">{{ overview?.totalUsers || 0 }}</div>
          <div class="text-gray-500 text-sm">Users</div>
        </div>
        <div class="card text-center">
          <div class="text-3xl font-bold text-green-400">{{ overview?.totalApplications || 0 }}</div>
          <div class="text-gray-500 text-sm">Applications</div>
        </div>
      </div>

      <!-- Salary Overview -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div class="card text-center">
          <div class="text-2xl font-bold text-yellow-400">{{ overview?.jobsWithSalary || 0 }}</div>
          <div class="text-gray-500 text-sm">Jobs with Salary</div>
        </div>
        <div class="card text-center">
          <div class="text-2xl font-bold text-green-400">{{ overview?.avgSalaryMin | number:'1.0-0' }} &euro;</div>
          <div class="text-gray-500 text-sm">Avg Min Salary</div>
        </div>
        <div class="card text-center">
          <div class="text-2xl font-bold text-accent">{{ overview?.avgSalaryMax | number:'1.0-0' }} &euro;</div>
          <div class="text-gray-500 text-sm">Avg Max Salary</div>
        </div>
      </div>

      <!-- Scraper Control -->
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

      <!-- Analytics Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        <!-- Salary Distribution -->
        <div class="card">
          <h2 class="text-lg font-semibold text-white mb-4">Salary Distribution (EUR/month)</h2>
          @if (salaryDist.length > 0) {
            <div class="space-y-2">
              @for (bucket of salaryDist; track bucket.range) {
                <div class="flex items-center gap-3">
                  <span class="text-sm text-gray-300 w-20 text-right">{{ bucket.range }}</span>
                  <div class="flex-1 bg-dark-900 rounded-full h-5 overflow-hidden">
                    <div class="bg-yellow-500 h-full rounded-full transition-all"
                      [style.width.%]="(bucket.count / maxSalaryCount) * 100"></div>
                  </div>
                  <span class="text-xs text-gray-500 w-10">{{ bucket.count }}</span>
                </div>
              }
            </div>
          } @else {
            <div class="text-gray-500 text-sm">No salary data yet</div>
          }
        </div>

        <!-- Top Companies -->
        <div class="card">
          <h2 class="text-lg font-semibold text-white mb-4">Top Companies</h2>
          @if (topCompanies.length > 0) {
            <div class="space-y-2">
              @for (c of topCompanies; track c.company) {
                <div class="flex items-center gap-3">
                  <span class="text-sm text-gray-300 w-32 text-right truncate" [title]="c.company">{{ c.company }}</span>
                  <div class="flex-1 bg-dark-900 rounded-full h-5 overflow-hidden">
                    <div class="bg-purple-500 h-full rounded-full transition-all"
                      [style.width.%]="(c.count / maxCompanyCount) * 100"></div>
                  </div>
                  <span class="text-xs text-gray-500 w-10">{{ c.count }}</span>
                </div>
              }
            </div>
          } @else {
            <div class="text-gray-500 text-sm">No data yet</div>
          }
        </div>

        <!-- Workplace Type -->
        <div class="card">
          <h2 class="text-lg font-semibold text-white mb-4">Workplace Types</h2>
          @if (workplaceTypes.length > 0) {
            <div class="space-y-2">
              @for (w of workplaceTypes; track w.type) {
                <div class="flex items-center gap-3">
                  <span class="text-sm text-gray-300 w-24 text-right">{{ w.type }}</span>
                  <div class="flex-1 bg-dark-900 rounded-full h-5 overflow-hidden">
                    <div class="bg-cyan-500 h-full rounded-full transition-all"
                      [style.width.%]="(w.count / maxWorkplaceCount) * 100"></div>
                  </div>
                  <span class="text-xs text-gray-500 w-10">{{ w.count }}</span>
                </div>
              }
            </div>
          } @else {
            <div class="text-gray-500 text-sm">No data yet</div>
          }
        </div>

        <!-- Job Type -->
        <div class="card">
          <h2 class="text-lg font-semibold text-white mb-4">Job Types</h2>
          @if (jobTypes.length > 0) {
            <div class="space-y-2">
              @for (j of jobTypes; track j.type) {
                <div class="flex items-center gap-3">
                  <span class="text-sm text-gray-300 w-24 text-right">{{ j.type }}</span>
                  <div class="flex-1 bg-dark-900 rounded-full h-5 overflow-hidden">
                    <div class="bg-pink-500 h-full rounded-full transition-all"
                      [style.width.%]="(j.count / maxJobTypeCount) * 100"></div>
                  </div>
                  <span class="text-xs text-gray-500 w-10">{{ j.count }}</span>
                </div>
              }
            </div>
          } @else {
            <div class="text-gray-500 text-sm">No data yet</div>
          }
        </div>
      </div>

      <!-- Recent Scrape Runs -->
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
  overview: any = null;
  scrapeStatus: any = null;
  scrapeRunning = false;
  runs: any[] = [];
  scrapeProgress: { source: string; jobsFound: number }[] = [];
  salaryDist: any[] = [];
  topCompanies: any[] = [];
  workplaceTypes: any[] = [];
  jobTypes: any[] = [];
  maxSalaryCount = 1;
  maxCompanyCount = 1;
  maxWorkplaceCount = 1;
  maxJobTypeCount = 1;

  private api = environment.apiUrl;
  private wsSub?: Subscription;
  private progressSub?: Subscription;

  constructor(private http: HttpClient, private wsService: WebSocketService) {}

  ngOnInit() {
    this.loadOverview();
    this.loadScrapeStatus();
    this.loadRuns();
    this.loadAnalytics();

    this.wsService.connect();
    this.wsSub = this.wsService.onJobUpdate().subscribe(() => {
      this.loadOverview();
      this.loadScrapeStatus();
      this.loadRuns();
      this.loadAnalytics();
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

  loadOverview() {
    this.http.get<any>(`${this.api}/api/stats/overview`).subscribe(s => this.overview = s);
  }

  loadAnalytics() {
    this.http.get<any[]>(`${this.api}/api/stats/salary-distribution`).subscribe(data => {
      this.salaryDist = data;
      this.maxSalaryCount = Math.max(...data.map(d => d.count), 1);
    });
    this.http.get<any[]>(`${this.api}/api/stats/top-companies`).subscribe(data => {
      this.topCompanies = data;
      this.maxCompanyCount = Math.max(...data.map(d => d.count), 1);
    });
    this.http.get<any[]>(`${this.api}/api/stats/workplace-types`).subscribe(data => {
      this.workplaceTypes = data;
      this.maxWorkplaceCount = Math.max(...data.map(d => d.count), 1);
    });
    this.http.get<any[]>(`${this.api}/api/stats/job-types`).subscribe(data => {
      this.jobTypes = data;
      this.maxJobTypeCount = Math.max(...data.map(d => d.count), 1);
    });
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
