import { Injectable, inject } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { ApiService } from './api.service';
import { PageResponse } from '../models/api-response.model';
import {
  AssignRolesRequest, CreateUserRequest, RoleName, UserResponse,
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly api = inject(ApiService);

  list(page = 0, size = 50, q?: string): Observable<PageResponse<UserResponse>> {
    return this.api.get<PageResponse<UserResponse>>('/users', {
      page, size, ...(q ? { q } : {}),
    });
  }

  assignRoles(req: AssignRolesRequest): Observable<UserResponse> {
    return this.api.post<UserResponse>('/users/roles', req);
  }

  setActive(uuid: string, active: boolean): Observable<UserResponse> {
    return this.api.patch<UserResponse>(`/users/${uuid}/active?active=${active}`, {});
  }

  /**
   * Admin-initiated account creation. /auth/register always grants the default
   * STUDENT role, so when the admin picked different roles we follow up with a
   * second call to set them.
   */
  createUser(req: CreateUserRequest, roles: RoleName[]): Observable<UserResponse> {
    const created$ = this.api.post<UserResponse>('/auth/register', req);
    const sameAsDefault = roles.length === 1 && roles[0] === 'STUDENT';
    if (sameAsDefault) return created$;
    return created$.pipe(
      switchMap((u) => this.assignRoles({ userUuid: u.uuid, roles })),
    );
  }
}
