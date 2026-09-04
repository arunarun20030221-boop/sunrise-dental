package com.sunrise.dental.service;

import com.sunrise.dental.config.ClinicProperties;
import com.sunrise.dental.domain.Appointment;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends appointment confirmation emails.
 *
 * <p>Degrades deliberately: when no SMTP host is configured the message is logged instead of
 * sent, so the application runs on a machine with no mail credentials and the booking flow is
 * never blocked by an email failure. A confirmation that fails to send must not lose the
 * appointment - the patient is already booked.</p>
 *
 * <p>The message body is produced by {@link MailTemplateProvider}, a classic singleton, so that
 * wording changes happen in one place.</p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final ClinicProperties clinic;
    private final String mailHost;

    public NotificationService(JavaMailSender mailSender,
                               ClinicProperties clinic,
                               @Value("${mail.host:}") String mailHost) {
        this.mailSender = mailSender;
        this.clinic = clinic;
        this.mailHost = mailHost;
    }

    public void appointmentConfirmed(Appointment appointment) {
        String recipient = appointment.getPatient().getEmail();
        if (recipient == null || recipient.isBlank()) {
            return; // no email on file; nothing to do
        }

        String subject = "%s - appointment %s confirmed"
                .formatted(clinic.getName(), appointment.getAppointmentNumber());
        String body = MailTemplateProvider.getInstance().confirmationBody(
                appointment.getPatient().getName(),
                appointment.getAppointmentNumber(),
                appointment.getAppointmentDate().format(DATE),
                appointment.getAppointmentTime().format(TIME),
                appointment.getDentist().getName(),
                appointment.getTreatmentType().getName(),
                clinic);

        if (mailHost == null || mailHost.isBlank()) {
            log.info("SMTP not configured; confirmation for {} would read:\n{}",
                    appointment.getAppointmentNumber(), body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Confirmation email sent for {}", appointment.getAppointmentNumber());
        } catch (Exception e) {
            // Never propagate: the appointment is booked either way.
            log.warn("Could not send confirmation for {}: {}",
                    appointment.getAppointmentNumber(), e.getMessage());
        }
    }
}
