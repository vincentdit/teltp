export type DeliveryMode = 'ONLINE' | 'IN_PERSON' | 'HYBRID';
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface CategoryResponse {
  uuid: string;
  name: string;
  description?: string;
  parentUuid?: string;
  children: CategoryResponse[];
}

export interface CourseResponse {
  uuid: string;
  referenceNumber: string;
  title: string;
  description?: string;
  categoryUuid?: string;
  deliveryMode: DeliveryMode;
  status: CourseStatus;
  durationHours?: number;
  instructorUuid?: string;
  pricingPlanUuid?: string;
  prerequisiteUuids: string[];
}

export interface CreateCourseRequest {
  title: string;
  description?: string;
  categoryUuid?: string;
  deliveryMode: DeliveryMode;
  durationHours?: number;
  instructorUuid?: string;
  pricingPlanUuid?: string;
  prerequisiteUuids?: string[];
}

export interface TransitionRequest { targetStatus: CourseStatus; }

// ---- curriculum (course-player) ----
export interface CurriculumLesson {
  uuid: string;
  title: string;
  orderIndex: number;
  estimatedMinutes?: number;
  mandatory: boolean;
  content?: string;
}

export interface CurriculumModule {
  uuid: string;
  title: string;
  orderIndex: number;
  lessons: CurriculumLesson[];
}

export interface CourseCurriculumResponse {
  courseUuid: string;
  title: string;
  modules: CurriculumModule[];
}
