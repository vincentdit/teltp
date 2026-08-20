package tz.go.tirdo.teltp.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class BillingDtos {
    private BillingDtos() {}

    public record CreatePricingPlanRequest(
            @NotBlank String name, @NotNull String chargeModel,
            @NotNull BigDecimal price, Integer cycleDays) {}

    public record PricingPlanResponse(String uuid, String name, String chargeModel,
                                      BigDecimal price, String currency, Integer cycleDays, boolean active) {}

    public record LineItemRequest(@NotBlank String description, @NotBlank String itemType,
                                  String itemUuid, int quantity, @NotNull BigDecimal unitPrice) {}

    public record CreateInvoiceRequest(
            @NotBlank String payerUuid, @NotBlank String payerType, List<LineItemRequest> lineItems) {}

    public record InvoiceResponse(String uuid, String referenceNumber, String payerUuid, String payerType,
                                  String status, BigDecimal total, String currency, List<LineItemResponse> lineItems) {}
    public record LineItemResponse(String description, String itemType, String itemUuid,
                                   int quantity, BigDecimal unitPrice) {}

    public record InitiatePaymentRequest(@NotBlank String invoiceUuid, @NotNull String channel,
                                         String payerName, String payerPhone, String payerEmail) {}
    public record PaymentResponse(String uuid, String referenceNumber, String invoiceUuid, String channel,
                                  String status, BigDecimal amount, String controlNumber, String instructions) {}

    public record CreateSubscriptionRequest(@NotBlank String subscriberUuid, @NotBlank String pricingPlanUuid,
                                            boolean autoRenew) {}
    public record SubscriptionResponse(String uuid, String subscriberUuid, String pricingPlanUuid,
                                       LocalDate startDate, LocalDate currentPeriodEnd, boolean active, boolean autoRenew) {}

    /** Mirrors the minimal fields a GePG payment-confirmation callback carries. */
    public record GepgCallbackRequest(String controlNumber, String providerReference, BigDecimal paidAmount) {}
}
