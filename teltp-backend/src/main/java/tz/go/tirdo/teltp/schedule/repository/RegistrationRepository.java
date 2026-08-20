package tz.go.tirdo.teltp.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.schedule.entity.EventRegistration;

import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<EventRegistration, Long> {
    Optional<EventRegistration> findByEventUuidAndParticipantUuid(String eventUuid, String participantUuid);
    long countByEventUuidAndConfirmedTrue(String eventUuid);
}
