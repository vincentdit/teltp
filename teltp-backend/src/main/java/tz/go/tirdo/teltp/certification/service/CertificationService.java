package tz.go.tirdo.teltp.certification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.auth.entity.User;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.catalog.entity.Course;
import tz.go.tirdo.teltp.catalog.service.CourseService;
import tz.go.tirdo.teltp.certification.dto.CertificationDtos.*;
import tz.go.tirdo.teltp.certification.entity.Certificate;
import tz.go.tirdo.teltp.certification.repository.CertificateRepository;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.content.service.ContentStorage;
import tz.go.tirdo.teltp.progress.service.ProgressService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CertificationService {

    private final CertificateRepository certificates;
    private final CertificatePdfGenerator pdfGenerator;
    private final ContentStorage storage;
    private final ProgressService progress;
    private final CourseService courses;
    private final UserService users;
    private final ReferenceNumberGenerator refGen;

    /** When true, passing the certifying exam (which completes the course) auto-issues the certificate. */
    @Value("${teltp.certificate.auto-issue:true}")
    private boolean autoIssueEnabled;

    /** Accrediting body stamped on auto-issued certificates; blank leaves it unset. */
    @Value("${teltp.certificate.default-accrediting-body:}")
    private String defaultAccreditingBody;

    public CertificationService(CertificateRepository certificates, CertificatePdfGenerator pdfGenerator,
                                ContentStorage storage, ProgressService progress, CourseService courses,
                                UserService users, ReferenceNumberGenerator refGen) {
        this.certificates = certificates;
        this.pdfGenerator = pdfGenerator;
        this.storage = storage;
        this.progress = progress;
        this.courses = courses;
        this.users = users;
        this.refGen = refGen;
    }

    /** Issue a certificate manually; gated on verified course completion and single-issue per course. */
    @Transactional
    public CertificateResponse issue(IssueRequest req) {
        if (!progress.isCourseCompleted(req.studentUuid(), req.courseUuid()))
            throw new BusinessRuleException("Student has not completed the course");
        certificates.findByStudentUuidAndCourseUuid(req.studentUuid(), req.courseUuid())
                .ifPresent(c -> { throw new BusinessRuleException("Certificate already issued"); });
        Certificate saved = buildAndStore(req.studentUuid(), req.courseUuid(),
                req.accreditingBody(), req.accreditationLevel(), req.expiresOn());
        return toResponse(saved);
    }

    /**
     * Idempotently issue a certificate in response to a completion signal. Returns empty (rather than
     * throwing) when auto-issue is disabled, a certificate already exists, or the course is not yet
     * fully complete — so it is safe to call from an event listener on every candidate signal.
     */
    @Transactional
    public Optional<CertificateResponse> issueAutomatic(String studentUuid, String courseUuid) {
        if (!autoIssueEnabled) return Optional.empty();
        if (certificates.findByStudentUuidAndCourseUuid(studentUuid, courseUuid).isPresent())
            return Optional.empty();
        if (!progress.isCourseCompleted(studentUuid, courseUuid)) return Optional.empty();
        String body = (defaultAccreditingBody == null || defaultAccreditingBody.isBlank())
                ? null : defaultAccreditingBody;
        Certificate saved = buildAndStore(studentUuid, courseUuid, body, null, null);
        return Optional.of(toResponse(saved));
    }

    /** Renewal: re-issues with a new expiry (cert-renewal revenue stream). */
    @Transactional
    public CertificateResponse renew(String uuid, LocalDate newExpiry) {
        Certificate cert = require(uuid);
        cert.setExpiresOn(newExpiry);
        cert.setIssuedOn(LocalDate.now());
        byte[] pdf = pdfGenerator.render(cert);
        cert.setPdfStorageKey(storage.store(cert.getReferenceNumber() + ".pdf", pdf, "application/pdf"));
        return toResponse(certificates.save(cert));
    }

    @Transactional
    public void revoke(String uuid) {
        Certificate cert = require(uuid);
        cert.setRevoked(true);
        certificates.save(cert);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(String uuid) {
        return storage.retrieve(require(uuid).getPdfStorageKey());
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> forStudent(String studentUuid) {
        return certificates.findByStudentUuid(studentUuid).stream().map(this::toResponse).toList();
    }

    /** Public verification by short code. */
    @Transactional(readOnly = true)
    public VerificationResult verify(String code) {
        return certificates.findByVerificationCode(code)
                .map(c -> {
                    String status;
                    boolean valid;
                    if (c.isRevoked()) { status = "REVOKED"; valid = false; }
                    else if (c.getExpiresOn() != null && c.getExpiresOn().isBefore(LocalDate.now())) {
                        status = "EXPIRED"; valid = false;
                    } else { status = "VALID"; valid = true; }
                    return new VerificationResult(valid, status, c.getRecipientName(), c.getCourseTitle(),
                            c.getIssuedOn(), c.getExpiresOn(), c.getAccreditingBody());
                })
                .orElse(new VerificationResult(false, "NOT_FOUND", null, null, null, null, null));
    }

    // ---- internals ----

    private Certificate buildAndStore(String studentUuid, String courseUuid,
                                      String accreditingBody, String accreditationLevel, LocalDate expiresOn) {
        User student = users.getEntity(studentUuid);
        Course course = courses.getEntity(courseUuid);

        Certificate cert = new Certificate();
        cert.setReferenceNumber(refGen.next("CERT"));
        cert.setVerificationCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        cert.setStudentUuid(studentUuid);
        cert.setCourseUuid(courseUuid);
        cert.setRecipientName(student.fullName().isBlank() ? student.getUsername() : student.fullName());
        cert.setCourseTitle(course.getTitle());
        cert.setIssuedOn(LocalDate.now());
        cert.setExpiresOn(expiresOn);
        cert.setAccreditingBody(accreditingBody);
        cert.setAccreditationLevel(accreditationLevel);

        Certificate saved = certificates.save(cert);
        byte[] pdf = pdfGenerator.render(saved);
        String key = storage.store(saved.getReferenceNumber() + ".pdf", pdf, "application/pdf");
        saved.setPdfStorageKey(key);
        return certificates.save(saved);
    }

    private Certificate require(String uuid) {
        return certificates.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Certificate", uuid));
    }

    private CertificateResponse toResponse(Certificate c) {
        return new CertificateResponse(c.getUuid(), c.getReferenceNumber(), c.getVerificationCode(),
                c.getRecipientName(), c.getCourseTitle(), c.getIssuedOn(), c.getExpiresOn(), c.isRevoked(),
                c.getAccreditingBody(), c.getAccreditationLevel());
    }
}
