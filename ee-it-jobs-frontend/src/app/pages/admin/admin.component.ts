import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { SlicePipe, DecimalPipe } from '@angular/common';
import { WebSocketService } from '../../services/websocket.service';
import { AdminService, UserListDto } from '../../services/admin.service';
import { environment } from '../../../environments/environment';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [SlicePipe, DecimalPipe, FormsModule],
  template: `
    <div class="max-w-6xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-white mb-6">Admin Dashboard</h1>

      <!-- Tab Navigation -->
      <div class="flex gap-2 mb-6">
        @for (tab of adminTabs; track tab.value) {
          <button (click)="activeTab = tab.value"
            class="px-4 py-2 rounded-lg text-sm transition-colors"
            [class]="activeTab === tab.value ? 'bg-accent text-dark-900 font-semibold' : 'bg-dark-800 text-gray-400 hover:text-white'">
            {{ tab.label }}
          </button>
        }
      </div>

      @if (activeTab === 'dashboard') {
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
      } <!-- end dashboard tab -->

      @if (activeTab === 'users') {
        <!-- Users Tab -->
        <div class="card">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-white">Users</h2>
            <input type="text" [(ngModel)]="userSearch" (ngModelChange)="searchUsers()"
              placeholder="Search by email or name..." class="w-64 text-sm" />
          </div>
          @if (users.length === 0) {
            <div class="text-gray-500">No users found.</div>
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="text-gray-400 text-left border-b border-dark-700">
                    <th class="pb-2">Email</th>
                    <th class="pb-2">Name</th>
                    <th class="pb-2">Status</th>
                    <th class="pb-2">Admin</th>
                    <th class="pb-2">Apps</th>
                    <th class="pb-2">Joined</th>
                    <th class="pb-2">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (u of users; track u.id) {
                    <tr class="border-b border-dark-800 text-gray-300">
                      <td class="py-2">{{ u.email }}</td>
                      <td>{{ u.firstName }} {{ u.lastName }}</td>
                      <td>
                        <span class="px-2 py-0.5 rounded text-xs"
                          [class]="u.isActive ? 'bg-green-900/50 text-green-400' : 'bg-red-900/50 text-red-400'">
                          {{ u.isActive ? 'Active' : 'Inactive' }}
                        </span>
                      </td>
                      <td>
                        <span class="px-2 py-0.5 rounded text-xs"
                          [class]="u.isAdmin ? 'bg-purple-900/50 text-purple-400' : 'bg-dark-700 text-gray-500'">
                          {{ u.isAdmin ? 'Admin' : 'User' }}
                        </span>
                      </td>
                      <td>{{ u.applicationCount }}</td>
                      <td>{{ u.createdAt | slice:0:10 }}</td>
                      <td class="flex gap-2 py-2">
                        <button (click)="toggleActive(u)" class="text-xs px-2 py-1 rounded transition-colors"
                          [class]="u.isActive ? 'bg-red-900/30 text-red-400 hover:bg-red-900/50' : 'bg-green-900/30 text-green-400 hover:bg-green-900/50'">
                          {{ u.isActive ? 'Deactivate' : 'Activate' }}
                        </button>
                        <button (click)="toggleAdmin(u)" class="text-xs px-2 py-1 rounded transition-colors"
                          [class]="u.isAdmin ? 'bg-gray-700 text-gray-400 hover:bg-gray-600' : 'bg-purple-900/30 text-purple-400 hover:bg-purple-900/50'">
                          {{ u.isAdmin ? 'Remove Admin' : 'Make Admin' }}
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            @if (userTotalPages > 1) {
              <div class="flex items-center justify-center gap-4 mt-4">
                <button (click)="loadUsers(userPage - 1)" [disabled]="userPage === 0" class="btn-secondary text-sm">Previous</button>
                <span class="text-gray-400 text-sm">Page {{ userPage + 1 }} of {{ userTotalPages }}</span>
                <button (click)="loadUsers(userPage + 1)" [disabled]="userPage >= userTotalPages - 1" class="btn-secondary text-sm">Next</button>
              </div>
            }
          }
        </div>
      }

      @if (activeTab === 'jobs') {
        <!-- Jobs Tab -->
        <div class="card">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-white">Job Management</h2>
            <input type="text" [(ngModel)]="jobSearch" (ngModelChange)="searchJobs()"
              placeholder="Search jobs..." class="w-64 text-sm" />
          </div>
          @if (adminJobs.length === 0) {
            <div class="text-gray-500">No jobs found.</div>
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="text-gray-400 text-left border-b border-dark-700">
                    <th class="pb-2">Title</th>
                    <th class="pb-2">Company</th>
                    <th class="pb-2">Source</th>
                    <th class="pb-2">Status</th>
                    <th class="pb-2">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (j of adminJobs; track j.id) {
                    <tr class="border-b border-dark-800 text-gray-300">
                      <td class="py-2 max-w-xs truncate">{{ j.title }}</td>
                      <td>{{ j.company }}</td>
                      <td>{{ j.source }}</td>
                      <td>
                        <span class="px-2 py-0.5 rounded text-xs"
                          [class]="j.isActive !== false ? 'bg-green-900/50 text-green-400' : 'bg-red-900/50 text-red-400'">
                          {{ j.isActive !== false ? 'Active' : 'Inactive' }}
                        </span>
                      </td>
                      <td class="flex gap-2 py-2">
                        <button (click)="toggleJobActive(j)" class="text-xs px-2 py-1 rounded bg-dark-700 text-gray-400 hover:text-white">
                          {{ j.isActive !== false ? 'Deactivate' : 'Activate' }}
                        </button>
                        <button (click)="deleteAdminJob(j.id)" class="text-xs px-2 py-1 rounded bg-red-900/30 text-red-400 hover:bg-red-900/50">
                          Delete
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            @if (jobTotalPages > 1) {
              <div class="flex items-center justify-center gap-4 mt-4">
                <button (click)="loadAdminJobs(jobPage - 1)" [disabled]="jobPage === 0" class="btn-secondary text-sm">Previous</button>
                <span class="text-gray-400 text-sm">Page {{ jobPage + 1 }} of {{ jobTotalPages }}</span>
                <button (click)="loadAdminJobs(jobPage + 1)" [disabled]="jobPage >= jobTotalPages - 1" class="btn-secondary text-sm">Next</button>
              </div>
            }
          }
        </div>
      }
    </div>
  `
})
export class AdminComponent implements OnInit, OnDestroy {
  activeTab = 'dashboard';
  adminTabs = [
    { label: 'Dashboard', value: 'dashboard' },
    { label: 'Users', value: 'users' },
    { label: 'Jobs', value: 'jobs' }
  ];

