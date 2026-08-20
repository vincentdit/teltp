package tz.go.tirdo.teltp.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ContentDtos {
    private ContentDtos() {}

    public record RegisterMaterialRequest(
            @NotBlank String lessonUuid,
            @NotBlank String title,
            @NotNull String type,
            @NotBlank String storageKey,
            Long sizeBytes,
            String mimeType) {}

    public record MaterialResponse(
            String uuid, String lessonUuid, String title, String type,
            String storageKey, Long sizeBytes, String mimeType) {}
}
