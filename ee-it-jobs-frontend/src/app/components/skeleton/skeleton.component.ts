import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  template: `
    <div class="animate-pulse bg-dark-700 rounded"
      [style.width]="width"
      [style.height]="height"
      [class]="extraClass">
    </div>
  `
})
export class SkeletonComponent {
  @Input() width = '100%';
  @Input() height = '1rem';
  @Input() extraClass = '';
}
