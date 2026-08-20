import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { TokenService } from './token.service';
import {
  LoginRequest, RegisterRequest, TokenResponse, UserResponse,
} from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokens = inject(TokenService);

  // expose session signals for templates/guards
  readonly isAuthenticated = this.tokens.isAuthenticated;
  readonly username = this.tokens.username;
  readonly roles = this.tokens.roles;

  login(req: LoginRequest): Observable<TokenResponse> {
    return this.api.post<TokenResponse>('/auth/login', req)
      .pipe(tap((t) => this.tokens.setTokens(t.accessToken, t.refreshToken)));
  }

  register(req: RegisterRequest): Observable<UserResponse> {
    return this.api.post<UserResponse>('/auth/register', req);
  }

  logout(): void {
    this.tokens.clear();
  }
}
