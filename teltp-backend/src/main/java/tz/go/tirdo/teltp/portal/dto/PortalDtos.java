package tz.go.tirdo.teltp.portal.dto;

public final class PortalDtos {
    private PortalDtos() {}

    public record SlideResponse(
            String uuid,
            String title,
            String body,
            String linkUrl,
            int sortOrder,
            boolean active,
            boolean hasImage,
            long imageVersion) {}
}
