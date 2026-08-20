package tz.go.tirdo.teltp.integration.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** SMS gateway seam. Disabled in v1; integrate the bulk-SMS provider (sender ID TIRDO) when enabled. */
@Component
public class SmsNotificationChannel implements NotificationChannel {

    @Value("${teltp.integration.notification.sms.enabled:false}")
    private boolean enabled;

    @Override public String name() { return "SMS"; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public void send(String recipient, String subject, String body) {
        if (!enabled) return;  // seam: integrate SMS gateway HTTP API here
        throw new UnsupportedOperationException("SMS delivery not provisioned in v1");
    }
}
