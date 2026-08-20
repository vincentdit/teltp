import { Injectable, computed, signal } from '@angular/core';
import { RoleName } from '../models/auth.model';

const ACCESS_KEY = 'teltp.access';
const REFRESH_KEY = 'teltp.refresh';

interface JwtClaims { sub?: string; roles?: RoleName[]; exp?: number; }

/**
 * Holds the JWT session in localStorage and exposes reactive signals for the
 * current username and roles, decoded from the access token.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly accessToken = signal<string | null>(localStorage.getItem(ACCESS_KEY));

  readonly claims = computed<JwtClaims | null>(() => decode(this.accessToken()));
  readonly isAuthenticated = computed(() => {
    const c = this.claims();
    return !!c && (c.exp ? c.exp * 1000 > Date.now() : true);
  });
  readonly username = computed(() => this.claims()?.sub ?? null);
  readonly roles = computed<RoleName[]>(() => this.claims()?.roles ?? []);

  setTokens(access: string, refresh: string): void {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
    this.accessToken.set(access);
  }

  getAccessToken(): string | null { return this.accessToken(); }
  getRefreshToken(): string | null { return localStorage.getItem(REFRESH_KEY); }

  clear(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this.accessToken.set(null);
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
