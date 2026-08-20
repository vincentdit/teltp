package tz.go.tirdo.teltp.notification.dto;

import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record NotificationResponse(String uuid, String type, String title, String body,
                                       boolean read, Instant createdAt) {}
}
