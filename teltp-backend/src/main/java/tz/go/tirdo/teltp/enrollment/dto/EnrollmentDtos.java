package tz.go.tirdo.teltp.enrollment.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Set;

public final class EnrollmentDtos {
    private EnrollmentDtos() {}

    public record EnrollRequest(@NotBlank String courseUuid, String cohortUuid) {}

    public record AdminAssignRequest(
            @NotBlank String courseUuid,
            String cohortUuid,
            @NotBlank String organizationUuid,
            Set<String> studentUuids) {}

    public record CohortRequest(@NotBlank String courseUuid, @NotBlank String name,
                                LocalDate startDate, LocalDate endDate, Integer capacity) {}

    public record EnrollmentResponse(
            String uuid, String courseUuid, String studentUuid, String cohortUuid,
            String status, String assignedByOrganizationUuid) {}

    public record CohortResponse(String uuid, String courseUuid, String name,
                                 LocalDate startDate, LocalDate endDate, Integer capacity, long activeCount) {}
}
