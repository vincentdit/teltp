package tz.go.tirdo.teltp.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

/** Option for a multiple-choice question. Correctness is never serialized to students. */
@Getter
@Setter
@Entity
@Table(name = "answer_options")
public class AnswerOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "option_text", nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private boolean correct = false;
}
