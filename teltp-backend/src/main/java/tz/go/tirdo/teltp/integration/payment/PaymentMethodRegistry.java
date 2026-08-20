package tz.go.tirdo.teltp.integration.payment;

import org.springframework.stereotype.Component;
import tz.go.tirdo.teltp.billing.entity.PaymentChannel;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the right PaymentMethod for a channel; new channels register automatically as beans. */
@Component
public class PaymentMethodRegistry {

    private final Map<PaymentChannel, PaymentMethod> methods;

    public PaymentMethodRegistry(List<PaymentMethod> methods) {
        this.methods = methods.stream().collect(Collectors.toMap(PaymentMethod::channel, Function.identity()));
    }

    public PaymentMethod forChannel(PaymentChannel channel) {
        PaymentMethod m = methods.get(channel);
        if (m == null) throw new BusinessRuleException("No payment method for channel " + channel);
        return m;
    }
}
