package tz.go.tirdo.teltp.common.event;

/**
 * Signals that something happened which *may* have completed a course for a student
 * (a summative exam was passed, or the last mandatory lesson was finished). Published by
 * the assessment and progress modules and consumed by certification, which idempotently
 * issues a certificate once the full completion gate is satisfied. Kept in {@code common}
 * so publishers need no dependency on the certification module.
 *
 * @param studentUuid the student whose completion to re-evaluate
 * @param courseUuid  the course to re-evaluate
 * @param sourceUuid  the originating entity (assessment or lesson uuid), for audit/tracing
 */
public record CourseCompletionCandidateEvent(String studentUuid, String courseUuid, String sourceUuid) {}
