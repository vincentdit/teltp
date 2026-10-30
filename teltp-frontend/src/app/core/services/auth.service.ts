import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';
import { ApiService } from './api.service';
import { TokenService } from './token.service';
import {
  LoginRequest, RegisterRequest, TokenResponse, UserResponse,
} from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokens = inject(TokenService);

  readonly isAuthenticated = this.tokens.isAuthenticated;
  readonly username = this.tokens.username;
  readonly roles = this.tokens.roles;

  login(req: LoginRequest): Observable<TokenResponse> {
    return this.api.post<TokenResponse>('/auth/login', req).pipe(tap((t) => this.keep(t)));
  }

  verifyMfa(challenge: string, code: string): Observable<TokenResponse> {
    return this.api.post<TokenResponse>('/auth/mfa/verify', { challenge, code }).pipe(tap((t) => this.keep(t)));
  }

  /** Re-issues an access token from the HttpOnly refresh cookie. Fails quietly when signed out. */
  restore(): Observable<TokenResponse | null> {
    return this.api.post<TokenResponse>('/auth/refresh', {}).pipe(
      tap((t) => this.keep(t)),
      catchError(() => of(null)),
    );
  }

  register(req: RegisterRequest): Observable<UserResponse> {
    return this.api.post<UserResponse>('/auth/register', req);
  }

  forgotPassword(email: string): Observable<void> {
    return this.api.post<void>('/auth/forgot-password', { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.api.post<void>('/auth/reset-password', { token, newPassword });
  }

  logout(): void {
    this.api.post<void>('/auth/logout', {}).subscribe({ error: () => undefined });
    this.tokens.clear();
  }

  private keep(t: TokenResponse): void {
    if (!t.mfaRequired && t.accessToken) this.tokens.setAccess(t.accessToken);
  }
}
