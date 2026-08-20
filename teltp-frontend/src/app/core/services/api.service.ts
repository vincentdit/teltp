import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';

/**
 * Thin wrapper that prefixes the API base URL and unwraps the ApiResponse<T>
 * envelope so callers receive the payload directly.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  get<T>(path: string, params?: Record<string, string | number | boolean>): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(this.base + path, { params: toParams(params) })
      .pipe(map((r) => r.data));
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<ApiResponse<T>>(this.base + path, body).pipe(map((r) => r.data));
  }

  patch<T>(path: string, body: unknown): Observable<T> {
    return this.http.patch<ApiResponse<T>>(this.base + path, body).pipe(map((r) => r.data));
  }
}

function toParams(params?: Record<string, string | number | boolean>): HttpParams {
  let p = new HttpParams();
  if (params) {
    for (const [k, v] of Object.entries(params)) p = p.set(k, String(v));
  }
  return p;
}
