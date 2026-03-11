import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/landing/landing.component').then(m => m.LandingComponent) },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent) },
  { path: 'forgot-password', loadComponent: () => import('./pages/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./pages/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
  { path: 'verify-email', loadComponent: () => import('./pages/verify-email/verify-email.component').then(m => m.VerifyEmailComponent) },
  { path: 'jobs', loadComponent: () => import('./pages/jobs/jobs.component').then(m => m.JobsComponent) },
  { path: 'jobs/:id', loadComponent: () => import('./pages/job-detail/job-detail.component').then(m => m.JobDetailComponent) },
  { path: 'compare', loadComponent: () => import('./pages/job-compare/job-compare.component').then(m => m.JobCompareComponent) },
  { path: 'stats', loadComponent: () => import('./pages/stats/stats.component').then(m => m.StatsComponent) },
  { path: 'profile', loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent), canActivate: [authGuard] },
  { path: 'saved', loadComponent: () => import('./pages/saved-jobs/saved-jobs.component').then(m => m.SavedJobsComponent), canActivate: [authGuard] },
  { path: 'applications', loadComponent: () => import('./pages/applications/applications.component').then(m => m.ApplicationsComponent), canActivate: [authGuard] },
  { path: 'recommendations', loadComponent: () => import('./pages/recommendations/recommendations.component').then(m => m.RecommendationsComponent), canActivate: [authGuard] },
  { path: 'match', loadComponent: () => import('./pages/match/match.component').then(m => m.MatchComponent), canActivate: [authGuard] },
  { path: 'cv-builder', loadComponent: () => import('./pages/cv-builder/cv-builder.component').then(m => m.CvBuilderComponent), canActivate: [authGuard] },
  { path: 'admin', loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent), canActivate: [adminGuard] },
  { path: '**', redirectTo: '' }
];
