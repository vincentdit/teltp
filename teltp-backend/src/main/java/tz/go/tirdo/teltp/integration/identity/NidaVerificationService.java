package tz.go.tirdo.teltp.integration.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;

/**
 * Calls a configured NIDA (or identity-broker) HTTP endpoint.
 * When the integration is off, the result is unverified and no identity is invented.
 * Expected JSON: {"verified":true,"fullName":"..."}.
 */
@Service
public class NidaVerificationService {

    @Value("${teltp.integration.nida.enabled:false}")
    private boolean enabled;

    @Value("${teltp.integration.nida.endpoint:}")
    private String endpoint;

    public boolean isEnabled() { return enabled && endpoint != null && !endpoint.isBlank(); }

    public record NidaResult(boolean verified, String fullName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NidaPayload(boolean verified, String fullName) {}

    public NidaResult verify(String nationalId) {
        if (!enabled) return new NidaResult(false, null);
        if (endpoint == null || endpoint.isBlank()) {
            throw new BusinessRuleException("NIDA is enabled but teltp.integration.nida.endpoint is empty");
        }
        NidaPayload payload = RestClient.create().post()
                .uri(endpoint)
                .body(java.util.Map.of("nationalId", nationalId))
                .retrieve()
                .body(NidaPayload.class);
        if (payload == null) return new NidaResult(false, null);
        return new NidaResult(payload.verified(), payload.fullName());
    }
}
