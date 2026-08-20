export type AssessmentType = 'QUIZ' | 'EXAM';
export type QuestionType = 'MULTIPLE_CHOICE' | 'ESSAY' | 'CASE_STUDY' | 'PRACTICAL_TASK';
export type AttemptStatus = 'IN_PROGRESS' | 'AUTO_GRADED' | 'AWAITING_MANUAL_GRADING';

export interface AssessmentSummary {
  uuid: string;
  referenceNumber: string;
  courseUuid: string;
  title: string;
  type: AssessmentType;
  passMark: number;
  timeLimitMinutes?: number;
}

export interface OptionView { uuid: string; text: string; }

export interface QuestionView {
  uuid: string;
  prompt: string;
  type: QuestionType;
  points: number;
  options: OptionView[];
}

export interface AssessmentView {
  uuid: string;
  title: string;
  type: AssessmentType;
  passMark: number;
  timeLimitMinutes?: number;
  questions: QuestionView[];
}

export interface SubmittedAnswer {
  questionUuid: string;
  selectedOptionUuid?: string;
  response?: string;
}

export interface SubmitAttemptRequest {
  assessmentUuid: string;
  answers: SubmittedAnswer[];
}

export interface AttemptResponse {
  uuid: string;
  assessmentUuid: string;
  studentUuid: string;
  status: AttemptStatus;
  scorePercent?: number;
  passed?: boolean;
}
