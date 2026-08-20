package tz.go.tirdo.teltp.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @Column(nullable = false, length = 36)
    private String subscriberUuid;

    @Column(nullable = false, length = 36)
    private String pricingPlanUuid;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate currentPeriodEnd;

    @Column(nullable = false)
    private boolean active = true;

    private boolean autoRenew = true;
}
