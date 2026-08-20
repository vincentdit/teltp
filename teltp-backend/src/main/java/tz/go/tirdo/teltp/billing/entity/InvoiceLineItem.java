package tz.go.tirdo.teltp.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;
import tz.go.tirdo.teltp.common.Money;

@Getter
@Setter
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false, length = 255)
    private String description;

    /** What is being billed: e.g. COURSE, EXAM, WEBINAR, SUBSCRIPTION, CONTRACT, MARKETPLACE. */
    @Column(nullable = false, length = 30)
    private String itemType;

    @Column(length = 36)
    private String itemUuid;

    @Column(nullable = false)
    private int quantity = 1;

    @Embedded
    private Money unitPrice = Money.zeroTzs();

    public Money lineTotal() {
        return unitPrice.times(quantity);
    }
}
