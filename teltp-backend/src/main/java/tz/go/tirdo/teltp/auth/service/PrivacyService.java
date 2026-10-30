package tz.go.tirdo.teltp.auth.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.audit.service.AuditService;
import tz.go.tirdo.teltp.auth.entity.User;
import tz.go.tirdo.teltp.auth.repository.UserRepository;
import tz.go.tirdo.teltp.billing.repository.InvoiceRepository;
import tz.go.tirdo.teltp.certification.repository.CertificateRepository;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.enrollment.repository.EnrollmentRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** PDPA-style export and erasure for the signed-in person. */
@Service
public class PrivacyService {

    private final UserRepository users;
    private final EnrollmentRepository enrollments;
    private final CertificateRepository certificates;
    private final InvoiceRepository invoices;
    private final RefreshTokenService refreshTokens;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public PrivacyService(UserRepository users, EnrollmentRepository enrollments,
                          CertificateRepository certificates, InvoiceRepository invoices,
                          RefreshTokenService refreshTokens, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.enrollments = enrollments;
        this.certificates = certificates;
        this.invoices = invoices;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(String username) {
        User user = require(username);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("fullName", user.fullName());
        body.put("phoneNumber", user.getPhoneNumber());
        body.put("profession", user.getProfession());
        body.put("nationalId", user.getNationalId());
        body.put("nidaVerified", user.isNidaVerified());
        body.put("dataProcessingConsent", user.isDataProcessingConsent());
        body.put("enrollments", enrollments.findByStudentUuid(user.getUuid(), PageRequest.of(0, 200)).getContent()
                .stream().map(e -> Map.of("courseUuid", e.getCourseUuid(), "status", e.getStatus().name())).toList());
        body.put("certificates", certificates.findByStudentUuid(user.getUuid()).stream()
                .map(c -> Map.of("reference", c.getReferenceNumber(), "course", c.getCourseTitle(),
                        "issuedOn", c.getIssuedOn().toString(), "revoked", c.isRevoked())).toList());
        body.put("invoices", invoices.findByPayerUuid(user.getUuid(), PageRequest.of(0, 200)).getContent()
                .stream().map(i -> Map.of("reference", i.getReferenceNumber(), "status", i.getStatus().name(),
                        "total", i.getTotal().amount().toPlainString())).toList());
        return body;
    }

    @Transactional
    public void erase(String username) {
        User user = require(username);
        refreshTokens.revokeAll(user.getUsername());
        String stamp = user.getUuid();
        user.setUsername("erased-" + stamp);
        user.setEmail("erased-" + stamp + "@invalid.local");
        user.setFirstName("Erased");
        user.setLastName("User");
        user.setPhoneNumber(null);
        user.setNationalId(null);
        user.setProfession(null);
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        user.setActive(false);
        user.setDeleted(true);
        user.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        users.save(user);
        audit.record("ACCOUNT_ERASED", "User", stamp, "self-service");
    }

    private User require(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", username));
    }
}
