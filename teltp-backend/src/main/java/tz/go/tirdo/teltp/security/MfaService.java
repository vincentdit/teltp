package tz.go.tirdo.teltp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * MFA seam. v1 ships disabled (teltp.security.mfa.enabled=false). The login flow
 * consults {@link #isRequired(String)} so a TOTP / SMS-OTP second factor can be
 * added later without restructuring authentication. No factor is implemented yet.
 */
@Service
public class MfaService {

    @Value("${teltp.security.mfa.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() { return enabled; }

    /** Whether the given user must complete a second factor. Always false in v1. */
    public boolean isRequired(String username) {
        return enabled;  // future: per-user MFA enrollment lookup
    }

    /** Verify a submitted OTP. Seam only — not implemented in v1. */
    public boolean verify(String username, String otp) {
        throw new UnsupportedOperationException("MFA not implemented in v1");
    }
}
