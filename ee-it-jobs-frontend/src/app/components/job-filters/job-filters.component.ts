import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { JobService } from '../../services/job.service';
import { JobFilters } from '../../models/job.model';
import { Subject, debounceTime } from 'rxjs';

@Component({
  selector: 'app-job-filters',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="space-y-3">
      <!-- Mobile filter toggle -->
      <button (click)="mobileFiltersOpen = !mobileFiltersOpen"
        class="md:hidden w-full flex items-center justify-between px-4 py-3 bg-dark-800 rounded-lg border border-dark-700 min-h-[44px]"
        aria-label="Toggle filters">
        <span class="text-gray-300 font-medium">Filters</span>
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-5 h-5 text-gray-400 transition-transform"
          [class.rotate-180]="mobileFiltersOpen">
          <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
        </svg>
      </button>

      <div [class.hidden]="!mobileFiltersOpen" class="md:!block">
      <div class="flex flex-wrap gap-3 items-center">
        <div class="relative flex-1 min-w-[200px]">
          <input type="text" [(ngModel)]="search" (ngModelChange)="onSearchChange($event)"
            (focus)="onSearchFocus()" (blur)="onSearchBlur()"
            (keydown)="onSearchKeydown($event)"
            placeholder="Search jobs..." class="w-full min-h-[44px]"
            aria-label="Search jobs" />
          @if (showSuggestions && suggestions.length > 0) {
            <div class="absolute top-full left-0 right-0 mt-1 bg-dark-800 border border-dark-600 rounded-lg shadow-xl z-30 overflow-hidden">
              @for (suggestion of suggestions; track suggestion; let i = $index) {
                <button (mousedown)="selectSuggestion(suggestion)"
                  class="block w-full text-left px-4 py-2 text-sm text-gray-300 hover:bg-dark-700 transition-colors"
                  [class.bg-dark-700]="i === selectedSuggestionIndex">
                  {{ suggestion }}
                </button>
              }
            </div>
          }
        </div>
        <select [(ngModel)]="company" (ngModelChange)="emitChange()" class="min-w-[150px] min-h-[44px]" aria-label="Filter by company">
          <option value="">All Companies</option>
          @for (c of filters?.companies; track c) {
            <option [value]="c">{{ c }}</option>
          }
        </select>
        <select [(ngModel)]="source" (ngModelChange)="emitChange()" class="min-w-[130px] min-h-[44px]" aria-label="Filter by source">
          <option value="">All Sources</option>
          @for (s of filters?.sources; track s) {
            <option [value]="s">{{ s }}</option>
          }
        </select>
        <select [(ngModel)]="workplaceType" (ngModelChange)="emitChange()" class="min-w-[130px] min-h-[44px]" aria-label="Filter by workplace type">
          <option value="">Workplace</option>
          <option value="REMOTE">Remote</option>
          <option value="HYBRID">Hybrid</option>
          <option value="ONSITE">Onsite</option>
        </select>
        <select [(ngModel)]="sortBy" (ngModelChange)="emitChange()" class="min-h-[44px]" aria-label="Sort by">
          <option value="dateScraped">Newest</option>
          @if (showMatchSort) {
            <option value="match">Best Match</option>
          }
          <option value="company">Company</option>
          <option value="title">Title</option>
        </select>
        <button (click)="showAdvanced = !showAdvanced" class="text-sm text-gray-400 hover:text-white min-h-[44px] px-3">
          {{ showAdvanced ? 'Less filters' : 'More filters' }}
        </button>
      </div>
      @if (showAdvanced) {
        <div class="flex flex-wrap gap-3 items-center">
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-400">Salary:</span>
            <input type="number" [(ngModel)]="salaryMin" (ngModelChange)="onSalaryChange()"
              placeholder="Min" class="w-24" />
            <span class="text-gray-500">-</span>
            <input type="number" [(ngModel)]="salaryMax" (ngModelChange)="onSalaryChange()"
              placeholder="Max" class="w-24" />
          </div>
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-400">Skills:</span>
            <div class="flex flex-wrap gap-1">
              @for (skill of availableSkills; track skill) {
                <button (click)="toggleSkill(skill)"
                  class="px-2 py-0.5 rounded text-xs transition-colors"
                  [class]="selectedSkills.has(skill) ? 'bg-accent text-dark-900 font-semibold' : 'bg-dark-700 text-gray-400 hover:text-white'">
                  {{ skill }}
                </button>
              }
            </div>
          </div>
        </div>
      }
      </div>
    </div>
  `
})
export class JobFiltersComponent implements OnInit {
  mobileFiltersOpen = false;
  @Output() filterChange = new EventEmitter<any>();

  @Input() showMatchSort = false;
  filters: JobFilters | null = null;
  search = '';
  company = '';
  source = '';
  workplaceType = '';
  sortBy = 'dateScraped';
  salaryMin: number | null = null;
  salaryMax: number | null = null;
  showAdvanced = false;
  selectedSkills = new Set<string>();

  // Autosuggest
  suggestions: string[] = [];
  showSuggestions = false;
  selectedSuggestionIndex = -1;

  availableSkills = [
    'Java', 'Python', 'JavaScript', 'TypeScript', 'C#', 'Go', 'Rust', 'PHP',
    'React', 'Angular', 'Vue', 'Node.js', 'Spring', 'Docker', 'Kubernetes',
    'AWS', 'Azure', 'PostgreSQL', 'MongoDB', 'Redis', 'Kafka', 'GraphQL'
  ];

  private searchSubject = new Subject<string>();
  private salarySubject = new Subject<void>();
  private suggestSubject = new Subject<string>();

  constructor(private jobService: JobService) {
    this.searchSubject.pipe(debounceTime(200)).subscribe(() => this.emitChange());
    this.salarySubject.pipe(debounceTime(400)).subscribe(() => this.emitChange());
    this.suggestSubject.pipe(debounceTime(200)).subscribe(q => {
      if (q.length >= 2) {
        this.jobService.getSuggestions(q).subscribe({
          next: (s) => { this.suggestions = s; this.selectedSuggestionIndex = -1; },
          error: () => { this.suggestions = []; }
        });
      } else {
        this.suggestions = [];
      }
    });
  }

  ngOnInit() {
    this.jobService.getFilters().subscribe(f => this.filters = f);
  }

  onSearchChange(value: string) {
    this.searchSubject.next(value);
    this.suggestSubject.next(value);
  }

  onSearchFocus() {
    if (this.suggestions.length > 0) {
      this.showSuggestions = true;
    }
  }

  onSearchBlur() {
    setTimeout(() => this.showSuggestions = false, 200);
  }

  onSearchKeydown(event: KeyboardEvent) {
    if (!this.showSuggestions || this.suggestions.length === 0) return;
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.selectedSuggestionIndex = Math.min(this.selectedSuggestionIndex + 1, this.suggestions.length - 1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.selectedSuggestionIndex = Math.max(this.selectedSuggestionIndex - 1, -1);
        break;
      case 'Enter':
        if (this.selectedSuggestionIndex >= 0) {
          event.preventDefault();
          this.selectSuggestion(this.suggestions[this.selectedSuggestionIndex]);
        }
        break;
      case 'Escape':
        this.showSuggestions = false;
        break;
    }
  }

  selectSuggestion(suggestion: string) {
    this.search = suggestion;
    this.showSuggestions = false;
    this.suggestions = [];
    this.emitChange();
  }

  onSalaryChange() {
    this.salarySubject.next();
  }

  toggleSkill(skill: string) {
    if (this.selectedSkills.has(skill)) {
      this.selectedSkills.delete(skill);
    } else {
      this.selectedSkills.add(skill);
    }
    this.emitChange();
  }

  emitChange() {
    if (this.search.length >= 2) {
      this.showSuggestions = true;
    }
    this.filterChange.emit({
      search: this.search, company: this.company, source: this.source,
      workplaceType: this.workplaceType, sortBy: this.sortBy, sortDir: 'DESC',
      skills: this.selectedSkills.size > 0 ? Array.from(this.selectedSkills) : undefined,
      salaryMin: this.salaryMin || undefined,
      salaryMax: this.salaryMax || undefined
    });
  }
}
