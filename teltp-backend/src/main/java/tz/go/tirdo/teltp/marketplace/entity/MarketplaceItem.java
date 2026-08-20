package tz.go.tirdo.teltp.marketplace.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

/** Digital products sold one-off or via subscription; pricing + checkout reuse the Billing engine. */
@Getter
@Setter
@Entity
@Table(name = "marketplace_items")
public class MarketplaceItem extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MarketplaceItemType type;

    /** PricingPlan uuid (ONE_TIME or SUBSCRIPTION). */
    @Column(nullable = false, length = 36)
    private String pricingPlanUuid;

    /** Storage key of the downloadable asset (delivered after purchase confirmation). */
    @Column(length = 500)
    private String storageKey;

    @Column(nullable = false)
    private boolean published = false;
}
