package tz.go.tirdo.teltp.integration.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * NIDA (National Identification Authority) verification seam. Disabled in v1. When enabled and
 * provisioned with NIDA API credentials, this verifies a national ID against the NIDA registry
 * and returns a verified full name for certificate-holder identity / KYC.
 */
@Service
public class NidaVerificationService {

    @Value("${teltp.integration.nida.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() { return enabled; }

    public record NidaResult(boolean verified, String fullName) {}

    public NidaResult verify(String nationalId) {
        if (!enabled) {
            // Seam: no live verification. Treat as unverified rather than fabricating identity.
            return new NidaResult(false, null);
        }
        throw new UnsupportedOperationException("Live NIDA verification not provisioned");
    }
}
