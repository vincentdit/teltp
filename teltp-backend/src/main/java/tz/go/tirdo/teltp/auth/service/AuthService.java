package tz.go.tirdo.teltp.auth.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
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
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MfaService mfa;
    private final UserMapper mapper;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                       JwtService jwt, MfaService mfa, UserMapper mapper,
                       RefreshTokenService refreshTokens, AuditService audit) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mfa = mfa;
        this.mapper = mapper;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (!req.dataProcessingConsent())
            throw new BusinessRuleException("Data processing consent is required");
        PasswordPolicy.require(req.password());
        if (users.existsByUsername(req.username())) throw new DuplicateResourceException("Username already taken");
        if (users.existsByEmail(req.email())) throw new DuplicateResourceException("Email already registered");

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setPhoneNumber(req.phoneNumber());
        user.setProfession(req.profession());
        user.setDataProcessingConsent(true);

        Role studentRole = roles.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Default STUDENT role missing"));
        user.getRoles().add(studentRole);

        User saved = users.save(user);
        audit.record("USER_REGISTERED", "User", saved.getUuid(), saved.getUsername());
        return mapper.toResponse(saved);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = users.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!user.isActive()) throw new DisabledException("Account is disabled");
        if (!encoder.matches(req.password(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");
        if (mfa.isRequired(user)) {
            return new TokenResponse(null, null, "Bearer", 0, true, jwt.generateMfaChallenge(user.getUsername()));
        }
        audit.record("LOGIN", "User", user.getUuid(), user.getUsername());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse verifyMfa(MfaVerifyRequest req) {
        if (!jwt.isMfaChallenge(req.challenge()))
            throw new BadCredentialsException("MFA challenge is invalid or expired");
        String username = jwt.extractUsername(req.challenge());
        User user = users.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!user.isActive()) throw new DisabledException("Account is disabled");
        if (!mfa.verify(user, req.code()))
            throw new BadCredentialsException("Invalid authentication code");
        audit.record("LOGIN_MFA", "User", user.getUuid(), user.getUsername());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || !jwt.isRefreshToken(rawRefreshToken))
            throw new BusinessRuleException("Invalid refresh token");
        String username = jwt.extractUsername(rawRefreshToken);
        refreshTokens.requireActive(username, rawRefreshToken);
        refreshTokens.revoke(rawRefreshToken);
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (!user.isActive()) throw new DisabledException("Account is disabled");
        return issueTokens(user);
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

    private TokenResponse issueTokens(User user) {
        List<String> roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String access = jwt.generateAccessToken(user.getUsername(), roleNames);
        String refresh = jwt.generateRefreshToken(user.getUsername());
        refreshTokens.store(user.getUsername(), refresh, Instant.now().plusSeconds(jwt.refreshTtlSeconds()));
        return new TokenResponse(access, refresh, "Bearer", jwt.accessTtlSeconds(), false, null);
    }
}
