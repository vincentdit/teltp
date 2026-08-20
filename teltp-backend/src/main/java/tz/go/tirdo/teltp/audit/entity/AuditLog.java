package tz.go.tirdo.teltp.audit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Dedicated action-trail entity (who did what, when) — distinct from BaseEntity row provenance.
 * Append-only; supports the Security Framework "Audit Trails" requirement and PDPA accountability.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor"),
        @Index(name = "idx_audit_action", columnList = "action")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, unique = true, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(nullable = false, length = 100)
    private String actor;        // username or "system"

    @Column(nullable = false, length = 80)
    private String action;       // e.g. COURSE_PUBLISHED, PAYMENT_CONFIRMED

    @Column(length = 60)
    private String targetType;

    @Column(length = 36)
    private String targetUuid;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String detail;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();
}
