package tz.go.tirdo.teltp.integration.payment;

import java.math.BigDecimal;

/** Inputs to initiate a payment with an external channel. */
public record PaymentInitiation(
        String paymentUuid,
        String invoiceReference,
        BigDecimal amount,
        String currency,
        String payerName,
        String payerPhone,
        String payerEmail) {}
