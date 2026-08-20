package tz.go.tirdo.teltp.integration.payment;

import tz.go.tirdo.teltp.billing.entity.PaymentChannel;

/**
 * Seam for an external payment channel. v1 ships stub implementations for GePG, mobile money
 * and bank transfer; real SP integration (GePG SOAP/REST + signing certificates) drops in here
 * without changing the Billing service.
 */
public interface PaymentMethod {
    PaymentChannel channel();
    PaymentInitiationResult initiate(PaymentInitiation initiation);
}
