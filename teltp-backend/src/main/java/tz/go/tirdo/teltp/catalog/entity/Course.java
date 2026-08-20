package tz.go.tirdo.teltp.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;  // TELTP-CRS-YYYY-00001

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryMode deliveryMode = DeliveryMode.ONLINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    private Integer durationHours;

    /** Optional instructor (User uuid). Resolved via auth module hook to avoid hard coupling. */
    @Column(length = 36)
    private String instructorUuid;

    /** Pricing is a Billing concern; a course references a PricingPlan uuid, or null when free. */
    @Column(length = 36)
    private String pricingPlanUuid;

    @ManyToMany
    @JoinTable(name = "course_prerequisites",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id"))
    private Set<Course> prerequisites = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<CourseModule> modules = new ArrayList<>();
}
