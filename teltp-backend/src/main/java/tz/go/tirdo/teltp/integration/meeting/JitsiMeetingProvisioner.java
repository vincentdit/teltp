package tz.go.tirdo.teltp.integration.meeting;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;

/** Public Jitsi room. No API key. The room name is derived from the title and start time. */
@Component
public class JitsiMeetingProvisioner implements MeetingProvisioner {

    @Override
    public String provider() { return "JITSI"; }

    @Override
    public MeetingDetails provision(String title, Instant startsAt, Instant endsAt, String hostEmail) {
        String slug = (title == null ? "session" : title).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) slug = "session";
        String room = "TeLTP-" + slug + "-" + startsAt.getEpochSecond();
        String url = "https://meet.jit.si/" + room;
        return new MeetingDetails(url, room, url);
    }
}
