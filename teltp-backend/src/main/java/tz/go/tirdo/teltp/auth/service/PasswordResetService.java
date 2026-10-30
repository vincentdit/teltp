package tz.go.tirdo.teltp.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.audit.service.AuditService;
import tz.go.tirdo.teltp.auth.entity.PasswordResetToken;
import tz.go.tirdo.teltp.auth.entity.User;
import tz.go.tirdo.teltp.auth.repository.PasswordResetTokenRepository;
import tz.go.tirdo.teltp.auth.repository.UserRepository;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.integration.notification.EmailNotificationChannel;
import tz.go.tirdo.teltp.security.TokenHasher;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Self-service password reset. The response never reveals whether the email exists.
 * The link is emailed when the email channel is enabled.
 */
@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(30);

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final EmailNotificationChannel email;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;
    private final PasswordEncoder encoder;

    @Value("${teltp.security.public-base-url:http://localhost:9090}")
    private String publicBaseUrl;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                EmailNotificationChannel email, RefreshTokenService refreshTokens,
                                AuditService audit, PasswordEncoder encoder) {
        this.users = users;
        this.tokens = tokens;
        this.email = email;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.encoder = encoder;
    }

    @Transactional
    public void request(String emailAddress) {
        users.findByEmail(emailAddress).ifPresent(user -> {
            byte[] buf = new byte[32];
            RANDOM.nextBytes(buf);
            String raw = HexFormat.of().formatHex(buf);
            PasswordResetToken row = new PasswordResetToken();
            row.setUsername(user.getUsername());
            row.setTokenHash(TokenHasher.sha256(raw));
            row.setExpiresAt(Instant.now().plus(TTL));
            tokens.save(row);
            String link = publicBaseUrl.replaceAll("/$", "") + "/reset-password?token=" + raw;
            email.sendToAddress(user.getEmail(), "Reset your TeLTP password",
                    "Open this link within 30 minutes to choose a new password:\n" + link);
            audit.record("PASSWORD_RESET_REQUESTED", "User", user.getUuid(), user.getUsername());
        });
    }

    @Transactional
    public void complete(String rawToken, String newPassword) {
        PasswordPolicy.require(newPassword);
        PasswordResetToken row = tokens.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Reset link is invalid"));
        if (row.isUsed() || row.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Reset link has expired");
        }
        User user = users.findByUsername(row.getUsername())
                .orElseThrow(() -> new BusinessRuleException("Reset link is invalid"));
        user.setPasswordHash(encoder.encode(newPassword));
        users.save(user);
        row.setUsed(true);
        tokens.save(row);
        refreshTokens.revokeAll(user.getUsername());
        audit.record("PASSWORD_RESET_COMPLETED", "User", user.getUuid(), user.getUsername());
    }
}
