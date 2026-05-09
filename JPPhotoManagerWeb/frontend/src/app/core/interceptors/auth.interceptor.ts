import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError(error => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !req.url.includes('/api/auth/login') &&
        !req.url.includes('/api/auth/refresh')
      ) {
        return authService.refresh().pipe(
          switchMap(() => next(req.clone())),
          catchError(refreshError => {
            authService.clearSession();
            router.navigateByUrl('/login');
            return throwError(() => refreshError);
          })
        );
      }

      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        req.url.includes('/api/auth/refresh')
      ) {
        authService.clearSession();
        router.navigateByUrl('/login');
      }

      return throwError(() => error);
    })
  );
};
