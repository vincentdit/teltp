package tz.go.tirdo.teltp.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;
import tz.go.tirdo.teltp.common.Money;

/** Attachable to any chargeable thing (course, assessment, webinar, marketplace item). */
@Getter
@Setter
@Entity
@Table(name = "pricing_plans")
public class PricingPlan extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeModel chargeModel;

    @Embedded
    private Money price;

    /** For SUBSCRIPTION/PERIODIC_RENEWAL: billing cycle length in days. */
    private Integer cycleDays;

    @Column(nullable = false)
    private boolean active = true;
}
