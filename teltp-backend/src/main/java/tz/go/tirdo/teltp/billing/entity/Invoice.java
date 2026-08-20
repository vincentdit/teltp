package tz.go.tirdo.teltp.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;
import tz.go.tirdo.teltp.common.Money;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;  // TELTP-INV-YYYY-00001

    /** Payer: an individual user uuid or an organization uuid (for B2B contracts). */
    @Column(nullable = false, length = 36)
    private String payerUuid;

    @Column(nullable = false, length = 20)
    private String payerType;  // USER | ORGANIZATION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Embedded
    private Money total = Money.zeroTzs();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    public void recalculateTotal() {
        Money sum = Money.zeroTzs();
        for (InvoiceLineItem li : lineItems) sum = sum.plus(li.lineTotal());
        this.total = sum;
    }
}
