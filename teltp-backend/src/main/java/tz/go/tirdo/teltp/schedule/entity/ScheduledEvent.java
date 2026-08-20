package tz.go.tirdo.teltp.schedule.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "scheduled_events")
public class ScheduledEvent extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;  // TELTP-EVT-YYYY-00001

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    /** Optional link to a course (sessions usually belong to a course; standalone webinars may not). */
    @Column(length = 36)
    private String courseUuid;

    @Column(length = 36)
    private String hostUuid;  // instructor / presenter

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    private Integer capacity;  // null = unlimited

    /** Webinars may charge a registration fee; references a PricingPlan, or null when free. */
    @Column(length = 36)
    private String pricingPlanUuid;

    // ---- virtual classroom (links/metadata only) ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MeetingProvider provider = MeetingProvider.MANUAL;

    @Column(length = 500)
    private String joinUrl;

    @Column(length = 100)
    private String externalMeetingId;

    @Column(length = 500)
    private String recordingUrl;  // stored as a link, not rebuilt
}
