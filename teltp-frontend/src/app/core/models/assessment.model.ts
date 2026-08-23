export type AssessmentType = 'QUIZ' | 'EXAM';
export type QuestionType = 'MULTIPLE_CHOICE' | 'ESSAY' | 'CASE_STUDY' | 'PRACTICAL_TASK';
export type AttemptStatus =
  | 'IN_PROGRESS' | 'SUBMITTED' | 'AUTO_GRADED' | 'AWAITING_MANUAL_GRADING' | 'GRADED' | 'EXPIRED';

export interface AssessmentSummary {
  uuid: string;
  referenceNumber: string;
  courseUuid: string;
  title: string;
  type: AssessmentType;
  passMark: number;
  timeLimitMinutes?: number;
  maxAttempts?: number;
  cooldownMinutes?: number;
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
  /** ISO instant; present only for timed attempts. */
  expiresAt?: string;
}

/** Whether the current student may (re)take an assessment, and why not when they cannot. */
export interface AttemptEligibility {
  assessmentUuid: string;
  canAttempt: boolean;
  reason?: string;
  attemptsUsed: number;
  maxAttempts?: number;
  attemptsRemaining?: number;
  alreadyPassed: boolean;
  cooldownUntil?: string;
  hasActiveAttempt: boolean;
  activeAttemptUuid?: string;
  activeExpiresAt?: string;
}

// ---- student-facing results ----
export interface MyAnswerResult {
  questionUuid: string;
  prompt: string;
  type: QuestionType;
  maxPoints: number;
  yourResponse?: string;
  yourSelectedOptionText?: string;
  awardedPoints?: number;
  feedback?: string;
}

export interface MyAttemptResult {
  uuid: string;
  assessmentUuid: string;
  assessmentTitle: string;
  status: AttemptStatus;
  scorePercent?: number;
  passed?: boolean;
  passMark: number;
  submittedAt?: string;
  expiresAt?: string;
  answers: MyAnswerResult[];
}

export interface MyAttemptSummary {
  uuid: string;
  assessmentUuid: string;
  assessmentTitle: string;
  type: AssessmentType | '';
  status: AttemptStatus;
  scorePercent?: number;
  passed?: boolean;
  submittedAt?: string;
}

// ---- instructor grading ----
export interface AttemptSummary {
  uuid: string;
  assessmentUuid: string;
  assessmentTitle: string;
  studentUuid: string;
  status: AttemptStatus;
  submittedAt?: string;
}

export interface AnswerToGrade {
  questionUuid: string;
  prompt: string;
  type: QuestionType;
  maxPoints: number;
  response?: string;
  selectedOptionText?: string;
  awardedPoints?: number;
  graderFeedback?: string;
  autoGraded: boolean;
}

export interface AttemptGradingView {
  uuid: string;
  assessmentUuid: string;
  assessmentTitle: string;
  studentUuid: string;
  status: AttemptStatus;
  scorePercent?: number;
  passed?: boolean;
  passMark: number;
  answers: AnswerToGrade[];
}

export interface GradeAnswerRequest {
  attemptUuid: string;
  questionUuid: string;
  awardedPoints: number;
  feedback?: string;
}

// ---- authoring ----
export interface OptionRequest {
  text: string;
  correct: boolean;
}

export interface CreateAssessmentRequest {
  courseUuid: string;
  title: string;
  type: AssessmentType;
  passMark: number;
  timeLimitMinutes?: number;
  maxAttempts?: number;
  cooldownMinutes?: number;
  pricingPlanUuid?: string;
}

export interface AddQuestionRequest {
  assessmentUuid: string;
  prompt: string;
  type: QuestionType;
  points: number;
  options: OptionRequest[];
}
