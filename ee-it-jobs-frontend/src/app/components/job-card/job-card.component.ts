import { Component, Input, Output, EventEmitter } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SlicePipe } from '@angular/common';
import { Job } from '../../models/job.model';

@Component({
  selector: 'app-job-card',
  standalone: true,
  imports: [RouterLink, SlicePipe],
  template: `
    <div class="card hover:border-dark-600 transition-all duration-200 cursor-pointer relative hover:scale-[1.01] hover:shadow-lg" [routerLink]="['/jobs', job.id]">
      @if (showSave) {
        <button (click)="onToggleSave($event)"
          class="absolute top-3 left-3 z-10 p-1 rounded-lg transition-colors"
          [class]="isSaved ? 'text-accent' : 'text-gray-600 hover:text-gray-400'"
          [title]="isSaved ? 'Unsave' : 'Save'"
          [attr.aria-label]="isSaved ? 'Unsave job' : 'Save job'"
          role="button">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-5 h-5"
            [attr.fill]="isSaved ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M17.593 3.322c1.1.128 1.907 1.077 1.907 2.185V21L12 17.25 4.5 21V5.507c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0z" />
          </svg>
        </button>
      }
      <!-- Share button -->
      <button (click)="onShare($event)"
        class="absolute top-3 right-3 z-10 p-1 rounded-lg text-gray-600 hover:text-gray-400 transition-colors"
        title="Share"
        aria-label="Share job"
        role="button">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="w-5 h-5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M7.217 10.907a2.25 2.25 0 100 2.186m0-2.186c.18.324.283.696.283 1.093s-.103.77-.283 1.093m0-2.186l9.566-5.314m-9.566 7.5l9.566 5.314m0 0a2.25 2.25 0 103.935 2.186 2.25 2.25 0 00-3.935-2.186zm0-12.814a2.25 2.25 0 103.933-2.185 2.25 2.25 0 00-3.933 2.185z" />
        </svg>
      </button>
      @if (showCopied) {
        <div class="absolute top-3 right-12 z-10 bg-green-900/80 text-green-400 text-xs px-2 py-1 rounded">
          Link copied!
        </div>
      }
      @if (matchPercentage !== null) {
        <div class="absolute -top-2 -right-2 px-2 py-1 rounded-full text-xs font-bold shadow-lg"
          [class]="matchBadgeClass">
          {{ matchPercentage }}% match
        </div>
      }
      @if (showCompare) {
        <label (click)="$event.stopPropagation()" class="absolute bottom-3 right-3 z-10 flex items-center gap-1 cursor-pointer">
          <input type="checkbox" [checked]="isSelected" (change)="onToggleCompare($event)"
            class="w-4 h-4 rounded border-dark-600" />
          <span class="text-xs text-gray-500">Compare</span>
        </label>
      }
      <div class="flex items-start justify-between mb-3">
        <h3 class="text-lg font-semibold text-white" [class.pl-7]="showSave">{{ job.title }}</h3>
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
  @Input() isSaved = false;
  @Input() showSave = false;
  @Input() showCompare = false;
  @Input() isSelected = false;
  @Output() toggleSave = new EventEmitter<number>();
  @Output() toggleCompare = new EventEmitter<number>();

  showCopied = false;

  onToggleSave(event: Event) {
    event.stopPropagation();
    event.preventDefault();
    this.toggleSave.emit(this.job.id);
  }

  onToggleCompare(event: Event) {
    event.stopPropagation();
    event.preventDefault();
    this.toggleCompare.emit(this.job.id);
  }

  onShare(event: Event) {
    event.stopPropagation();
    event.preventDefault();
    const url = `${window.location.origin}/jobs/${this.job.id}`;
    if (navigator.share) {
      navigator.share({ title: this.job.title, text: `${this.job.title} at ${this.job.company}`, url });
    } else {
      navigator.clipboard.writeText(url).then(() => {
        this.showCopied = true;
        setTimeout(() => this.showCopied = false, 2000);
      });
    }
  }

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
