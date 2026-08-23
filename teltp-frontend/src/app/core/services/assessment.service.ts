import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  AddQuestionRequest, AssessmentSummary, AssessmentView, AttemptEligibility, AttemptGradingView,
  AttemptResponse, AttemptSummary, CreateAssessmentRequest, GradeAnswerRequest, MyAttemptResult,
  MyAttemptSummary, QuestionView, SubmitAttemptRequest,
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

  /** Whether the current student may (re)take this assessment; drives the Start button. */
  eligibility(uuid: string): Observable<AttemptEligibility> {
    return this.api.get<AttemptEligibility>(`/assessments/${uuid}/eligibility`);
  }

  /** Start (or resume) an attempt; consumes an attempt per the retake policy. */
  start(uuid: string): Observable<AttemptResponse> {
    return this.api.post<AttemptResponse>(`/assessments/${uuid}/start`, {});
  }

  submit(req: SubmitAttemptRequest): Observable<AttemptResponse> {
    return this.api.post<AttemptResponse>('/assessments/submit', req);
  }

  /** The current student's latest result for an assessment (own answers + feedback). */
  myResult(uuid: string): Observable<MyAttemptResult> {
    return this.api.get<MyAttemptResult>(`/assessments/${uuid}/result`);
  }

  /** The current student's full attempt history across all assessments. */
  myHistory(): Observable<MyAttemptSummary[]> {
    return this.api.get<MyAttemptSummary[]>('/assessments/attempts/mine');
  }

  pendingAttempts(): Observable<AttemptSummary[]> {
    return this.api.get<AttemptSummary[]>('/assessments/attempts/pending');
  }

  gradingView(uuid: string): Observable<AttemptGradingView> {
    return this.api.get<AttemptGradingView>(`/assessments/attempts/${uuid}`);
  }

  grade(req: GradeAnswerRequest): Observable<AttemptResponse> {
    return this.api.post<AttemptResponse>('/assessments/grade', req);
  }

  // ---- authoring ----
  createAssessment(req: CreateAssessmentRequest): Observable<AssessmentSummary> {
    return this.api.post<AssessmentSummary>('/assessments', req);
  }

  addQuestion(req: AddQuestionRequest): Observable<QuestionView> {
    return this.api.post<QuestionView>('/assessments/questions', req);
  }
}
