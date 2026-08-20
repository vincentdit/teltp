package tz.go.tirdo.teltp.certification.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public final class CertificationDtos {
    private CertificationDtos() {}

    public record IssueRequest(
            @NotBlank String studentUuid,
            @NotBlank String courseUuid,
            String accreditingBody,
            String accreditationLevel,
            LocalDate expiresOn) {}

    public record CertificateResponse(
            String uuid, String referenceNumber, String verificationCode,
            String recipientName, String courseTitle,
            LocalDate issuedOn, LocalDate expiresOn, boolean revoked,
            String accreditingBody, String accreditationLevel) {}

    /** Public, unauthenticated verification payload. */
    public record VerificationResult(
            boolean valid, String status, String recipientName, String courseTitle,
            LocalDate issuedOn, LocalDate expiresOn, String accreditingBody) {}
}
