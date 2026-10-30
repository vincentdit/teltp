package tz.go.tirdo.teltp.progress.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.assessment.service.AssessmentService;
import tz.go.tirdo.teltp.catalog.entity.DeliveryMode;
import tz.go.tirdo.teltp.catalog.repository.LessonRepository;
import tz.go.tirdo.teltp.catalog.service.CourseService;
import tz.go.tirdo.teltp.common.event.CourseCompletionCandidateEvent;
import tz.go.tirdo.teltp.progress.dto.ProgressDtos.*;
import tz.go.tirdo.teltp.progress.entity.LessonProgress;
import tz.go.tirdo.teltp.progress.repository.LessonProgressRepository;
import tz.go.tirdo.teltp.schedule.entity.EventType;
import tz.go.tirdo.teltp.schedule.entity.ScheduledEvent;
import tz.go.tirdo.teltp.schedule.repository.AttendanceRepository;
import tz.go.tirdo.teltp.schedule.repository.EventRepository;

import java.time.Instant;
import java.util.List;

/**
 * Tracks per-lesson completion and computes course completion. Online courses finish on
 * lessons and exams. In-person and hybrid courses also require attendance at every
 * training session and workshop scheduled for the course.
 */
@Service
public class ProgressService {

    private final LessonProgressRepository progress;
    private final LessonRepository lessons;
    private final AssessmentService assessments;
    private final CourseService courses;
    private final EventRepository events;
    private final AttendanceRepository attendance;
    private final ApplicationEventPublisher publisher;

    public ProgressService(LessonProgressRepository progress, LessonRepository lessons,
                           AssessmentService assessments, CourseService courses,
                           EventRepository events, AttendanceRepository attendance,
                           ApplicationEventPublisher publisher) {
        this.progress = progress;
        this.lessons = lessons;
        this.assessments = assessments;
        this.courses = courses;
        this.events = events;
        this.attendance = attendance;
        this.publisher = publisher;
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
        CourseProgressResponse result = computeForCourse(studentUuid, req.courseUuid());
        // Finishing the last mandatory lesson can complete the course; let certification react.
        if (result.courseCompleted()) {
            publisher.publishEvent(new CourseCompletionCandidateEvent(
                    studentUuid, req.courseUuid(), req.lessonUuid()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse computeForCourse(String studentUuid, String courseUuid) {
        long mandatory = lessons.countByModuleCourseUuidAndMandatoryTrue(courseUuid);
        long completed = progress.countByStudentUuidAndCourseUuidAndCompletedTrue(studentUuid, courseUuid);
        int percent = mandatory == 0 ? 0 : (int) Math.round(100.0 * completed / mandatory);
        boolean lessonsDone = mandatory > 0 && completed >= mandatory;
        boolean examSatisfied = assessments.hasSatisfiedExamGate(studentUuid, courseUuid);
        boolean sessionsSatisfied = sessionsSatisfied(studentUuid, courseUuid);
        boolean courseCompleted = lessonsDone && examSatisfied && sessionsSatisfied;
        return new CourseProgressResponse(courseUuid, mandatory, completed, percent, courseCompleted);
    }

    /** In-person and hybrid courses require presence at each training session and workshop. */
    private boolean sessionsSatisfied(String studentUuid, String courseUuid) {
        DeliveryMode mode = courses.getEntity(courseUuid).getDeliveryMode();
        if (mode == DeliveryMode.ONLINE) return true;
        List<ScheduledEvent> required = events.findByCourseUuid(courseUuid).stream()
                .filter(e -> e.getType() != EventType.WEBINAR)
                .toList();
        if (required.isEmpty()) return true;
        for (ScheduledEvent event : required) {
            boolean present = attendance.findByEventUuidAndParticipantUuid(event.getUuid(), studentUuid)
                    .map(a -> a.isPresent())
                    .orElse(false);
            if (!present) return false;
        }
        return true;
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
