package tz.go.tirdo.teltp.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Server-side refresh session. Only the SHA-256 hash of the token is stored. */
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    /** False when the user did not choose Remember me: the cookie is a browser session cookie. */
    @Column(nullable = false)
    private boolean persistent = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
