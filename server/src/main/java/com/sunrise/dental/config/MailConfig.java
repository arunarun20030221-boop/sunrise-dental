package com.sunrise.dental.config;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Outbound email for appointment confirmations.
 *
 * <p>The sender is always created, even when no SMTP host is configured, so that
 * {@link com.sunrise.dental.service.NotificationService} can be injected unconditionally.
 * That service checks whether a host was supplied and logs the message instead of sending it
 * when one was not - which is how the application runs on a machine with no mail credentials.
 * Making the bean itself conditional would mean the whole application refused to start without
 * SMTP configured, which is the wrong trade for a feature that is a courtesy to patients.</p>
 */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(@Value("${mail.host:}") String host,
                                         @Value("${mail.port:587}") int port,
                                         @Value("${mail.username:}") String username,
                                         @Value("${mail.password:}") String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.transport.protocol", "smtp");
        mailProperties.put("mail.smtp.auth", String.valueOf(!username.isBlank()));
        mailProperties.put("mail.smtp.starttls.enable", "true");
        // Fail fast rather than hanging the booking flow on an unreachable mail server.
        mailProperties.put("mail.smtp.connectiontimeout", "5000");
        mailProperties.put("mail.smtp.timeout", "5000");
        mailProperties.put("mail.smtp.writetimeout", "5000");

        return sender;
    }
}
