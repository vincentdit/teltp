export type EnrollmentStatus =
  | 'PENDING_PAYMENT' | 'ACTIVE' | 'WAITLISTED' | 'COMPLETED' | 'CANCELLED';

export interface EnrollRequest { courseUuid: string; cohortUuid?: string; }

export interface EnrollmentResponse {
  uuid: string;
  courseUuid: string;
  studentUuid: string;
  cohortUuid?: string;
  status: EnrollmentStatus;
  assignedByOrganizationUuid?: string;
}

export interface CourseProgressResponse {
  courseUuid: string;
  mandatoryLessons: number;
  completedLessons: number;
  percentComplete: number;
  courseCompleted: boolean;
}

export interface MarkLessonRequest {
  lessonUuid: string;
  courseUuid: string;
  percentComplete: number;
  completed: boolean;
}

export interface LessonProgressView {
  lessonUuid: string;
  completed: boolean;
  percentComplete: number;
}

// ---- cohorts ----
export interface CohortResponse {
  uuid: string;
  courseUuid: string;
  name: string;
  startDate?: string;
  endDate?: string;
  capacity?: number;
  activeCount: number;
}

export interface CohortRequest {
  courseUuid: string;
  name: string;
  startDate?: string;
  endDate?: string;
  capacity?: number;
}

export interface AdminAssignRequest {
  courseUuid: string;
  cohortUuid?: string;
  organizationUuid: string;
  studentUuids: string[];
}
