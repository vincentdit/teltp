package tz.go.tirdo.teltp.billing.controller;

import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.GepgCallbackRequest;
import tz.go.tirdo.teltp.billing.service.BillingService;

/**
 * Public, unauthenticated GePG payment-confirmation callback (whitelisted in SecurityConfig).
 * v1 accepts a minimal JSON shape; the real integration would verify the signed GePG payload
 * before confirming. Kept deliberately thin so the verification logic slots in at one place.
 */
@RestController
@RequestMapping("/billing/gepg")
public class GepgCallbackController {

    private final BillingService billing;

    public GepgCallbackController(BillingService billing) {
        this.billing = billing;
    }

    @PostMapping("/callback")
    public String callback(@RequestBody GepgCallbackRequest req) {
        // TODO seam: verify GePG signature / digital certificate before trusting the payload.
        billing.confirmByControlNumber(req.controlNumber(), req.providerReference());
        // GePG expects a specific acknowledgement envelope; a plain ack stands in for v1.
        return "<gepgPmtSpInfoAck><TrxStsCode>7101</TrxStsCode></gepgPmtSpInfoAck>";
    }
}
