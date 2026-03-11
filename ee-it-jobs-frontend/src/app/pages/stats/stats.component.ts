import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { SkeletonComponent } from '../../components/skeleton/skeleton.component';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [SkeletonComponent],
  template: `
    <div class="max-w-5xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-white mb-6">Job Market Statistics</h1>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="card">
          <h2 class="text-lg font-semibold text-white mb-4">Top Skills</h2>
          @if (skills.length > 0) {
            <div class="space-y-2">
              @for (skill of skills; track skill.skill) {
                <div class="flex items-center gap-3">
                  <span class="text-sm text-gray-300 w-24 text-right">{{ skill.skill }}</span>
                  <div class="flex-1 bg-dark-900 rounded-full h-5 overflow-hidden">
                    <div class="bg-accent h-full rounded-full transition-all"
                      [style.width.%]="(skill.count / maxSkillCount) * 100"></div>
                  </div>
                  <span class="text-xs text-gray-500 w-10">{{ skill.count }}</span>
                </div>
              }
            </div>
          } @else if (loadingSkills) {
            <div class="space-y-3">
              @for (i of [1,2,3,4,5,6]; track i) {
                <div class="flex items-center gap-3">
                  <app-skeleton width="96px" height="1rem" />
                  <div class="flex-1"><app-skeleton width="100%" height="1.25rem" /></div>
                  <app-skeleton width="30px" height="0.75rem" />
                </div>
              }
            </div>
          }
        </div>

        <div class="card">
          <h2 class="text-lg font-semibold text-white mb-4">Jobs by Source</h2>
          @if (sources.length > 0) {
            <div class="space-y-2">
              @for (source of sources; track source.source) {
                <div class="flex items-center gap-3">
                  <span class="text-sm text-gray-300 w-28 text-right truncate">{{ source.source }}</span>
                  <div class="flex-1 bg-dark-900 rounded-full h-5 overflow-hidden">
                    <div class="bg-green-500 h-full rounded-full transition-all"
                      [style.width.%]="(source.count / maxSourceCount) * 100"></div>
                  </div>
                  <span class="text-xs text-gray-500 w-10">{{ source.count }}</span>
                </div>
              }
            </div>
          } @else if (loadingSources) {
            <div class="space-y-3">
              @for (i of [1,2,3,4,5]; track i) {
                <div class="flex items-center gap-3">
                  <app-skeleton width="112px" height="1rem" />
                  <div class="flex-1"><app-skeleton width="100%" height="1.25rem" /></div>
                  <app-skeleton width="30px" height="0.75rem" />
                </div>
              }
            </div>
          }
        </div>
      </div>

      <div class="card mt-6">
        <h2 class="text-lg font-semibold text-white mb-4">Daily Job Postings (Last 30 Days)</h2>
        @if (trends.length > 0) {
          <div class="flex items-end gap-1 h-40">
            @for (day of trends; track day.date) {
              <div class="flex-1 flex flex-col items-center justify-end h-full group relative">
                <div class="w-full bg-accent/80 rounded-t transition-all hover:bg-accent"
                  [style.height.%]="(day.count / maxTrendCount) * 100"
                  [style.min-height.px]="2"></div>
                <div class="absolute -top-6 bg-dark-700 text-white text-xs px-1 rounded hidden group-hover:block">
                  {{ day.count }}
                </div>
              </div>
            }
          </div>
          <div class="flex justify-between text-xs text-gray-600 mt-1">
            <span>{{ trends[0]?.date }}</span>
            <span>{{ trends[trends.length - 1]?.date }}</span>
          </div>
        } @else if (loadingTrends) {
          <div class="flex items-end gap-1 h-40">
            @for (i of [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]; track i) {
              <div class="flex-1">
                <app-skeleton width="100%" [height]="(20 + i * 5) + 'px'" />
              </div>
            }
          </div>
        } @else {
          <div class="text-gray-500">No data available.</div>
        }
      </div>
    </div>
  `
})
export class StatsComponent implements OnInit {
  skills: any[] = [];
  sources: any[] = [];
  trends: any[] = [];
  maxSkillCount = 1;
  maxSourceCount = 1;
  maxTrendCount = 1;
  loadingSkills = true;
  loadingSources = true;
  loadingTrends = true;

  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<any[]>(`${this.api}/api/stats/skills`).subscribe(data => {
      this.skills = data;
      this.maxSkillCount = Math.max(...data.map(d => d.count), 1);
      this.loadingSkills = false;
    });
    this.http.get<any[]>(`${this.api}/api/stats/sources`).subscribe(data => {
      this.sources = data;
      this.maxSourceCount = Math.max(...data.map(d => d.count), 1);
      this.loadingSources = false;
    });
    this.http.get<any[]>(`${this.api}/api/stats/trends`).subscribe(data => {
      this.trends = data;
      this.maxTrendCount = Math.max(...data.map(d => d.count), 1);
      this.loadingTrends = false;
    });
  }
}
