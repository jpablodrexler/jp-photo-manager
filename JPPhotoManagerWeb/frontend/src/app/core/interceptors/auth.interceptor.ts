import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError(error => {
      if (error instanceof HttpErrorResponse
          && error.status === 401
          && !req.url.includes('/api/auth/login')) {
        inject(AuthService).clearSession();
        inject(Router).navigateByUrl('/login');
      }
      return throwError(() => error);
    })
  );
};
