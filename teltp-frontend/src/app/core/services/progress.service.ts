import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { CourseProgressResponse, LessonProgressView, MarkLessonRequest } from '../models/enrollment.model';

@Injectable({ providedIn: 'root' })
export class ProgressService {
  private readonly api = inject(ApiService);

  courseProgress(courseUuid: string): Observable<CourseProgressResponse> {
    return this.api.get<CourseProgressResponse>(`/progress/courses/${courseUuid}`);
  }

  markLesson(req: MarkLessonRequest): Observable<CourseProgressResponse> {
    return this.api.post<CourseProgressResponse>('/progress/lessons', req);
  }

  lessonProgress(courseUuid: string): Observable<LessonProgressView[]> {
    return this.api.get<LessonProgressView[]>(`/progress/courses/${courseUuid}/lessons`);
  }
}
