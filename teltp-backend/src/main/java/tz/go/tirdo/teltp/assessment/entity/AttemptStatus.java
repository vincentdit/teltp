package tz.go.tirdo.teltp.assessment.entity;

/**
 * Lifecycle of a student's attempt. EXPIRED is a terminal state for a timed attempt whose
 * window elapsed before submission; it counts as a consumed attempt but never passes.
 */
public enum AttemptStatus { IN_PROGRESS, SUBMITTED, AUTO_GRADED, AWAITING_MANUAL_GRADING, GRADED, EXPIRED }
