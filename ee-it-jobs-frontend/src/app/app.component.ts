import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { trigger, transition, style, animate, query } from '@angular/animations';
import { NavbarComponent } from './components/navbar/navbar.component';
import { ToastComponent } from './components/toast/toast.component';

export const routeAnimation = trigger('routeAnimation', [
  transition('* <=> *', [
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(8px)' }),
      animate('200ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
    ], { optional: true })
  ])
]);

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, ToastComponent],
  animations: [routeAnimation],
  template: `
    <app-navbar />
    <main class="min-h-screen pt-16" [@routeAnimation]="getRouteState(outlet)">
      <router-outlet #outlet="outlet" />
    </main>
    <app-toast />
  `
})
export class AppComponent {
  getRouteState(outlet: RouterOutlet) {
    return outlet?.activatedRouteData?.['animation'] || outlet?.activatedRoute?.snapshot?.url?.toString() || '';
  }
}
