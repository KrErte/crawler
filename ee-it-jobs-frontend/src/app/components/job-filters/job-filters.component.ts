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
    <div class="flex flex-wrap gap-3 items-center">
      <input type="text" [(ngModel)]="search" (ngModelChange)="onSearchChange($event)"
        placeholder="Search jobs..." class="flex-1 min-w-[200px]" />
      <select [(ngModel)]="company" (ngModelChange)="emitChange()" class="min-w-[150px]">
        <option value="">All Companies</option>
        @for (c of filters?.companies; track c) {
          <option [value]="c">{{ c }}</option>
        }
      </select>
      <select [(ngModel)]="source" (ngModelChange)="emitChange()" class="min-w-[130px]">
        <option value="">All Sources</option>
        @for (s of filters?.sources; track s) {
          <option [value]="s">{{ s }}</option>
        }
      </select>
      <select [(ngModel)]="workplaceType" (ngModelChange)="emitChange()" class="min-w-[130px]">
        <option value="">Workplace</option>
        <option value="REMOTE">Remote</option>
        <option value="HYBRID">Hybrid</option>
        <option value="ONSITE">Onsite</option>
      </select>
      <select [(ngModel)]="sortBy" (ngModelChange)="emitChange()">
        <option value="dateScraped">Newest</option>
        @if (showMatchSort) {
          <option value="match">Best Match</option>
        }
        <option value="company">Company</option>
        <option value="title">Title</option>
      </select>
    </div>
  `
})
export class JobFiltersComponent implements OnInit {
  @Output() filterChange = new EventEmitter<any>();

  @Input() showMatchSort = false;
  filters: JobFilters | null = null;
  search = '';
  company = '';
  source = '';
  workplaceType = '';
  sortBy = 'dateScraped';

  private searchSubject = new Subject<string>();

  constructor(private jobService: JobService) {
    this.searchSubject.pipe(debounceTime(200)).subscribe(() => this.emitChange());
  }

  ngOnInit() {
    this.jobService.getFilters().subscribe(f => this.filters = f);
  }

  onSearchChange(value: string) {
    this.searchSubject.next(value);
  }

  emitChange() {
    this.filterChange.emit({
      search: this.search, company: this.company, source: this.source,
      workplaceType: this.workplaceType, sortBy: this.sortBy, sortDir: 'DESC'
    });
  }
}