  // Users tab
  users: UserListDto[] = [];
  userSearch = '';
  userPage = 0;
  userTotalPages = 0;

  // Jobs tab
  adminJobs: any[] = [];
  jobSearch = '';
  jobPage = 0;
  jobTotalPages = 0;

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

  constructor(
    private http: HttpClient,
    private wsService: WebSocketService,
    private adminService: AdminService
  ) {}

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

  // Users management
  loadUsers(page: number = 0) {
    this.userPage = page;
    this.adminService.getUsers(page, 20, this.userSearch || undefined).subscribe(res => {
      this.users = res.content;
      this.userTotalPages = res.totalPages;
    });
  }

  searchUsers() {
    this.userPage = 0;
    this.loadUsers(0);
  }

  toggleActive(user: UserListDto) {
    this.adminService.toggleUserActive(user.id, !user.isActive).subscribe(() => {
      user.isActive = !user.isActive;
    });
  }

  toggleAdmin(user: UserListDto) {
    this.adminService.toggleUserAdmin(user.id, !user.isAdmin).subscribe(() => {
      user.isAdmin = !user.isAdmin;
    });
  }

  // Jobs management
  loadAdminJobs(page: number = 0) {
    this.jobPage = page;
    let params: any = { page, size: 20, sortBy: 'dateScraped', sortDir: 'desc' };
    if (this.jobSearch) params.search = this.jobSearch;
    this.http.get<any>(`${this.api}/api/jobs`, { params }).subscribe(res => {
      this.adminJobs = res.content || [];
      this.jobTotalPages = res.totalPages || 0;
    });
  }

  searchJobs() {
    this.jobPage = 0;
    this.loadAdminJobs(0);
  }

  toggleJobActive(job: any) {
    const newActive = job.isActive === false ? true : false;
    this.adminService.updateJob(job.id, { isActive: newActive }).subscribe(() => {
      job.isActive = newActive;
    });
  }

  deleteAdminJob(jobId: number) {
    this.adminService.deleteJob(jobId).subscribe(() => {
      this.adminJobs = this.adminJobs.filter(j => j.id !== jobId);
    });
  }
}
