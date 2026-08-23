import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { PageResponse } from '../models/api-response.model';
import { CreateOrganizationRequest, OrganizationResponse } from '../models/organization.model';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly api = inject(ApiService);

  list(page = 0, size = 100): Observable<PageResponse<OrganizationResponse>> {
    return this.api.get<PageResponse<OrganizationResponse>>('/organizations', { page, size });
  }

  create(req: CreateOrganizationRequest): Observable<OrganizationResponse> {
    return this.api.post<OrganizationResponse>('/organizations', req);
  }
}
