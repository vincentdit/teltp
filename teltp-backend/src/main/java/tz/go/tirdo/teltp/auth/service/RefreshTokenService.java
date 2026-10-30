package tz.go.tirdo.teltp.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.auth.entity.RefreshToken;
import tz.go.tirdo.teltp.auth.repository.RefreshTokenRepository;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.security.TokenHasher;

import java.time.Instant;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository tokens;

    public RefreshTokenService(RefreshTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Transactional
    public void store(String username, String rawToken, Instant expiresAt) {
        RefreshToken row = new RefreshToken();
        row.setUsername(username);
        row.setTokenHash(TokenHasher.sha256(rawToken));
        row.setExpiresAt(expiresAt);
        tokens.save(row);
    }

    /** Accepts a live, unrevoked refresh token for this user. */
    @Transactional(readOnly = true)
    public void requireActive(String username, String rawToken) {
        RefreshToken row = tokens.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Refresh token is not recognized"));
        if (!username.equals(row.getUsername()) || row.isRevoked() || row.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Refresh token is no longer valid");
        }
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        tokens.findByTokenHash(TokenHasher.sha256(rawToken)).ifPresent(row -> {
            row.setRevoked(true);
            tokens.save(row);
        });
    }

    @Transactional
    public void revokeAll(String username) {
        for (RefreshToken row : tokens.findByUsernameAndRevokedFalse(username)) {
            row.setRevoked(true);
            tokens.save(row);
        }
    }
}
