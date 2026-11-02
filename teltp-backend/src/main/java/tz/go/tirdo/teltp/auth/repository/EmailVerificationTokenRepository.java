package tz.go.tirdo.teltp.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.auth.entity.EmailVerificationToken;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
