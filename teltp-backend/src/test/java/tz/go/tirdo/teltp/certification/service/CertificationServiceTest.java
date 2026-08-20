package tz.go.tirdo.teltp.certification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tz.go.tirdo.teltp.auth.entity.User;
import tz.go.tirdo.teltp.auth.service.UserService;
import tz.go.tirdo.teltp.catalog.entity.Course;
import tz.go.tirdo.teltp.catalog.service.CourseService;
import tz.go.tirdo.teltp.certification.dto.CertificationDtos.*;
import tz.go.tirdo.teltp.certification.entity.Certificate;
import tz.go.tirdo.teltp.certification.repository.CertificateRepository;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.content.service.ContentStorage;
import tz.go.tirdo.teltp.progress.service.ProgressService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the certificate issuance guards, focused on the new idempotent
 * auto-issue path invoked by the completion-candidate event listener.
 */
class CertificationServiceTest {

    private CertificateRepository certificates;
    private CertificatePdfGenerator pdfGenerator;
    private ContentStorage storage;
    private ProgressService progress;
    private CourseService courses;
    private UserService users;
    private ReferenceNumberGenerator refGen;
    private CertificationService service;

    private static final String S = "S1";
    private static final String C = "C1";

    @BeforeEach
    void setUp() {
        certificates = mock(CertificateRepository.class);
        pdfGenerator = mock(CertificatePdfGenerator.class);
        storage = mock(ContentStorage.class);
        progress = mock(ProgressService.class);
        courses = mock(CourseService.class);
        users = mock(UserService.class);
        refGen = mock(ReferenceNumberGenerator.class);
        service = new CertificationService(certificates, pdfGenerator, storage, progress, courses, users, refGen);
        ReflectionTestUtils.setField(service, "autoIssueEnabled", true);
        ReflectionTestUtils.setField(service, "defaultAccreditingBody", "");
    }

    private void happyPathStubs() {
        User u = new User();
        u.setUsername("jdoe");
        u.setFirstName("Jane");
        u.setLastName("Doe");
        when(users.getEntity(S)).thenReturn(u);

        Course course = new Course();
        course.setUuid(C);
        course.setTitle("Industrial Cybersecurity");
        when(courses.getEntity(C)).thenReturn(course);

        when(refGen.next("CERT")).thenReturn("TELTP-CERT-2026-00001");
        when(storage.store(anyString(), any(), eq("application/pdf"))).thenReturn("cert/key.pdf");
        when(pdfGenerator.render(any(Certificate.class))).thenReturn(new byte[]{1, 2, 3});
        when(certificates.save(any(Certificate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void autoIssue_disabled_returnsEmpty_andPersistsNothing() {
        ReflectionTestUtils.setField(service, "autoIssueEnabled", false);

        Optional<CertificateResponse> result = service.issueAutomatic(S, C);

        assertThat(result).isEmpty();
        verify(certificates, never()).save(any());
        verify(progress, never()).isCourseCompleted(anyString(), anyString());
    }

    @Test
    void autoIssue_whenCertificateAlreadyExists_returnsEmpty() {
        when(certificates.findByStudentUuidAndCourseUuid(S, C))
                .thenReturn(Optional.of(new Certificate()));

        Optional<CertificateResponse> result = service.issueAutomatic(S, C);

        assertThat(result).isEmpty();
        verify(certificates, never()).save(any());
    }

    @Test
    void autoIssue_whenCourseNotComplete_returnsEmpty() {
        when(certificates.findByStudentUuidAndCourseUuid(S, C)).thenReturn(Optional.empty());
        when(progress.isCourseCompleted(S, C)).thenReturn(false);

        Optional<CertificateResponse> result = service.issueAutomatic(S, C);

        assertThat(result).isEmpty();
        verify(certificates, never()).save(any());
    }

    @Test
    void autoIssue_whenComplete_issuesCertificateWithStoredPdf() {
        when(certificates.findByStudentUuidAndCourseUuid(S, C)).thenReturn(Optional.empty());
        when(progress.isCourseCompleted(S, C)).thenReturn(true);
        happyPathStubs();

        Optional<CertificateResponse> result = service.issueAutomatic(S, C);

        assertThat(result).isPresent();
        assertThat(result.get().recipientName()).isEqualTo("Jane Doe");
        assertThat(result.get().courseTitle()).isEqualTo("Industrial Cybersecurity");
        assertThat(result.get().referenceNumber()).isEqualTo("TELTP-CERT-2026-00001");
        verify(storage).store(anyString(), any(), eq("application/pdf"));
        verify(certificates, atLeastOnce()).save(any(Certificate.class));
    }

    @Test
    void manualIssue_whenCourseNotComplete_throws() {
        when(progress.isCourseCompleted(S, C)).thenReturn(false);

        assertThatThrownBy(() -> service.issue(new IssueRequest(S, C, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not completed");
    }

    @Test
    void manualIssue_whenAlreadyIssued_throws() {
        when(progress.isCourseCompleted(S, C)).thenReturn(true);
        when(certificates.findByStudentUuidAndCourseUuid(S, C))
                .thenReturn(Optional.of(new Certificate()));

        assertThatThrownBy(() -> service.issue(new IssueRequest(S, C, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already issued");
    }

    @Test
    void manualIssue_whenEligible_succeeds() {
        when(progress.isCourseCompleted(S, C)).thenReturn(true);
        when(certificates.findByStudentUuidAndCourseUuid(S, C)).thenReturn(Optional.empty());
        happyPathStubs();

        CertificateResponse r = service.issue(new IssueRequest(S, C, "TIRDO", "NTA-6", null));

        assertThat(r.referenceNumber()).isEqualTo("TELTP-CERT-2026-00001");
        assertThat(r.recipientName()).isEqualTo("Jane Doe");
    }
}
