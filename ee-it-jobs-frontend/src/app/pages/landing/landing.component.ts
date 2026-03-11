import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="flex flex-col items-center justify-center min-h-screen px-4 -mt-16">
      <h1 class="text-5xl font-bold text-white mb-4">AI Career Assistant</h1>
      <p class="text-xl text-gray-400 mb-2">Estonian IT Job Market</p>
      <p class="text-gray-500 mb-8 text-center max-w-xl">
        Browse hundreds of IT jobs from CV.ee, CVKeskus, Lever, Greenhouse, and more.
        Upload your CV for AI-powered job matching.
      </p>
      <div class="flex gap-4">
        <a routerLink="/jobs" class="btn-primary text-lg px-8 py-3">Browse Jobs</a>
        <a routerLink="/register" class="btn-secondary text-lg px-8 py-3">Get Started</a>
      </div>
      <div class="mt-16 grid grid-cols-1 md:grid-cols-3 gap-8 max-w-4xl">
        <div class="card text-center">
          <div class="text-3xl mb-3 text-accent">20+</div>
          <div class="text-gray-400">Job Sources</div>
        </div>
        <div class="card text-center">
          <div class="text-3xl mb-3 text-accent">AI</div>
          <div class="text-gray-400">CV Matching</div>
        </div>
        <div class="card text-center">
          <div class="text-3xl mb-3 text-accent">Track</div>
          <div class="text-gray-400">Applications</div>
        </div>
      </div>
    </div>
  `
})
export class LandingComponent {}
