package tz.go.tirdo.teltp.certification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "certificates")
public class Certificate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;  // TELTP-CERT-YYYY-00001

    /** Short public code embedded in the QR / verification URL. */
    @Column(nullable = false, unique = true, length = 24)
    private String verificationCode;

    @Column(nullable = false, length = 36)
    private String studentUuid;

    @Column(nullable = false, length = 36)
    private String courseUuid;

    @Column(nullable = false, length = 200)
    private String recipientName;

    @Column(nullable = false, length = 255)
    private String courseTitle;

    @Column(nullable = false)
    private LocalDate issuedOn;

    /** Null when the certificate does not expire. */
    private LocalDate expiresOn;

    private boolean revoked = false;

    /** Extensible accreditation metadata (awarding body, NTA level, framework alignment). */
    @Column(length = 200)
    private String accreditingBody;

    @Column(length = 50)
    private String accreditationLevel;

    /** Storage key of the rendered PDF. */
    @Column(length = 500)
    private String pdfStorageKey;
}
