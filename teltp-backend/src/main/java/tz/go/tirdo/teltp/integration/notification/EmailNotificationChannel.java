package tz.go.tirdo.teltp.integration.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Email channel seam. Disabled in v1 (logs instead of sending); wire JavaMailSender when enabled. */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    @Value("${teltp.integration.notification.email.enabled:false}")
    private boolean enabled;

    @Override public String name() { return "EMAIL"; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public void send(String recipient, String subject, String body) {
        if (!enabled) return;  // seam: integrate JavaMailSender / SMTP relay here
        throw new UnsupportedOperationException("Email delivery not provisioned in v1");
    }
}
