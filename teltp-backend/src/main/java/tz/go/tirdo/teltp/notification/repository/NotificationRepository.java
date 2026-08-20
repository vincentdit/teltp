package tz.go.tirdo.teltp.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.notification.entity.Notification;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByUuid(String uuid);
    Page<Notification> findByRecipientUuidOrderByCreatedAtDesc(String recipientUuid, Pageable pageable);
    long countByRecipientUuidAndReadFalse(String recipientUuid);
}
