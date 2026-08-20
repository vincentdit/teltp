package tz.go.tirdo.teltp.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tz.go.tirdo.teltp.billing.entity.PaymentChannel;

/** Manual/EFT bank transfer seam. Disabled in v1; reconciliation is operator-driven. */
@Component
public class BankTransferPaymentMethod implements PaymentMethod {

    @Value("${teltp.integration.bank-transfer.enabled:false}")
    private boolean enabled;

    @Override
    public PaymentChannel channel() { return PaymentChannel.BANK_TRANSFER; }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiation initiation) {
        return new PaymentInitiationResult(true, initiation.invoiceReference(), "BANK-STUB",
                "Transfer to TIRDO collection account quoting reference " + initiation.invoiceReference());
    }
}
