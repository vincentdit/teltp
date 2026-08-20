package tz.go.tirdo.teltp.billing.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.*;
import tz.go.tirdo.teltp.billing.entity.PricingPlan;
import tz.go.tirdo.teltp.billing.entity.Subscription;
import tz.go.tirdo.teltp.billing.repository.SubscriptionRepository;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptions;
    private final PricingService pricing;

    public SubscriptionService(SubscriptionRepository subscriptions, PricingService pricing) {
        this.subscriptions = subscriptions;
        this.pricing = pricing;
    }

    @Transactional
    public SubscriptionResponse subscribe(CreateSubscriptionRequest req) {
        PricingPlan plan = pricing.getEntity(req.pricingPlanUuid());
        int cycle = plan.getCycleDays() == null ? 30 : plan.getCycleDays();
        Subscription s = new Subscription();
        s.setSubscriberUuid(req.subscriberUuid());
        s.setPricingPlanUuid(req.pricingPlanUuid());
        s.setStartDate(LocalDate.now());
        s.setCurrentPeriodEnd(LocalDate.now().plusDays(cycle));
        s.setAutoRenew(req.autoRenew());
        return toResponse(subscriptions.save(s));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> forSubscriber(String subscriberUuid) {
        return subscriptions.findBySubscriberUuid(subscriberUuid).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void cancel(String uuid) {
        Subscription s = subscriptions.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", uuid));
        s.setActive(false);
        s.setAutoRenew(false);
        subscriptions.save(s);
    }

    /**
     * Daily renewal sweep. Identifies due auto-renew subscriptions and advances their period.
     * In production this would raise a renewal invoice via BillingService; left as a clear seam.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processRenewals() {
        List<Subscription> due = subscriptions
                .findByActiveTrueAndAutoRenewTrueAndCurrentPeriodEndBefore(LocalDate.now());
        for (Subscription s : due) {
            PricingPlan plan = pricing.getEntity(s.getPricingPlanUuid());
            int cycle = plan.getCycleDays() == null ? 30 : plan.getCycleDays();
            s.setCurrentPeriodEnd(s.getCurrentPeriodEnd().plusDays(cycle));
            subscriptions.save(s);
            // TODO seam: billingService.createInvoice(...) + notify subscriber of renewal charge
        }
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(s.getUuid(), s.getSubscriberUuid(), s.getPricingPlanUuid(),
                s.getStartDate(), s.getCurrentPeriodEnd(), s.isActive(), s.isAutoRenew());
    }
}
