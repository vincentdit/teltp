package tz.go.tirdo.teltp.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.audit.service.AuditService;
import tz.go.tirdo.teltp.auth.entity.EmailVerificationToken;
import tz.go.tirdo.teltp.auth.entity.User;
import tz.go.tirdo.teltp.auth.repository.EmailVerificationTokenRepository;
import tz.go.tirdo.teltp.auth.repository.UserRepository;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.integration.notification.EmailNotificationChannel;
import tz.go.tirdo.teltp.security.TokenHasher;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Email confirmation for new accounts. The resend response never reveals whether the address exists.
 * When the mail channel is off, registration marks the account verified instead of issuing a dead link.
 */
@Service
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TTL = Duration.ofHours(24);

    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final EmailNotificationChannel email;
    private final AuditService audit;

    @Value("${teltp.security.public-base-url:http://localhost:9090}")
    private String publicBaseUrl;

    public EmailVerificationService(UserRepository users, EmailVerificationTokenRepository tokens,
                                    EmailNotificationChannel email, AuditService audit) {
        this.users = users;
        this.tokens = tokens;
        this.email = email;
        this.audit = audit;
    }

    public boolean mailEnabled() {
        return email.isEnabled();
    }

    @Transactional
    public void send(User user) {
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        String raw = HexFormat.of().formatHex(buf);
        EmailVerificationToken row = new EmailVerificationToken();
        row.setUsername(user.getUsername());
        row.setTokenHash(TokenHasher.sha256(raw));
        row.setExpiresAt(Instant.now().plus(TTL));
        tokens.save(row);
        String link = publicBaseUrl.replaceAll("/$", "") + "/verify-email?token=" + raw;
        email.sendToAddress(user.getEmail(), "Confirm your TeLTP account",
                "Open this link within 24 hours to confirm your email and sign in:\n" + link);
        audit.record("EMAIL_VERIFICATION_SENT", "User", user.getUuid(), user.getUsername());
    }

    /** Same reply whether or not the address is registered. */
    @Transactional
    public void resend(String emailAddress) {
        users.findByEmailIgnoreCase(emailAddress.trim()).ifPresent(user -> {
            if (!user.isEmailVerified()) send(user);
        });
    }

    @Transactional
    public void confirm(String rawToken) {
        EmailVerificationToken row = tokens.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Confirmation link is invalid"));
        if (row.isUsed() || row.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Confirmation link has expired");
        }
        User user = users.findByUsername(row.getUsername())
                .orElseThrow(() -> new BusinessRuleException("Confirmation link is invalid"));
        user.setEmailVerified(true);
        users.save(user);
        row.setUsed(true);
        tokens.save(row);
        audit.record("EMAIL_VERIFIED", "User", user.getUuid(), user.getUsername());
    }
}
