package tz.go.tirdo.teltp.integration.meeting;

/**
 * Virtual-classroom provisioning seam. v1 stores join links/metadata only (MANUAL). A Zoom/Teams/Jitsi
 * implementation would create the meeting and return a join URL + external id, slotting in here without
 * changing the Schedule module. Recording and chat are referenced by link, not rebuilt.
 */
public interface MeetingProvisioner {
    String provider();  // MANUAL | ZOOM | TEAMS | JITSI

    record MeetingDetails(String joinUrl, String externalMeetingId, String hostUrl) {}

    MeetingDetails provision(String title, java.time.Instant startsAt, java.time.Instant endsAt, String hostEmail);
}
