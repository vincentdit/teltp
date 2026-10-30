package tz.go.tirdo.teltp.integration.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import tz.go.tirdo.teltp.auth.service.UserService;

/** Sends mail when teltp.integration.notification.email.enabled is true. Otherwise the message is logged. */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mail;
    private final UserService users;

    @Value("${teltp.integration.notification.email.enabled:false}")
    private boolean enabled;

    @Value("${teltp.integration.notification.email.from:noreply@tirdo.go.tz}")
    private String from;

    public EmailNotificationChannel(JavaMailSender mail, UserService users) {
        this.mail = mail;
        this.users = users;
    }

    @Override public String name() { return "EMAIL"; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public void send(String recipientUuid, String subject, String body) {
        try {
            sendToAddress(users.getEntity(recipientUuid).getEmail(), subject, body);
        } catch (RuntimeException ex) {
            log.warn("Email lookup failed for {}: {}", recipientUuid, ex.getMessage());
        }
    }

    public void sendToAddress(String address, String subject, String body) {
        if (address == null || address.isBlank()) return;
        if (!enabled) {
            log.info("Email disabled. Not sent to {} — {}", address, subject);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(address);
        message.setSubject(subject);
        message.setText(body);
        mail.send(message);
    }
}
