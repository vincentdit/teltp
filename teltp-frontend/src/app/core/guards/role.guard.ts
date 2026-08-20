import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';
import { RoleName } from '../models/auth.model';

/** Use with route data: { roles: ['ADMIN','INSTRUCTOR'] }. */
export const roleGuard: CanActivateFn = (route) => {
  const tokens = inject(TokenService);
  const router = inject(Router);
  const required = (route.data?.['roles'] as RoleName[] | undefined) ?? [];
  if (!tokens.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }
  if (tokens.hasAnyRole(required)) return true;
  return router.createUrlTree(['/catalog']);
};
