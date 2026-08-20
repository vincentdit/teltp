package tz.go.tirdo.teltp.corporate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CorporateDtos {
    private CorporateDtos() {}

    public record CreateContractRequest(
            @NotBlank String organizationUuid, @NotBlank String title, String scope,
            Integer participantTarget, LocalDate startDate, LocalDate endDate) {}

    public record QuoteRequest(@NotNull BigDecimal contractValue) {}

    public record TransitionRequest(@NotNull String targetStatus) {}

    public record ContractResponse(
            String uuid, String referenceNumber, String organizationUuid, String title, String scope,
            String status, BigDecimal contractValue, Integer participantTarget,
            LocalDate startDate, LocalDate endDate, String invoiceUuid) {}
}
