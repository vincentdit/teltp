package tz.go.tirdo.teltp.enrollment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.time.LocalDate;

/** A scheduled run of a course (esp. instructor-led / hybrid), with optional capacity. */
@Getter
@Setter
@Entity
@Table(name = "cohorts")
public class Cohort extends BaseEntity {

    @Column(nullable = false, length = 36)
    private String courseUuid;

    @Column(nullable = false, length = 150)
    private String name;

    private LocalDate startDate;
    private LocalDate endDate;

    /** Null means unlimited (typical for self-paced online). */
    private Integer capacity;
}
