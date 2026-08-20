package tz.go.tirdo.teltp.assessment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    private Attempt attempt;

    @Column(nullable = false, length = 36)
    private String questionUuid;

    /** For MCQ: selected option uuid. */
    @Column(length = 36)
    private String selectedOptionUuid;

    /** For ESSAY/CASE_STUDY/PRACTICAL_TASK: free-text or storage-key submission. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String response;

    /** Points awarded (auto for MCQ, set by grader otherwise). */
    private Integer awardedPoints;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String graderFeedback;
}
