package tz.go.tirdo.teltp.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tz.go.tirdo.teltp.billing.entity.PaymentChannel;

/**
 * GePG (Government electronic Payment Gateway) seam. Disabled in v1. When enabled, this is where
 * the signed bill-submission request to GePG is built and the returned control number captured.
 * The asynchronous payment confirmation arrives at the whitelisted /billing/gepg/callback endpoint.
 */
@Component
public class GepgPaymentMethod implements PaymentMethod {

    @Value("${teltp.integration.gepg.enabled:false}")
    private boolean enabled;

    @Override
    public PaymentChannel channel() { return PaymentChannel.GEPG; }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiation initiation) {
        if (!enabled) {
            // Stub: emit a deterministic pseudo control number so the flow is testable end-to-end.
            String control = "99" + Math.abs(initiation.paymentUuid().hashCode() % 10_000_000_000L);
            return new PaymentInitiationResult(true, control, "GEPG-STUB",
                    "Pay via any bank or mobile wallet using GePG control number " + control);
        }
        throw new UnsupportedOperationException("Live GePG integration not provisioned (SP code + certs required)");
    }
}
