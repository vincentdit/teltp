package tz.go.tirdo.teltp.billing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.*;
import tz.go.tirdo.teltp.billing.entity.ChargeModel;
import tz.go.tirdo.teltp.billing.entity.PricingPlan;
import tz.go.tirdo.teltp.billing.repository.PricingPlanRepository;
import tz.go.tirdo.teltp.common.Money;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

@Service
public class PricingService {

    private final PricingPlanRepository plans;

    public PricingService(PricingPlanRepository plans) {
        this.plans = plans;
    }

    @Transactional
    public PricingPlanResponse create(CreatePricingPlanRequest req) {
        PricingPlan p = new PricingPlan();
        p.setName(req.name());
        p.setChargeModel(ChargeModel.valueOf(req.chargeModel()));
        p.setPrice(Money.tzs(req.price()));
        p.setCycleDays(req.cycleDays());
        return toResponse(plans.save(p));
    }

    @Transactional(readOnly = true)
    public PricingPlanResponse get(String uuid) {
        return toResponse(require(uuid));
    }

    public PricingPlan getEntity(String uuid) { return require(uuid); }

    private PricingPlan require(String uuid) {
        return plans.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("PricingPlan", uuid));
    }

    private PricingPlanResponse toResponse(PricingPlan p) {
        return new PricingPlanResponse(p.getUuid(), p.getName(), p.getChargeModel().name(),
                p.getPrice().amount(), p.getPrice().currency(), p.getCycleDays(), p.isActive());
    }
}
