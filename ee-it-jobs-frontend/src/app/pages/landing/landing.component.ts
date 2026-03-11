import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  template: `
    <!-- Hero Section -->
    <div class="flex flex-col items-center justify-center min-h-[85vh] px-4 -mt-16 relative overflow-hidden">
      <!-- Background gradient -->
      <div class="absolute inset-0 bg-gradient-to-b from-accent/5 via-transparent to-transparent pointer-events-none"></div>

      <div class="relative z-10 text-center">
        <div class="inline-block px-4 py-1.5 rounded-full bg-accent/10 text-accent text-sm font-medium mb-6">
          {{ activeJobs }}+ active IT jobs in Estonia
        </div>
        <h1 class="text-5xl md:text-6xl font-bold text-white mb-4 leading-tight">
          Your AI-Powered<br>
          <span class="text-accent">IT Career Assistant</span>
        </h1>
        <p class="text-xl text-gray-400 mb-8 max-w-2xl mx-auto">
          All Estonian IT jobs in one place. AI-powered CV matching tells you which jobs fit you best.
          From junior to CTO &mdash; find your next opportunity.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <a routerLink="/jobs" class="btn-primary text-lg px-10 py-3.5 rounded-xl shadow-lg shadow-accent/20 hover:shadow-accent/40 transition-all">
            Browse Jobs
          </a>
          <a routerLink="/register" class="btn-secondary text-lg px-10 py-3.5 rounded-xl">
            Upload CV & Match
          </a>
        </div>
      </div>

      <!-- Stats row -->
      <div class="relative z-10 mt-20 grid grid-cols-2 md:grid-cols-4 gap-6 max-w-4xl w-full">
        <div class="card text-center py-6 hover:border-accent/30 transition-colors">
          <div class="text-3xl font-bold text-accent mb-1">{{ activeJobs }}+</div>
          <div class="text-gray-500 text-sm">Active Jobs</div>
        </div>
        <div class="card text-center py-6 hover:border-accent/30 transition-colors">
          <div class="text-3xl font-bold text-green-400 mb-1">25+</div>
          <div class="text-gray-500 text-sm">Job Sources</div>
        </div>
        <div class="card text-center py-6 hover:border-accent/30 transition-colors">
          <div class="text-3xl font-bold text-purple-400 mb-1">AI</div>
          <div class="text-gray-500 text-sm">CV Matching</div>
        </div>
        <div class="card text-center py-6 hover:border-accent/30 transition-colors">
          <div class="text-3xl font-bold text-yellow-400 mb-1">Free</div>
          <div class="text-gray-500 text-sm">Always Free</div>
        </div>
      </div>
    </div>

    <!-- How It Works -->
    <div class="max-w-5xl mx-auto px-4 py-20">
      <h2 class="text-3xl font-bold text-white text-center mb-4">How It Works</h2>
      <p class="text-gray-500 text-center mb-12 max-w-xl mx-auto">Three simple steps to find your perfect IT job in Estonia</p>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div class="card text-center p-8 relative">
          <div class="absolute -top-4 left-1/2 -translate-x-1/2 w-8 h-8 rounded-full bg-accent text-dark-900 font-bold flex items-center justify-center text-sm">1</div>
          <div class="text-4xl mb-4">
            <svg class="w-12 h-12 mx-auto text-accent" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-white mb-2">Browse & Filter</h3>
          <p class="text-gray-500 text-sm">Search by skills, salary, location, company. Filter remote, hybrid, or on-site jobs.</p>
        </div>

        <div class="card text-center p-8 relative">
          <div class="absolute -top-4 left-1/2 -translate-x-1/2 w-8 h-8 rounded-full bg-accent text-dark-900 font-bold flex items-center justify-center text-sm">2</div>
          <div class="text-4xl mb-4">
            <svg class="w-12 h-12 mx-auto text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-white mb-2">Upload Your CV</h3>
          <p class="text-gray-500 text-sm">Our AI reads your CV and extracts skills, experience level, and career preferences.</p>
        </div>

        <div class="card text-center p-8 relative">
          <div class="absolute -top-4 left-1/2 -translate-x-1/2 w-8 h-8 rounded-full bg-accent text-dark-900 font-bold flex items-center justify-center text-sm">3</div>
          <div class="text-4xl mb-4">
            <svg class="w-12 h-12 mx-auto text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-white mb-2">Get Matched</h3>
          <p class="text-gray-500 text-sm">See your match percentage for every job. Get daily alerts when new matching jobs appear.</p>
        </div>
      </div>
    </div>

    <!-- Features Grid -->
    <div class="max-w-5xl mx-auto px-4 py-16">
      <h2 class="text-3xl font-bold text-white text-center mb-12">Everything You Need</h2>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="card p-6 flex gap-4">
          <div class="w-10 h-10 rounded-lg bg-accent/10 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-accent" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-white mb-1">25+ Sources Aggregated</h3>
            <p class="text-gray-500 text-sm">CV.ee, CVKeskus, MeetFrank, Indeed, Greenhouse, Lever, Teamtailor, SmartRecruiters, and company career pages.</p>
          </div>
        </div>

        <div class="card p-6 flex gap-4">
          <div class="w-10 h-10 rounded-lg bg-purple-500/10 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-white mb-1">Smart CV Analysis</h3>
            <p class="text-gray-500 text-sm">Get completeness score, missing in-demand skills, and personalized improvement suggestions.</p>
          </div>
        </div>

        <div class="card p-6 flex gap-4">
          <div class="w-10 h-10 rounded-lg bg-green-500/10 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-white mb-1">Daily Job Alerts</h3>
            <p class="text-gray-500 text-sm">Get notified when new jobs match your profile. Set minimum match threshold and salary preferences.</p>
          </div>
        </div>

        <div class="card p-6 flex gap-4">
          <div class="w-10 h-10 rounded-lg bg-yellow-500/10 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5h12M9 3v2m1.048 9.5A18.022 18.022 0 016.412 9m6.088 9h7M11 21l5-10 5 10M12.751 5C11.783 10.77 8.07 15.61 3 18.129"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-white mb-1">Bilingual (EN/ET)</h3>
            <p class="text-gray-500 text-sm">Full Estonian and English interface. Auto-translate job descriptions between languages.</p>
          </div>
        </div>

        <div class="card p-6 flex gap-4">
          <div class="w-10 h-10 rounded-lg bg-pink-500/10 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-pink-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-white mb-1">Market Statistics</h3>
            <p class="text-gray-500 text-sm">Track top skills, salary distributions, trending companies, and daily job posting trends.</p>
          </div>
        </div>

        <div class="card p-6 flex gap-4">
          <div class="w-10 h-10 rounded-lg bg-cyan-500/10 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-white mb-1">Save & Compare</h3>
            <p class="text-gray-500 text-sm">Save interesting jobs, compare them side-by-side, and track your applications all in one place.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- CTA -->
    <div class="max-w-3xl mx-auto px-4 py-20 text-center">
      <h2 class="text-3xl font-bold text-white mb-4">Ready to Find Your Next IT Job?</h2>
      <p class="text-gray-500 mb-8">Join thousands of IT professionals using EE IT Jobs to advance their careers in Estonia.</p>
      <div class="flex flex-col sm:flex-row gap-4 justify-center">
        <a routerLink="/jobs" class="btn-primary text-lg px-10 py-3.5 rounded-xl">Start Browsing</a>
        <a routerLink="/stats" class="btn-secondary text-lg px-10 py-3.5 rounded-xl">View Market Stats</a>
      </div>
    </div>
  `
})
export class LandingComponent implements OnInit {
  activeJobs = 500;

  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<any>(`${this.api}/api/stats/overview`).subscribe({
      next: (data) => {
        if (data?.activeJobs) this.activeJobs = data.activeJobs;
      },
      error: () => {} // Silently fall back to default
    });
  }
}
