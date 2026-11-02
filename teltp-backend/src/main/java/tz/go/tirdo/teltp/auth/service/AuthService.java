package tz.go.tirdo.teltp.auth.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.audit.service.AuditService;
import tz.go.tirdo.teltp.auth.dto.AuthDtos.*;
import tz.go.tirdo.teltp.auth.entity.Role;
import tz.go.tirdo.teltp.auth.entity.RoleName;
import tz.go.tirdo.teltp.auth.entity.User;
import tz.go.tirdo.teltp.auth.repository.RoleRepository;
import tz.go.tirdo.teltp.auth.repository.UserRepository;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.DuplicateResourceException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.security.JwtService;
import tz.go.tirdo.teltp.security.MfaService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_FOR = Duration.ofMinutes(15);
    /** Equalises response time when the account does not exist. Not a real user password. */
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MfaService mfa;
    private final UserMapper mapper;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;
    private final EmailVerificationService verification;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                       JwtService jwt, MfaService mfa, UserMapper mapper,
                       RefreshTokenService refreshTokens, AuditService audit,
                       EmailVerificationService verification) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mfa = mfa;
        this.mapper = mapper;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.verification = verification;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (!req.dataProcessingConsent())
            throw new BusinessRuleException("Accept the terms and data-processing consent to create an account");
        PasswordPolicy.require(req.password());
        if (users.existsByUsernameIgnoreCase(req.username().trim()))
            throw new DuplicateResourceException("That username is already taken. Choose another.");
        if (users.existsByEmailIgnoreCase(req.email().trim()))
            throw new DuplicateResourceException("That email is already registered. Sign in or reset your password.");

        boolean verifyByEmail = verification.mailEnabled();
        User user = new User();
        user.setUsername(req.username().trim());
        user.setEmail(req.email().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhoneNumber(req.phoneNumber());
        user.setProfession(req.profession());
        user.setDataProcessingConsent(true);
        user.setEmailVerified(!verifyByEmail);

        Role studentRole = roles.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Default STUDENT role missing"));
        user.getRoles().add(studentRole);

        User saved = users.save(user);
        if (verifyByEmail) verification.send(saved);
        audit.record("USER_REGISTERED", "User", saved.getUuid(), saved.getUsername());
        return mapper.toResponse(saved);
    }

    @Transactional
    public AuthSession login(LoginRequest req) {
        User user = findForLogin(req.username().trim());
        if (user == null) {
            encoder.matches(req.password(), DUMMY_HASH);
            throw new BadCredentialsException("Invalid credentials");
        }
        ensureNotLocked(user);
        if (!encoder.matches(req.password(), user.getPasswordHash())) failLogin(user);
        if (!user.isActive()) throw new DisabledException("This account is disabled. Contact TIRDO if you need access.");
        if (!user.isEmailVerified())
            throw new DisabledException("Confirm your email before signing in.");
        clearFailures(user);
        if (mfa.isRequired(user)) {
            TokenResponse challenge = new TokenResponse(
                    null, null, "Bearer", 0, true, jwt.generateMfaChallenge(user.getUsername()));
            return new AuthSession(challenge, req.rememberMe());
        }
        audit.record("LOGIN", "User", user.getUuid(), user.getUsername());
        return new AuthSession(issueTokens(user, req.rememberMe()), req.rememberMe());
    }

    @Transactional
    public AuthSession verifyMfa(MfaVerifyRequest req) {
        if (!jwt.isMfaChallenge(req.challenge()))
            throw new BadCredentialsException("MFA challenge is invalid or expired");
        String username = jwt.extractUsername(req.challenge());
        User user = users.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!user.isActive()) throw new DisabledException("Account is disabled");
        if (!user.isEmailVerified())
            throw new DisabledException("Confirm your email before signing in.");
        if (!mfa.verify(user, req.code()))
            throw new BadCredentialsException("Invalid authentication code");
        audit.record("LOGIN_MFA", "User", user.getUuid(), user.getUsername());
        return new AuthSession(issueTokens(user, req.rememberMe()), req.rememberMe());
    }

    @Transactional
    public AuthSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || !jwt.isRefreshToken(rawRefreshToken))
            throw new BusinessRuleException("Invalid refresh token");
        String username = jwt.extractUsername(rawRefreshToken);
        var current = refreshTokens.requireActive(username, rawRefreshToken);
        boolean persistent = current.isPersistent();
        refreshTokens.revoke(rawRefreshToken);
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (!user.isActive()) throw new DisabledException("This account is disabled. Contact TIRDO if you need access.");
        return new AuthSession(issueTokens(user, persistent), persistent);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
        audit.record("LOGOUT", "User", null, CurrentActor.orAnonymous());
    }

    @Transactional
    public MfaSetupResponse beginMfa(String username) {
        if (!mfa.enrollmentAllowed())
            throw new BusinessRuleException("MFA enrollment is disabled on this server");
        User user = requireUser(username);
        String secret = mfa.begin(user);
        users.save(user);
        String label = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String uri = "otpauth://totp/TeLTP:" + label + "?secret=" + secret + "&issuer=TeLTP&digits=6&period=30";
        return new MfaSetupResponse(secret, uri);
    }

    @Transactional
    public void confirmMfa(String username, String code) {
        User user = requireUser(username);
        if (user.getMfaSecret() == null || !mfa.verify(user, code))
            throw new BusinessRuleException("Invalid authentication code");
        user.setMfaEnabled(true);
        users.save(user);
        audit.record("MFA_ENABLED", "User", user.getUuid(), username);
    }

    @Transactional
    public void disableMfa(String username, String code) {
        User user = requireUser(username);
        if (!mfa.verify(user, code))
            throw new BusinessRuleException("Invalid authentication code");
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        users.save(user);
        audit.record("MFA_DISABLED", "User", user.getUuid(), username);
    }

    @Transactional
    public UserResponse assignRoles(AssignRolesRequest req) {
        User user = users.findByUuid(req.userUuid())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.userUuid()));
        Set<Role> resolved = req.roles().stream()
                .map(r -> roles.findByName(RoleName.valueOf(r))
                        .orElseThrow(() -> new ResourceNotFoundException("Role", r)))
                .collect(Collectors.toSet());
        user.setRoles(resolved);
        User saved = users.save(user);
        audit.record("ROLES_ASSIGNED", "User", saved.getUuid(), String.join(",", req.roles()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public UserResponse resetPassword(String userUuid, String newPassword) {
        PasswordPolicy.require(newPassword);
        User user = users.findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", userUuid));
        user.setPasswordHash(encoder.encode(newPassword));
        refreshTokens.revokeAll(user.getUsername());
        audit.record("PASSWORD_RESET_BY_ADMIN", "User", user.getUuid(), user.getUsername());
        return mapper.toResponse(users.save(user));
    }

    private User requireUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    private User findForLogin(String identifier) {
        Optional<User> byName = users.findByUsernameIgnoreCase(identifier);
        if (byName.isPresent()) return byName.get();
        return users.findByEmailIgnoreCase(identifier).orElse(null);
    }

    private void ensureNotLocked(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new LockedException("This account is temporarily locked. Try again in 15 minutes.");
        }
    }

    private void failLogin(User user) {
        int next = user.getFailedLoginCount() + 1;
        if (next >= MAX_FAILURES) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(Instant.now().plus(LOCK_FOR));
            users.save(user);
            audit.record("LOGIN_LOCKED", "User", user.getUuid(), user.getUsername());
            throw new LockedException("This account is temporarily locked. Try again in 15 minutes.");
        }
        user.setFailedLoginCount(next);
        users.save(user);
        throw new BadCredentialsException("Invalid credentials");
    }

    private void clearFailures(User user) {
        if (user.getFailedLoginCount() == 0 && user.getLockedUntil() == null) return;
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        users.save(user);
    }

    private TokenResponse issueTokens(User user, boolean persistent) {
        List<String> roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String access = jwt.generateAccessToken(user.getUsername(), roleNames);
        String refresh = jwt.generateRefreshToken(user.getUsername());
        refreshTokens.store(user.getUsername(), refresh, Instant.now().plusSeconds(jwt.refreshTtlSeconds()), persistent);
        return new TokenResponse(access, refresh, "Bearer", jwt.accessTtlSeconds(), false, null);
    }
}
