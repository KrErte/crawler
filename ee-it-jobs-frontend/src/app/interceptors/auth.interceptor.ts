import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, retry, timer } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const auth = inject(AuthService);

  const token = localStorage.getItem('accessToken');
  if (token && !req.url.includes('/api/auth/')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req).pipe(
    retry({
      count: 1,
      delay: (error: HttpErrorResponse) => {
        if (error.status === 429) {
          toast.info('Too many requests. Retrying...');
          return timer(2000);
        }
        return throwError(() => error);
      }
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/')) {
        auth.logout();
        toast.error('Session expired. Please log in again.');
      } else if (error.status === 500) {
        toast.error('Server error. Please try again later.');
      } else if (error.status === 0) {
        toast.error('Network error. Check your connection.');
      }
      return throwError(() => error);
    })
  );
};
