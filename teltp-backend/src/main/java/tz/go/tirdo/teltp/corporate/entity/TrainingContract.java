package tz.go.tirdo.teltp.corporate.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;
import tz.go.tirdo.teltp.common.Money;

import java.time.LocalDate;

/**
 * A negotiated training engagement for an organization: scope -> quotation -> approval ->
 * delivery -> completion. Billing for the contract is raised through the Billing module
 * (B2B contract pattern), so this entity holds the contract value and links to invoices by uuid.
 */
@Getter
@Setter
@Entity
@Table(name = "training_contracts")
public class TrainingContract extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;  // TELTP-CON-YYYY-00001

    @Column(nullable = false, length = 36)
    private String organizationUuid;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status = ContractStatus.DRAFT;

    @Embedded
    private Money contractValue = Money.zeroTzs();

    private Integer participantTarget;

    private LocalDate startDate;
    private LocalDate endDate;

    /** Invoice raised against this contract (Billing module uuid). */
    @Column(length = 36)
    private String invoiceUuid;
}
