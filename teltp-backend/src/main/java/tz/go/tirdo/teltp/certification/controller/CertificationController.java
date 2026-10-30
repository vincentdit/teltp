package tz.go.tirdo.teltp.certification.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.certification.dto.CertificationDtos.*;
import tz.go.tirdo.teltp.certification.service.CertificationService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.security.AccessGuard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/certification")
public class CertificationController {

    private final CertificationService service;
    private final AccessGuard access;

    public CertificationController(CertificationService service, AccessGuard access) {
        this.service = service;
        this.access = access;
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<CertificateResponse> issue(@Valid @RequestBody IssueRequest req) {
        return ApiResponse.ok("Certificate issued", service.issue(req));
    }

    @PostMapping("/{uuid}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ApiResponse<CertificateResponse> renew(@PathVariable String uuid, @RequestParam LocalDate expiresOn) {
        return ApiResponse.ok("Certificate renewed", service.renew(uuid, expiresOn));
    }

    @PostMapping("/{uuid}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> revoke(@PathVariable String uuid) {
        service.revoke(uuid);
        return ApiResponse.ok("Certificate revoked", null);
    }

    @GetMapping("/{uuid}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(@PathVariable String uuid) {
        access.assertSelfOrRoles(service.ownerUuid(uuid), "ADMIN", "INSTRUCTOR");
        byte[] pdf = service.downloadPdf(uuid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/students/{studentUuid}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CertificateResponse>> forStudent(@PathVariable String studentUuid) {
        access.assertSelfOrRoles(studentUuid, "ADMIN", "INSTRUCTOR");
        return ApiResponse.ok(service.forStudent(studentUuid));
    }

    /** Public Open Badges 2.0 assertion. Not wrapped in ApiResponse so badge verifiers can read it. */
    @GetMapping("/badges/{code}")
    public Map<String, Object> badge(@PathVariable String code) {
        return service.openBadge(code);
    }

    /** Public endpoint (whitelisted in SecurityConfig). */
    @GetMapping("/verify/{code}")
    public ApiResponse<VerificationResult> verify(@PathVariable String code) {
        return ApiResponse.ok(service.verify(code));
    }
}
