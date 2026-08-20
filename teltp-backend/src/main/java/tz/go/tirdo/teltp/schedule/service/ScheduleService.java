package tz.go.tirdo.teltp.schedule.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.schedule.dto.ScheduleDtos.*;
import tz.go.tirdo.teltp.schedule.entity.*;
import tz.go.tirdo.teltp.schedule.repository.AttendanceRepository;
import tz.go.tirdo.teltp.schedule.repository.EventRepository;
import tz.go.tirdo.teltp.schedule.repository.RegistrationRepository;

import java.time.Instant;

@Service
public class ScheduleService {

    private final EventRepository events;
    private final RegistrationRepository registrations;
    private final AttendanceRepository attendances;
    private final ReferenceNumberGenerator refGen;

    public ScheduleService(EventRepository events, RegistrationRepository registrations,
                           AttendanceRepository attendances, ReferenceNumberGenerator refGen) {
        this.events = events;
        this.registrations = registrations;
        this.attendances = attendances;
        this.refGen = refGen;
    }

    @Transactional
    public EventResponse create(CreateEventRequest req) {
        if (req.endsAt().isBefore(req.startsAt()))
            throw new BusinessRuleException("Event end must be after start");
        ScheduledEvent e = new ScheduledEvent();
        e.setReferenceNumber(refGen.next("EVT"));
        e.setType(EventType.valueOf(req.type()));
        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setCourseUuid(req.courseUuid());
        e.setHostUuid(req.hostUuid());
        e.setStartsAt(req.startsAt());
        e.setEndsAt(req.endsAt());
        e.setCapacity(req.capacity());
        e.setPricingPlanUuid(req.pricingPlanUuid());
        e.setProvider(req.provider() == null ? MeetingProvider.MANUAL : MeetingProvider.valueOf(req.provider()));
        e.setJoinUrl(req.joinUrl());
        e.setExternalMeetingId(req.externalMeetingId());
        return toResponse(events.save(e));
    }

    @Transactional
    public RegistrationResponse register(String participantUuid, RegisterRequest req) {
        ScheduledEvent e = requireEvent(req.eventUuid());
        registrations.findByEventUuidAndParticipantUuid(req.eventUuid(), participantUuid)
                .ifPresent(x -> { throw new BusinessRuleException("Already registered"); });
        if (e.getCapacity() != null
                && registrations.countByEventUuidAndConfirmedTrue(req.eventUuid()) >= e.getCapacity())
            throw new BusinessRuleException("Event is at capacity");

        EventRegistration reg = new EventRegistration();
        reg.setEventUuid(req.eventUuid());
        reg.setParticipantUuid(participantUuid);
        // Paid webinars require payment confirmation before the seat is confirmed.
        reg.setConfirmed(e.getPricingPlanUuid() == null);
        EventRegistration saved = registrations.save(reg);
        return new RegistrationResponse(saved.getUuid(), saved.getEventUuid(),
                saved.getParticipantUuid(), saved.isConfirmed());
    }

    @Transactional
    public AttendanceResponse markAttendance(MarkAttendanceRequest req) {
        requireEvent(req.eventUuid());
        Attendance att = attendances.findByEventUuidAndParticipantUuid(req.eventUuid(), req.participantUuid())
                .orElseGet(Attendance::new);
        att.setEventUuid(req.eventUuid());
        att.setParticipantUuid(req.participantUuid());
        att.setPresent(req.present());
        if (req.present() && att.getCheckedInAt() == null) att.setCheckedInAt(Instant.now());
        attendances.save(att);
        long total = attendances.countByEventUuidAndPresentTrue(req.eventUuid());
        return new AttendanceResponse(req.eventUuid(), req.participantUuid(), req.present(), total);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventResponse> listByType(String type, Pageable pageable) {
        return PageResponse.from(events.findByType(EventType.valueOf(type), pageable), this::toResponse);
    }

    public ScheduledEvent getEntity(String uuid) { return requireEvent(uuid); }

    private ScheduledEvent requireEvent(String uuid) {
        return events.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("ScheduledEvent", uuid));
    }

    private EventResponse toResponse(ScheduledEvent e) {
        long confirmed = registrations.countByEventUuidAndConfirmedTrue(e.getUuid());
        return new EventResponse(e.getUuid(), e.getReferenceNumber(), e.getType().name(), e.getTitle(),
                e.getDescription(), e.getCourseUuid(), e.getHostUuid(), e.getStartsAt(), e.getEndsAt(),
                e.getCapacity(), e.getProvider().name(), e.getJoinUrl(), confirmed);
    }
}
