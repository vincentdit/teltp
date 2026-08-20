package tz.go.tirdo.teltp.notification.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.integration.notification.NotificationChannel;
import tz.go.tirdo.teltp.notification.dto.NotificationDtos.*;
import tz.go.tirdo.teltp.notification.entity.Notification;
import tz.go.tirdo.teltp.notification.entity.NotificationType;
import tz.go.tirdo.teltp.notification.repository.NotificationRepository;

import java.util.List;

/**
 * Creates in-app notifications and best-effort fans them out across enabled external channels.
 * Other modules call {@link #notify} on domain events (enrollment, payment, certificate-ready, deadlines).
 */
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final List<NotificationChannel> channels;

    public NotificationService(NotificationRepository notifications, List<NotificationChannel> channels) {
        this.notifications = notifications;
        this.channels = channels;
    }

    @Transactional
    public void notify(String recipientUuid, NotificationType type, String title, String body) {
        Notification n = new Notification();
        n.setRecipientUuid(recipientUuid);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        notifications.save(n);

        for (NotificationChannel channel : channels) {
            if (channel.isEnabled()) {
                try {
                    channel.send(recipientUuid, title, body);
                } catch (RuntimeException ignored) {
                    // delivery failures must not break the originating transaction
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> inbox(String recipientUuid, Pageable pageable) {
        return PageResponse.from(
                notifications.findByRecipientUuidOrderByCreatedAtDesc(recipientUuid, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String recipientUuid) {
        return notifications.countByRecipientUuidAndReadFalse(recipientUuid);
    }

    @Transactional
    public void markRead(String uuid) {
        Notification n = notifications.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", uuid));
        n.setRead(true);
        notifications.save(n);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getUuid(), n.getType().name(), n.getTitle(),
                n.getBody(), n.isRead(), n.getCreatedAt());
    }
}
