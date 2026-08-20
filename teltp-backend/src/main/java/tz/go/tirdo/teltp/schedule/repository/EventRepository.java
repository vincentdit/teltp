package tz.go.tirdo.teltp.schedule.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.schedule.entity.EventType;
import tz.go.tirdo.teltp.schedule.entity.ScheduledEvent;

import java.util.Optional;

public interface EventRepository extends JpaRepository<ScheduledEvent, Long> {
    Optional<ScheduledEvent> findByUuid(String uuid);
    Page<ScheduledEvent> findByType(EventType type, Pageable pageable);
}
