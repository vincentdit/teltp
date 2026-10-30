package tz.go.tirdo.teltp.integration.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tz.go.tirdo.teltp.auth.service.UserService;

import java.util.Map;

/**
 * Posts to a configured SMS HTTP gateway. Disabled until both the flag and a gateway URL are set.
 * The body is JSON: to, sender, text.
 */
@Component
public class SmsNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationChannel.class);

    private final UserService users;

    @Value("${teltp.integration.notification.sms.enabled:false}")
    private boolean enabled;

    @Value("${teltp.integration.notification.sms.gateway-url:}")
    private String gatewayUrl;

    @Value("${teltp.integration.notification.sms.sender-id:TIRDO}")
    private String senderId;

    public SmsNotificationChannel(UserService users) {
        this.users = users;
    }

    @Override public String name() { return "SMS"; }
    @Override public boolean isEnabled() { return enabled && gatewayUrl != null && !gatewayUrl.isBlank(); }

    @Override
    public void send(String recipientUuid, String subject, String body) {
        if (!isEnabled()) return;
        String phone;
        try {
            phone = users.getEntity(recipientUuid).getPhoneNumber();
        } catch (RuntimeException ex) {
            log.warn("SMS lookup failed for {}: {}", recipientUuid, ex.getMessage());
            return;
        }
        if (phone == null || phone.isBlank()) return;
        RestClient.create().post()
                .uri(gatewayUrl)
                .body(Map.of("to", phone, "sender", senderId, "text", subject + "\n" + body))
                .retrieve()
                .toBodilessEntity();
    }
}
