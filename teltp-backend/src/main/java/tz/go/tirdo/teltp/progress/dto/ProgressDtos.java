package tz.go.tirdo.teltp.progress.dto;

import jakarta.validation.constraints.NotBlank;

public final class ProgressDtos {
    private ProgressDtos() {}

    public record MarkLessonRequest(@NotBlank String lessonUuid, @NotBlank String courseUuid,
                                    int percentComplete, boolean completed) {}

    public record CourseProgressResponse(
            String courseUuid,
            long mandatoryLessons,
            long completedLessons,
            int percentComplete,
            boolean courseCompleted) {}

    /** Per-lesson progress for a student in a course (drives course-player tick state). */
    public record LessonProgressView(String lessonUuid, boolean completed, int percentComplete) {}
}
