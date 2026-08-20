package tz.go.tirdo.teltp.assessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "assessments")
public class Assessment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;  // TELTP-ASMT-YYYY-00001

    @Column(nullable = false, length = 36)
    private String courseUuid;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AssessmentType type;

    /** Percentage required to pass (0-100). */
    private int passMark = 50;

    private Integer timeLimitMinutes;

    /** Examination fee is a Billing concern; references a PricingPlan uuid, or null when free. */
    @Column(length = 36)
    private String pricingPlanUuid;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();
}
