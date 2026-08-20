package tz.go.tirdo.teltp.progress.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.assessment.service.AssessmentService;
import tz.go.tirdo.teltp.catalog.repository.LessonRepository;
import tz.go.tirdo.teltp.progress.dto.ProgressDtos.*;
import tz.go.tirdo.teltp.progress.entity.LessonProgress;
import tz.go.tirdo.teltp.progress.repository.LessonProgressRepository;

import java.time.Instant;
import java.util.List;

/**
 * Tracks per-lesson completion and computes course completion. For HYBRID courses the
 * completion rule also factors session attendance (delegated via the schedule module's
 * attendance count), so a single completion record spans online + in-person delivery.
 */
@Service
public class ProgressService {

    private final LessonProgressRepository progress;
    private final LessonRepository lessons;
    private final AssessmentService assessments;

    public ProgressService(LessonProgressRepository progress, LessonRepository lessons,
                           AssessmentService assessments) {
        this.progress = progress;
        this.lessons = lessons;
        this.assessments = assessments;
    }

    @Transactional
    public CourseProgressResponse mark(String studentUuid, MarkLessonRequest req) {
        LessonProgress lp = progress.findByStudentUuidAndLessonUuid(studentUuid, req.lessonUuid())
                .orElseGet(LessonProgress::new);
        lp.setStudentUuid(studentUuid);
        lp.setLessonUuid(req.lessonUuid());
        lp.setCourseUuid(req.courseUuid());
        lp.setPercentComplete(Math.min(100, Math.max(0, req.percentComplete())));
        if (req.completed() && !lp.isCompleted()) {
            lp.setCompleted(true);
            lp.setCompletedAt(Instant.now());
            lp.setPercentComplete(100);
        }
        progress.save(lp);
        return computeForCourse(studentUuid, req.courseUuid());
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse computeForCourse(String studentUuid, String courseUuid) {
        long mandatory = lessons.countByModuleCourseUuidAndMandatoryTrue(courseUuid);
        long completed = progress.countByStudentUuidAndCourseUuidAndCompletedTrue(studentUuid, courseUuid);
        int percent = mandatory == 0 ? 0 : (int) Math.round(100.0 * completed / mandatory);
        boolean lessonsDone = mandatory > 0 && completed >= mandatory;
        boolean examSatisfied = assessments.hasSatisfiedExamGate(studentUuid, courseUuid);
        boolean courseCompleted = lessonsDone && examSatisfied;
        return new CourseProgressResponse(courseUuid, mandatory, completed, percent, courseCompleted);
    }

    /** Course completion is the gate Certification consults before issuing a certificate. */
    @Transactional(readOnly = true)
    public boolean isCourseCompleted(String studentUuid, String courseUuid) {
        return computeForCourse(studentUuid, courseUuid).courseCompleted();
    }

    /** Per-lesson progress records for a student in a course. */
    @Transactional(readOnly = true)
    public List<LessonProgressView> lessonProgress(String studentUuid, String courseUuid) {
        return progress.findByStudentUuidAndCourseUuid(studentUuid, courseUuid).stream()
                .map(lp -> new LessonProgressView(lp.getLessonUuid(), lp.isCompleted(), lp.getPercentComplete()))
                .toList();
    }
}
