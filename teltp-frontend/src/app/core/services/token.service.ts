import { Injectable, computed, signal } from '@angular/core';
import { RoleName } from '../models/auth.model';

const ACCESS_KEY = 'teltp.access';

interface JwtClaims { sub?: string; roles?: RoleName[]; exp?: number; }

/**
 * Holds the access token in memory. The refresh token is an HttpOnly cookie
 * set by the API, so page reloads call /auth/refresh rather than reading storage.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly accessToken = signal<string | null>(null);

  readonly claims = computed<JwtClaims | null>(() => decode(this.accessToken()));
  readonly isAuthenticated = computed(() => {
    const c = this.claims();
    return !!c && (c.exp ? c.exp * 1000 > Date.now() : true);
  });
  readonly username = computed(() => this.claims()?.sub ?? null);
  readonly roles = computed<RoleName[]>(() => this.claims()?.roles ?? []);

  setAccess(access: string | null): void {
    this.accessToken.set(access);
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem('teltp.refresh');
  }

  getAccessToken(): string | null { return this.accessToken(); }

  clear(): void {
    this.setAccess(null);
  }

  hasAnyRole(required: RoleName[]): boolean {
    if (required.length === 0) return true;
    const mine = this.roles();
    return required.some((r) => mine.includes(r));
  }
}

function decode(token: string | null): JwtClaims | null {
  if (!token) return null;
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}
