import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { PageResponse } from '../models/api-response.model';
import {
  CategoryResponse, CourseResponse, CourseCurriculumResponse, CreateCourseRequest, TransitionRequest,
} from '../models/catalog.model';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly api = inject(ApiService);

  categories(): Observable<CategoryResponse[]> {
    return this.api.get<CategoryResponse[]>('/catalog/categories');
  }

  publishedCourses(page = 0, size = 12): Observable<PageResponse<CourseResponse>> {
    return this.api.get<PageResponse<CourseResponse>>('/catalog/courses', { page, size });
  }

  course(uuid: string): Observable<CourseResponse> {
    return this.api.get<CourseResponse>(`/catalog/courses/${uuid}`);
  }

  curriculum(uuid: string): Observable<CourseCurriculumResponse> {
    return this.api.get<CourseCurriculumResponse>(`/catalog/courses/${uuid}/curriculum`);
  }

  createCourse(req: CreateCourseRequest): Observable<CourseResponse> {
    return this.api.post<CourseResponse>('/catalog/courses', req);
  }

  assignInstructor(courseUuid: string, instructorUuid: string | null): Observable<CourseResponse> {
    const q = instructorUuid ? `?instructorUuid=${instructorUuid}` : '';
    return this.api.patch<CourseResponse>(`/catalog/courses/${courseUuid}/instructor${q}`, {});
  }

  transition(uuid: string, req: TransitionRequest): Observable<CourseResponse> {
    return this.api.post<CourseResponse>(`/catalog/courses/${uuid}/transition`, req);
  }
}
