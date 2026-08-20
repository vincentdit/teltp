package tz.go.tirdo.teltp.progress.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_uuid", "lesson_uuid"}))
public class LessonProgress extends BaseEntity {

    @Column(name = "student_uuid", nullable = false, length = 36)
    private String studentUuid;

    @Column(name = "lesson_uuid", nullable = false, length = 36)
    private String lessonUuid;

    @Column(name = "course_uuid", nullable = false, length = 36)
    private String courseUuid;

    @Column(nullable = false)
    private boolean completed = false;

    private Instant completedAt;

    /** 0-100 fractional progress within a lesson (e.g. video watch %). */
    private int percentComplete = 0;
}
