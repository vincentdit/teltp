package tz.go.tirdo.teltp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class OrganizationDtos {
    private OrganizationDtos() {}

    public record CreateRequest(
            @NotBlank String name,
            @NotNull String type,
            String subType,
            String contactEmail,
            String contactPhone,
            String region,
            String district,
            String tin) {}

    public record OrganizationResponse(
            String uuid,
            String name,
            String type,
            String subType,
            String contactEmail,
            String contactPhone,
            String region,
            String district,
            String tin) {}
}
