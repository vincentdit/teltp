package tz.go.tirdo.teltp.integration.meeting;

import org.springframework.stereotype.Component;

import java.time.Instant;

/** Default provisioner: links are entered by the organizer. No external call. */
@Component
public class ManualMeetingProvisioner implements MeetingProvisioner {

    @Override
    public String provider() { return "MANUAL"; }

    @Override
    public MeetingDetails provision(String title, Instant startsAt, Instant endsAt, String hostEmail) {
        // MANUAL provider returns nothing to provision; the organizer supplies joinUrl on the event.
        return new MeetingDetails(null, null, null);
    }
}
