package tz.go.tirdo.teltp.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.auth.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
