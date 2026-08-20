package tz.go.tirdo.teltp.schedule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "attendances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_uuid", "participant_uuid"}))
public class Attendance extends BaseEntity {

    @Column(name = "event_uuid", nullable = false, length = 36)
    private String eventUuid;

    @Column(name = "participant_uuid", nullable = false, length = 36)
    private String participantUuid;

    @Column(nullable = false)
    private boolean present = false;

    private Instant checkedInAt;
}
