package tz.go.tirdo.teltp.certification.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tz.go.tirdo.teltp.certification.service.CertificationService;
import tz.go.tirdo.teltp.common.event.CourseCompletionCandidateEvent;

/**
 * Bridges the assessment/progress modules to certification: after a completion-candidate signal
 * commits, it asks CertificationService to (idempotently) issue the certificate. Runs AFTER_COMMIT
 * so a certificate is never issued against an attempt/lesson change that later rolls back, and
 * swallows failures so certificate rendering can never break grading or progress tracking.
 */
@Component
public class CertificateAutoIssueListener {

    private static final Logger log = LoggerFactory.getLogger(CertificateAutoIssueListener.class);

    private final CertificationService certification;

    public CertificateAutoIssueListener(CertificationService certification) {
        this.certification = certification;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompletionCandidate(CourseCompletionCandidateEvent event) {
        try {
            certification.issueAutomatic(event.studentUuid(), event.courseUuid())
                    .ifPresent(cert -> log.info(
                            "Auto-issued certificate {} to student {} for course {} (trigger {})",
                            cert.referenceNumber(), event.studentUuid(), event.courseUuid(), event.sourceUuid()));
        } catch (Exception ex) {
            log.warn("Auto-issue skipped for student {} course {}: {}",
                    event.studentUuid(), event.courseUuid(), ex.getMessage());
        }
    }
}
