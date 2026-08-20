package tz.go.tirdo.teltp.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.billing.entity.PricingPlan;

import java.util.Optional;

public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {
    Optional<PricingPlan> findByUuid(String uuid);
}
