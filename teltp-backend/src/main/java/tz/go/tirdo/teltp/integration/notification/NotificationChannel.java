package tz.go.tirdo.teltp.integration.notification;

/** Seam for an outbound notification transport. */
public interface NotificationChannel {
    String name();              // EMAIL | SMS
    boolean isEnabled();
    void send(String recipient, String subject, String body);
}
