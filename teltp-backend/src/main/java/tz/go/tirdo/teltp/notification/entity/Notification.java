package tz.go.tirdo.teltp.notification.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

/** In-app notification record; external delivery (email/SMS) is attempted via channel seams. */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(nullable = false, length = 36)
    private String recipientUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
