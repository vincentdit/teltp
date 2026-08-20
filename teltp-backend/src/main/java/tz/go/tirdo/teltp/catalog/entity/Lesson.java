package tz.go.tirdo.teltp.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id")
    private CourseModule module;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String content;

    @Column(nullable = false)
    private int orderIndex;

    private Integer estimatedMinutes;

    /** Marks a lesson as mandatory for course-completion calculation in the Progress module. */
    private boolean mandatory = true;
}
