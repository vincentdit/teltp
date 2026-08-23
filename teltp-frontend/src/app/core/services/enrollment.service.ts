import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { PageResponse } from '../models/api-response.model';
import { AdminAssignRequest, CohortRequest, CohortResponse, EnrollRequest, EnrollmentResponse } from '../models/enrollment.model';

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  private readonly api = inject(ApiService);

  selfEnroll(req: EnrollRequest): Observable<EnrollmentResponse> {
    return this.api.post<EnrollmentResponse>('/enrollments/self', req);
  }

  myEnrollments(page = 0, size = 20): Observable<PageResponse<EnrollmentResponse>> {
    return this.api.get<PageResponse<EnrollmentResponse>>('/enrollments/me', { page, size });
  }

  listCohorts(courseUuid: string): Observable<CohortResponse[]> {
    return this.api.get<CohortResponse[]>('/enrollments/cohorts', { courseUuid });
  }

  createCohort(req: CohortRequest): Observable<CohortResponse> {
    return this.api.post<CohortResponse>('/enrollments/cohorts', req);
  }

  adminAssign(req: AdminAssignRequest): Observable<EnrollmentResponse[]> {
    return this.api.post<EnrollmentResponse[]>('/enrollments/assign', req);
  }
}
