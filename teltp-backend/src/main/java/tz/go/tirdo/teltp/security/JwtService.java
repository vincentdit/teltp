package tz.go.tirdo.teltp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import tz.go.tirdo.teltp.config.TeltpProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessValidity;
    private final long refreshValidity;

    public JwtService(TeltpProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getSecurity().getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessValidity = props.getSecurity().getJwt().getAccessTokenValiditySeconds();
        this.refreshValidity = props.getSecurity().getJwt().getRefreshTokenValiditySeconds();
    }

    public String generateAccessToken(String username, List<String> roles) {
        return build(username, Map.of("roles", roles, "type", "access"), accessValidity);
    }

    public String generateRefreshToken(String username) {
        return build(username, Map.of("type", "refresh"), refreshValidity);
    }

    public String generateMfaChallenge(String username) {
        return build(username, Map.of("type", "mfa"), 300);
    }

    public long accessTtlSeconds() { return accessValidity; }

    public long refreshTtlSeconds() { return refreshValidity; }

    private String build(String subject, Map<String, Object> claims, long validitySeconds) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validitySeconds * 1000))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        List<String> roles = parse(token, c -> (List<String>) c.get("roles"));
        return roles == null ? List.of() : roles;
    }

    public boolean isAccessToken(String token) {
        return isType(token, "access");
    }

    public boolean isRefreshToken(String token) {
        return isType(token, "refresh");
    }

    public boolean isMfaChallenge(String token) {
        return isType(token, "mfa");
    }

    public boolean isValid(String token) {
        try {
            return parse(token, Claims::getExpiration).after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isType(String token, String expected) {
        try {
            Claims claims = parse(token, c -> c);
            return expected.equals(claims.get("type")) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T parse(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return resolver.apply(claims);
    }
}
