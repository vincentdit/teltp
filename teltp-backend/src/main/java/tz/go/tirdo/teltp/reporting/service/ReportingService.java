package tz.go.tirdo.teltp.reporting.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.auth.entity.RoleName;
import tz.go.tirdo.teltp.auth.repository.UserRepository;
import tz.go.tirdo.teltp.billing.entity.InvoiceStatus;
import tz.go.tirdo.teltp.billing.entity.Payment;
import tz.go.tirdo.teltp.billing.entity.PaymentStatus;
import tz.go.tirdo.teltp.billing.repository.InvoiceRepository;
import tz.go.tirdo.teltp.billing.repository.PaymentRepository;
import tz.go.tirdo.teltp.catalog.entity.Course;
import tz.go.tirdo.teltp.catalog.entity.CourseStatus;
import tz.go.tirdo.teltp.catalog.repository.CourseRepository;
import tz.go.tirdo.teltp.certification.repository.CertificateRepository;
import tz.go.tirdo.teltp.enrollment.repository.EnrollmentRepository;
import tz.go.tirdo.teltp.organization.entity.OrganizationType;
import tz.go.tirdo.teltp.organization.repository.OrganizationRepository;
import tz.go.tirdo.teltp.reporting.dto.ReportingDtos.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-model query services. These aggregate across module repositories to feed dashboards;
 * the dashboards themselves are a deferred Angular deliverable. Kept read-only and side-effect free.
 */
@Service
public class ReportingService {

    private final UserRepository users;
    private final CourseRepository courses;
    private final CertificateRepository certificates;
    private final OrganizationRepository organizations;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final EnrollmentRepository enrollments;

    public ReportingService(UserRepository users, CourseRepository courses, CertificateRepository certificates,
                            OrganizationRepository organizations, InvoiceRepository invoices,
                            PaymentRepository payments, EnrollmentRepository enrollments) {
        this.users = users;
        this.courses = courses;
        this.certificates = certificates;
        this.organizations = organizations;
        this.invoices = invoices;
        this.payments = payments;
        this.enrollments = enrollments;
    }

    @Transactional(readOnly = true)
    public PlatformKpis kpis() {
        long activeLearners = users.findAll().stream()
                .filter(u -> u.isActive() && u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.STUDENT))
                .count();
        long publishedCourses = courses.findAll().stream()
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED).count();
        long certs = certificates.count();
        long corporate = organizations.findAll().stream()
                .filter(o -> o.getType() != OrganizationType.ACADEMIC).count();
        BigDecimal revenue = confirmedRevenue();
        return new PlatformKpis(activeLearners, publishedCourses, certs, corporate, revenue, "TZS");
    }

    @Transactional(readOnly = true)
    public RevenueDashboard revenue() {
        List<Payment> confirmed = payments.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.CONFIRMED).toList();
        Map<String, List<Payment>> byChannel = confirmed.stream()
                .collect(Collectors.groupingBy(p -> p.getChannel().name()));
        List<RevenueByChannel> rows = byChannel.entrySet().stream()
                .map(e -> new RevenueByChannel(
                        e.getKey(),
                        e.getValue().stream().map(p -> p.getAmount().amount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size()))
                .toList();
        return new RevenueDashboard(confirmedRevenue(), "TZS", rows);
    }

    @Transactional(readOnly = true)
    public CompletionDashboard completion() {
        List<CompletionRow> rows = courses.findAll().stream()
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .map(c -> {
                    long enrolled = enrollments.countByCourseUuid(c.getUuid());
                    long completed = certificates.countByCourseUuid(c.getUuid());
                    int rate = enrolled == 0 ? 0 : (int) Math.round(100.0 * completed / enrolled);
                    return new CompletionRow(c.getUuid(), c.getTitle(), enrolled, completed, rate);
                })
                .toList();
        return new CompletionDashboard(rows);
    }

    @Transactional(readOnly = true)
    public TrainerDashboard trainer() {
        Map<String, List<Course>> byInstructor = courses.findAll().stream()
                .filter(c -> c.getInstructorUuid() != null)
                .collect(Collectors.groupingBy(Course::getInstructorUuid));
        List<TrainerRow> rows = byInstructor.entrySet().stream()
                .map(e -> {
                    long learners = e.getValue().stream()
                            .mapToLong(c -> enrollments.countByCourseUuid(c.getUuid())).sum();
                    String name = users.findByUuid(e.getKey())
                            .map(u -> {
                                String fn = u.fullName();
                                return (fn == null || fn.isBlank()) ? u.getUsername() : fn;
                            })
                            .orElse(e.getKey());
                    return new TrainerRow(e.getKey(), name, e.getValue().size(), learners);
                })
                .toList();
        return new TrainerDashboard(rows);
    }

    private BigDecimal confirmedRevenue() {
        return invoices.findByStatus(InvoiceStatus.PAID).stream()
                .map(i -> i.getTotal().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
