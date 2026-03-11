import { Component } from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  animations: [
    trigger('slideIn', [
      transition(':enter', [
        style({ transform: 'translateX(100%)', opacity: 0 }),
        animate('200ms ease-out', style({ transform: 'translateX(0)', opacity: 1 }))
      ]),
      transition(':leave', [
        animate('150ms ease-in', style({ transform: 'translateX(100%)', opacity: 0 }))
      ])
    ])
  ],
  template: `
    <div class="fixed top-20 right-4 z-50 flex flex-col gap-2 max-w-sm">
      @for (toast of toastService.toasts(); track toast.id) {
        <div @slideIn
          class="px-4 py-3 rounded-lg shadow-lg text-sm font-medium flex items-center gap-2 cursor-pointer"
          [class]="toastClass(toast.type)"
          (click)="toastService.remove(toast.id)">
          @switch (toast.type) {
            @case ('success') {
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-5 h-5 shrink-0">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd" />
              </svg>
            }
            @case ('error') {
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-5 h-5 shrink-0">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
              </svg>
            }
            @case ('info') {
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-5 h-5 shrink-0">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z" clip-rule="evenodd" />
              </svg>
            }
          }
          <span>{{ toast.message }}</span>
        </div>
      }
    </div>
  `
})
export class ToastComponent {
  constructor(public toastService: ToastService) {}

  toastClass(type: string): string {
    switch (type) {
      case 'success': return 'bg-green-900/90 text-green-300 border border-green-800';
      case 'error': return 'bg-red-900/90 text-red-300 border border-red-800';
      case 'info': return 'bg-blue-900/90 text-blue-300 border border-blue-800';
      default: return 'bg-dark-800 text-gray-300 border border-dark-600';
    }
  }
}
