package tz.go.tirdo.teltp.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class MarketplaceDtos {
    private MarketplaceDtos() {}

    public record CreateItemRequest(
            @NotBlank String title, String description, @NotNull String type,
            @NotBlank String pricingPlanUuid, String storageKey) {}

    public record ItemResponse(String uuid, String title, String description, String type,
                               String pricingPlanUuid, boolean published) {}

    public record PurchaseRequest(@NotBlank String itemUuid) {}
}
