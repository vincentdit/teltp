package tz.go.tirdo.teltp.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tz.go.tirdo.teltp.billing.entity.PaymentChannel;

/** Mobile money (M-Pesa / Tigo Pesa / Airtel Money) seam. Disabled in v1. */
@Component
public class MobileMoneyPaymentMethod implements PaymentMethod {

    @Value("${teltp.integration.mobile-money.enabled:false}")
    private boolean enabled;

    @Override
    public PaymentChannel channel() { return PaymentChannel.MOBILE_MONEY; }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiation initiation) {
        if (!enabled) {
            return new PaymentInitiationResult(true, null, "MNO-STUB",
                    "A push prompt would be sent to " + initiation.payerPhone());
        }
        throw new UnsupportedOperationException("Live mobile money integration not provisioned");
    }
}
