package tz.go.tirdo.teltp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tz.go.tirdo.teltp.auth.entity.User;

/**
 * Per-user TOTP. The platform flag only controls whether a user may enroll.
 * Login asks for a code only when that user has confirmed MFA.
 */
@Service
public class MfaService {

    @Value("${teltp.security.mfa.enabled:true}")
    private boolean enrollmentAllowed;

    public boolean enrollmentAllowed() { return enrollmentAllowed; }

    public boolean isRequired(User user) {
        return user.isMfaEnabled() && user.getMfaSecret() != null;
    }

    public boolean verify(User user, String code) {
        return Totp.verify(user.getMfaSecret(), code);
    }

    public String begin(User user) {
        String secret = Totp.generateSecret();
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        return secret;
    }
}
