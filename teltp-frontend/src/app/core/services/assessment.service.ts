import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  AssessmentSummary, AssessmentView, AttemptResponse, SubmitAttemptRequest,
} from '../models/assessment.model';

@Injectable({ providedIn: 'root' })
export class AssessmentService {
  private readonly api = inject(ApiService);

  forCourse(courseUuid: string): Observable<AssessmentSummary[]> {
    return this.api.get<AssessmentSummary[]>(`/assessments/courses/${courseUuid}`);
  }

  view(uuid: string): Observable<AssessmentView> {
    return this.api.get<AssessmentView>(`/assessments/${uuid}/view`);
  }

  submit(req: SubmitAttemptRequest): Observable<AttemptResponse> {
    return this.api.post<AttemptResponse>('/assessments/submit', req);
  }
}
