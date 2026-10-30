package tz.go.tirdo.teltp.billing.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.*;
import tz.go.tirdo.teltp.billing.entity.PricingPlan;
import tz.go.tirdo.teltp.billing.entity.Subscription;
import tz.go.tirdo.teltp.billing.repository.SubscriptionRepository;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.notification.entity.NotificationType;
import tz.go.tirdo.teltp.notification.service.NotificationService;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptions;
    private final PricingService pricing;
    private final BillingService billing;
    private final NotificationService notifications;

    public SubscriptionService(SubscriptionRepository subscriptions, PricingService pricing,
                               BillingService billing, NotificationService notifications) {
        this.subscriptions = subscriptions;
        this.pricing = pricing;
        this.billing = billing;
        this.notifications = notifications;
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
     * Daily renewal sweep. Raises an invoice for each due auto-renew subscription, then advances the period.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processRenewals() {
        List<Subscription> due = subscriptions
                .findByActiveTrueAndAutoRenewTrueAndCurrentPeriodEndBefore(LocalDate.now());
        for (Subscription s : due) {
            PricingPlan plan = pricing.getEntity(s.getPricingPlanUuid());
            int cycle = plan.getCycleDays() == null ? 30 : plan.getCycleDays();
            billing.createInvoice(new CreateInvoiceRequest(s.getSubscriberUuid(), "USER", List.of(
                    new LineItemRequest(plan.getName() + " renewal", "SUBSCRIPTION", s.getUuid(),
                            1, plan.getPrice().amount()))));
            s.setCurrentPeriodEnd(s.getCurrentPeriodEnd().plusDays(cycle));
            subscriptions.save(s);
            notifications.notify(s.getSubscriberUuid(), NotificationType.GENERAL,
                    "Subscription renewal", "An invoice was issued for " + plan.getName());
        }
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(s.getUuid(), s.getSubscriberUuid(), s.getPricingPlanUuid(),
                s.getStartDate(), s.getCurrentPeriodEnd(), s.isActive(), s.isAutoRenew());
    }
}
