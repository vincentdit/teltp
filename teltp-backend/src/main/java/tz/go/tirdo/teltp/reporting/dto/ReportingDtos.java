package tz.go.tirdo.teltp.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

/** Read-model payloads feeding the four dashboards (Revenue, Student Performance, Completion, Trainer). */
public final class ReportingDtos {
    private ReportingDtos() {}

    public record PlatformKpis(
            long activeLearners,
            long publishedCourses,
            long certificatesIssued,
            long corporateClients,
            BigDecimal confirmedRevenue,
            String currency) {}

    public record RevenueByChannel(String channel, BigDecimal amount, long transactions) {}
    public record RevenueDashboard(BigDecimal totalConfirmed, String currency, List<RevenueByChannel> byChannel) {}

    public record CompletionRow(String courseUuid, String courseTitle, long enrolled, long completed, int completionRate) {}
    public record CompletionDashboard(List<CompletionRow> rows) {}

    public record TrainerRow(String instructorUuid, String instructorName, long coursesAuthored, long learnersTaught) {}
    public record TrainerDashboard(List<TrainerRow> rows) {}
}
