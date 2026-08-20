package tz.go.tirdo.teltp.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tz.go.tirdo.teltp.common.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment is append-only: it carries only @CreatedDate (no mutable audit fields), matching the
 * CIAP convention that payment records are immutable transaction facts.
 */
@Getter
@Setter
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, unique = true, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;  // TELTP-PAY-YYYY-00001

    @Column(nullable = false, length = 36)
    private String invoiceUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Embedded
    private Money amount = Money.zeroTzs();

    /** Control number / provider transaction reference (e.g. GePG control number). */
    @Column(length = 60)
    private String controlNumber;

    @Column(length = 60)
    private String providerReference;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant confirmedAt;
}
