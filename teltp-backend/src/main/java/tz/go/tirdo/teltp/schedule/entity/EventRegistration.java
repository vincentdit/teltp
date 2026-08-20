package tz.go.tirdo.teltp.schedule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_uuid", "participant_uuid"}))
public class EventRegistration extends BaseEntity {

    @Column(name = "event_uuid", nullable = false, length = 36)
    private String eventUuid;

    @Column(name = "participant_uuid", nullable = false, length = 36)
    private String participantUuid;

    @Column(nullable = false)
    private boolean confirmed = true;  // false while awaiting payment for paid webinars
}
