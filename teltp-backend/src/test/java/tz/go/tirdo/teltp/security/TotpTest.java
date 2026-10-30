package tz.go.tirdo.teltp.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpTest {

    @Test
    void generatedCodeVerifies() {
        String secret = Totp.generateSecret();
        byte[] key = Totp.decodeBase32(secret);
        long step = java.time.Instant.now().getEpochSecond() / 30;
        String code = Totp.hotp(key, step);
        assertThat(Totp.verify(secret, code)).isTrue();
        assertThat(Totp.verify(secret, "abcdef")).isFalse();
    }

    @Test
    void rejectsMalformedCode() {
        assertThat(Totp.verify(Totp.generateSecret(), "abcdef")).isFalse();
        assertThat(Totp.verify(Totp.generateSecret(), null)).isFalse();
    }
}
