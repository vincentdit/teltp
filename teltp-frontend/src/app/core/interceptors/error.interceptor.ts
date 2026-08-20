import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { TokenService } from '../services/token.service';

/** Surfaces backend ApiResponse error messages and handles 401 by clearing the session. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snack = inject(MatSnackBar);
  const router = inject(Router);
  const tokens = inject(TokenService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const message = err.error?.message ?? err.message ?? 'Unexpected error';
      if (err.status === 401) {
        tokens.clear();
        router.navigate(['/login']);
      } else if (err.status !== 0) {
        snack.open(message, 'Dismiss', { duration: 5000 });
      }
      return throwError(() => err);
    }),
  );
};
