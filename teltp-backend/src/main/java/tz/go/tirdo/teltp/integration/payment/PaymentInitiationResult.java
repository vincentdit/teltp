package tz.go.tirdo.teltp.integration.payment;

/** Result of initiating a payment: a control number / redirect the payer uses to pay. */
public record PaymentInitiationResult(
        boolean accepted,
        String controlNumber,
        String providerReference,
        String instructions) {}
