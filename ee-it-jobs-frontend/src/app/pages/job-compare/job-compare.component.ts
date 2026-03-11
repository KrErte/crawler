import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { JobService } from '../../services/job.service';
import { Job } from '../../models/job.model';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-job-compare',
  standalone: true,
  imports: [RouterLink, SkeletonComponent],
  template: `
    <div class="max-w-7xl mx-auto px-4 py-8">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-white">Compare Jobs</h1>
        <a routerLink="/jobs" class="btn-secondary text-sm">&larr; Back to jobs</a>
      </div>

      @if (loading) {
        <div class="grid gap-4" [style.grid-template-columns]="'repeat(' + ids.length + ', 1fr)'">
          @for (i of ids; track i) {
            <div class="card space-y-3">
              <app-skeleton width="80%" height="1.5rem" />
              <app-skeleton width="60%" height="1rem" />
              <app-skeleton width="50%" height="0.875rem" />
              <app-skeleton width="100%" height="3rem" />
            </div>
          }
        </div>
      } @else if (jobs.length > 0) {
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-dark-700">
                <th class="text-left text-gray-400 font-normal py-3 pr-4 w-32">Field</th>
                @for (job of jobs; track job.id) {
                  <th class="text-left py-3 px-4">
                    <a [routerLink]="['/jobs', job.id]" class="text-accent hover:text-accent-hover font-semibold">
                      {{ job.title }}
                    </a>
                  </th>
                }
              </tr>
            </thead>
            <tbody>
              <tr class="border-b border-dark-800">
                <td class="py-3 pr-4 text-gray-400">Company</td>
                @for (job of jobs; track job.id) {
                  <td class="py-3 px-4 text-white">{{ job.company }}</td>
                }
              </tr>
              <tr class="border-b border-dark-800">
                <td class="py-3 pr-4 text-gray-400">Location</td>
                @for (job of jobs; track job.id) {
                  <td class="py-3 px-4 text-gray-300">{{ job.location || 'Estonia' }}</td>
                }
              </tr>
              <tr class="border-b border-dark-800">
                <td class="py-3 pr-4 text-gray-400">Salary</td>
                @for (job of jobs; track job.id) {
                  <td class="py-3 px-4 text-green-400">{{ job.salaryText || 'Not specified' }}</td>
                }
              </tr>
              <tr class="border-b border-dark-800">
                <td class="py-3 pr-4 text-gray-400">Workplace</td>
                @for (job of jobs; track job.id) {
                  <td class="py-3 px-4 text-gray-300">{{ job.workplaceType || 'Unknown' }}</td>
                }
              </tr>
              <tr class="border-b border-dark-800">
                <td class="py-3 pr-4 text-gray-400">Department</td>
                @for (job of jobs; track job.id) {
                  <td class="py-3 px-4 text-gray-300">{{ job.department || '-' }}</td>
                }
              </tr>
              <tr class="border-b border-dark-800">
                <td class="py-3 pr-4 text-gray-400">Source</td>
                @for (job of jobs; track job.id) {
                  <td class="py-3 px-4 text-gray-300">{{ job.source }}</td>
                }
              </tr>
              <tr class="border-b border-dark-800">
                <td class="py-3 pr-4 text-gray-400">Skills</td>
                @for (job of jobs; track job.id) {
                  <td class="py-3 px-4">
                    @if (job.skills && job.skills.length > 0) {
                      <div class="flex flex-wrap gap-1">
                        @for (skill of job.skills; track skill) {
                          <span class="bg-accent/10 text-accent px-2 py-0.5 rounded text-xs">{{ skill }}</span>
                        }
                      </div>
                    } @else {
                      <span class="text-gray-500">-</span>
                    }
                  </td>
                }
              </tr>
              <tr>
                <td class="py-3 pr-4 text-gray-400">Apply</td>
                @for (job of jobs; track job.id) {
                  <td class="py-3 px-4">
                    <a [href]="job.url" target="_blank" rel="noopener" class="btn-primary text-sm">Apply</a>
                  </td>
                }
              </tr>
            </tbody>
          </table>
        </div>
      } @else {
        <div class="text-center py-12 text-gray-500">No jobs to compare. Select at least 2 jobs from the jobs page.</div>
      }
    </div>
  `
})
export class JobCompareComponent implements OnInit {
  jobs: Job[] = [];
  ids: number[] = [];
  loading = true;

  constructor(private route: ActivatedRoute, private jobService: JobService) {}

  ngOnInit() {
    const idsParam = this.route.snapshot.queryParamMap.get('ids');
    if (!idsParam) {
      this.loading = false;
      return;
    }
    this.ids = idsParam.split(',').map(Number).filter(id => !isNaN(id));
    if (this.ids.length === 0) {
      this.loading = false;
      return;
    }
    forkJoin(this.ids.map(id => this.jobService.getJob(id))).subscribe({
      next: (jobs) => { this.jobs = jobs; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
