package tz.go.tirdo.teltp.billing.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.billing.dto.BillingDtos.GepgCallbackRequest;
import tz.go.tirdo.teltp.billing.service.BillingService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Public GePG payment callback. Rejects the call unless the body is signed with
 * the shared secret in teltp.integration.gepg.callback-secret.
 * Header: X-Teltp-Signature: hex HMAC-SHA256 of controlNumber|providerReference|paidAmount.
 */
@RestController
@RequestMapping("/billing/gepg")
public class GepgCallbackController {

    private final BillingService billing;

    @Value("${teltp.integration.gepg.callback-secret:}")
    private String callbackSecret;

    public GepgCallbackController(BillingService billing) {
        this.billing = billing;
    }

    @PostMapping("/callback")
    public String callback(@RequestBody GepgCallbackRequest req,
                           @RequestHeader(value = "X-Teltp-Signature", required = false) String signature) {
        if (callbackSecret == null || callbackSecret.isBlank()) {
            throw new AccessDeniedException("GePG callback secret is not configured");
        }
        String amount = req.paidAmount() == null ? "" : req.paidAmount().toPlainString();
        String payload = nullToEmpty(req.controlNumber()) + "|" + nullToEmpty(req.providerReference()) + "|" + amount;
        String expected = hmac(callbackSecret, payload);
        if (signature == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("GePG callback signature rejected");
        }
        billing.confirmByControlNumber(req.controlNumber(), req.providerReference());
        return "<gepgPmtSpInfoAck><TrxStsCode>7101</TrxStsCode></gepgPmtSpInfoAck>";
    }

    static String hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
