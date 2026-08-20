package tz.go.tirdo.teltp.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "attempts")
public class Attempt extends BaseEntity {

    @Column(nullable = false, length = 36)
    private String assessmentUuid;

    @Column(nullable = false, length = 36)
    private String studentUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    /** When a timed attempt must be submitted by; null for untimed assessments. */
    private Instant expiresAt;

    private Instant submittedAt;

    private Integer scorePercent;   // null until graded
    private Boolean passed;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttemptAnswer> answers = new ArrayList<>();
}
