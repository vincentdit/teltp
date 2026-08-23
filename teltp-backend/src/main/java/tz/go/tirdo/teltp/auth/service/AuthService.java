package tz.go.tirdo.teltp.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.auth.dto.AuthDtos.*;
import tz.go.tirdo.teltp.auth.entity.Role;
import tz.go.tirdo.teltp.auth.entity.RoleName;
import tz.go.tirdo.teltp.auth.entity.User;
import tz.go.tirdo.teltp.auth.repository.RoleRepository;
import tz.go.tirdo.teltp.auth.repository.UserRepository;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.DuplicateResourceException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import tz.go.tirdo.teltp.security.JwtService;
import tz.go.tirdo.teltp.security.MfaService;

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

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                       JwtService jwt, MfaService mfa, UserMapper mapper) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mfa = mfa;
        this.mapper = mapper;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
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
        user.setDataProcessingConsent(req.dataProcessingConsent());

        Role studentRole = roles.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Default STUDENT role missing"));
        user.getRoles().add(studentRole);

        return mapper.toResponse(users.save(user));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        User user = users.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!user.isActive()) throw new DisabledException("Account is disabled");
        if (!encoder.matches(req.password(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");
        if (mfa.isRequired(user.getUsername()))
            throw new BusinessRuleException("MFA challenge required (not implemented in v1)");

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest req) {
        if (!jwt.isValid(req.refreshToken())) throw new BusinessRuleException("Invalid refresh token");
        String username = jwt.extractUsername(req.refreshToken());
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return issueTokens(user);
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
        return mapper.toResponse(users.save(user));
    }

    /**
     * Admin-initiated password reset. There is no email delivery provisioned in v1 (see
     * EmailNotificationChannel), so the flow is: admin sets a new temporary password here and
     * shares it with the user out of band — the same pattern already used for account creation.
     */
    @Transactional
    public UserResponse resetPassword(String userUuid, String newPassword) {
        User user = users.findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", userUuid));
        user.setPasswordHash(encoder.encode(newPassword));
        return mapper.toResponse(users.save(user));
    }

    private TokenResponse issueTokens(User user) {
        List<String> roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String access = jwt.generateAccessToken(user.getUsername(), roleNames);
        String refresh = jwt.generateRefreshToken(user.getUsername());
        return new TokenResponse(access, refresh, "Bearer", 3600);
    }
}
