import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SlicePipe } from '@angular/common';
import { Job } from '../../models/job.model';

@Component({
  selector: 'app-job-card',
  standalone: true,
  imports: [RouterLink, SlicePipe],
  template: `
    <div class="card hover:border-dark-600 transition-colors cursor-pointer relative" [routerLink]="['/jobs', job.id]">
      @if (matchPercentage !== null) {
        <div class="absolute -top-2 -right-2 px-2 py-1 rounded-full text-xs font-bold shadow-lg"
          [class]="matchBadgeClass">
          {{ matchPercentage }}% match
        </div>
      }
      <div class="flex items-start justify-between mb-3">
        <h3 class="text-lg font-semibold text-white">{{ job.title }}</h3>
        <span class="text-xs px-2 py-1 rounded-full whitespace-nowrap ml-2"
          [class]="workplaceClass">{{ job.workplaceType || 'Unknown' }}</span>
      </div>
      <div class="text-accent font-medium mb-1">{{ job.company }}</div>
      <div class="text-gray-500 text-sm mb-3">{{ job.location || 'Estonia' }}</div>
      @if (job.salaryText) {
        <div class="text-green-400 text-sm mb-2">{{ job.salaryText }}</div>
      }
      @if (job.descriptionSnippet) {
        <p class="text-gray-400 text-sm line-clamp-2 mb-3">{{ job.descriptionSnippet }}</p>
      }
      @if (matchedSkills.length > 0) {
        <div class="flex flex-wrap gap-1 mb-3">
          @for (skill of matchedSkills | slice:0:3; track skill) {
            <span class="bg-accent/10 text-accent px-2 py-0.5 rounded text-xs">{{ skill }}</span>
          }
        </div>
      }
      <div class="flex items-center justify-between text-xs text-gray-500">
        <span>{{ job.source }}</span>
        <span>{{ job.dateScraped }}</span>
      </div>
    </div>
  `
})
export class JobCardComponent {
  @Input({ required: true }) job!: Job;
  @Input() matchPercentage: number | null = null;
  @Input() matchedSkills: string[] = [];

  get workplaceClass(): string {
    switch (this.job.workplaceType) {
      case 'REMOTE': return 'bg-green-900/50 text-green-400 border border-green-800';
      case 'HYBRID': return 'bg-orange-900/50 text-orange-400 border border-orange-800';
      case 'ONSITE': return 'bg-pink-900/50 text-pink-400 border border-pink-800';
      default: return 'bg-dark-700 text-gray-400 border border-dark-600';
    }
  }

  get matchBadgeClass(): string {
    if (this.matchPercentage === null) return '';
    if (this.matchPercentage >= 70) return 'bg-green-500 text-white';
    if (this.matchPercentage >= 40) return 'bg-yellow-500 text-black';
    return 'bg-orange-500 text-white';
  }
}
