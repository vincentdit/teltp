package tz.go.tirdo.teltp.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.schedule.entity.Attendance;

import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEventUuidAndParticipantUuid(String eventUuid, String participantUuid);
    long countByEventUuidAndPresentTrue(String eventUuid);
}
