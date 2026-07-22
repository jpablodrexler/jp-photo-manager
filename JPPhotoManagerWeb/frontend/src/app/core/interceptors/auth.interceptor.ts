import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const DEFAULT_ERROR_MESSAGE = 'An unexpected error occurred';

function extractErrorMessage(error: HttpErrorResponse): string {
  const body: unknown = error.error;
  if (body && typeof body === 'object' && 'message' in body && typeof (body as { message: unknown }).message === 'string') {
    return (body as { message: string }).message;
  }
  return DEFAULT_ERROR_MESSAGE;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);

  const showError = (error: HttpErrorResponse) => {
    snackBar.open(extractErrorMessage(error), 'Dismiss', { duration: 5000 });
  };

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
            if (refreshError instanceof HttpErrorResponse) {
              showError(refreshError);
            }
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

      if (error instanceof HttpErrorResponse && !req.url.includes('/api/auth/login')) {
        showError(error);
      }

      return throwError(() => error);
    })
  );
};
