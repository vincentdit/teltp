package tz.go.tirdo.teltp.billing.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.*;
import tz.go.tirdo.teltp.billing.service.BillingService;
import tz.go.tirdo.teltp.billing.service.PricingService;
import tz.go.tirdo.teltp.billing.service.SubscriptionService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final PricingService pricing;
    private final BillingService billing;
    private final SubscriptionService subscriptions;
    private final UserService users;

    public BillingController(PricingService pricing, BillingService billing,
                             SubscriptionService subscriptions, UserService users) {
        this.pricing = pricing;
        this.billing = billing;
        this.subscriptions = subscriptions;
        this.users = users;
    }

    // --- pricing plans ---
    @PostMapping("/pricing-plans")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<PricingPlanResponse> createPlan(@Valid @RequestBody CreatePricingPlanRequest req) {
        return ApiResponse.ok("Pricing plan created", pricing.create(req));
    }

    @GetMapping("/pricing-plans/{uuid}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PricingPlanResponse> getPlan(@PathVariable String uuid) {
        return ApiResponse.ok(pricing.get(uuid));
    }

    // --- invoices ---
    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest req) {
        return ApiResponse.ok("Invoice created", billing.createInvoice(req));
    }

    @GetMapping("/invoices/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<InvoiceResponse>> myInvoices(Pageable pageable) {
        String uuid = users.uuidForUsername(CurrentUser.requireUsername());
        return ApiResponse.ok(billing.invoicesForPayer(uuid, pageable));
    }

    @GetMapping("/invoices/{uuid}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<InvoiceResponse> getInvoice(@PathVariable String uuid) {
        return ApiResponse.ok(billing.getInvoice(uuid));
    }

    @GetMapping("/invoices/payer/{payerUuid}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<InvoiceResponse>> payerInvoices(@PathVariable String payerUuid, Pageable pageable) {
        return ApiResponse.ok(billing.invoicesForPayer(payerUuid, pageable));
    }

    // --- payments ---
    @PostMapping("/payments/initiate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PaymentResponse> initiate(@Valid @RequestBody InitiatePaymentRequest req) {
        return ApiResponse.ok("Payment initiated", billing.initiatePayment(req));
    }

    // --- subscriptions ---
    @PostMapping("/subscriptions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SubscriptionResponse> subscribe(@Valid @RequestBody CreateSubscriptionRequest req) {
        return ApiResponse.ok("Subscribed", subscriptions.subscribe(req));
    }

    @GetMapping("/subscriptions/subscriber/{subscriberUuid}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<SubscriptionResponse>> subscriberSubs(@PathVariable String subscriberUuid) {
        return ApiResponse.ok(subscriptions.forSubscriber(subscriberUuid));
    }

    @PostMapping("/subscriptions/{uuid}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> cancel(@PathVariable String uuid) {
        subscriptions.cancel(uuid);
        return ApiResponse.ok("Subscription cancelled", null);
    }
}
