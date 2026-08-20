package tz.go.tirdo.teltp.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class ScheduleDtos {
    private ScheduleDtos() {}

    public record CreateEventRequest(
            @NotNull String type, @NotBlank String title, String description,
            String courseUuid, String hostUuid,
            @NotNull Instant startsAt, @NotNull Instant endsAt,
            Integer capacity, String pricingPlanUuid,
            String provider, String joinUrl, String externalMeetingId) {}

    public record EventResponse(
            String uuid, String referenceNumber, String type, String title, String description,
            String courseUuid, String hostUuid, Instant startsAt, Instant endsAt,
            Integer capacity, String provider, String joinUrl, long confirmedRegistrations) {}

    public record RegisterRequest(@NotBlank String eventUuid) {}
    public record RegistrationResponse(String uuid, String eventUuid, String participantUuid, boolean confirmed) {}

    public record MarkAttendanceRequest(@NotBlank String eventUuid, @NotBlank String participantUuid, boolean present) {}
    public record AttendanceResponse(String eventUuid, String participantUuid, boolean present, long totalPresent) {}
}
