package com.sunrise.dental.service;

import com.sunrise.dental.config.ClinicProperties;

/**
 * SINGLETON pattern, in its classic eager form.
 *
 * <p>Included deliberately alongside Spring's container-managed singletons so the report can
 * compare the two. This class holds no state beyond the message wording, needs no injected
 * collaborators, and is called from a method rather than wired into a constructor - so the
 * classic idiom costs nothing here.</p>
 *
 * <p>The instance is created eagerly in a static final field, which the JVM guarantees is
 * initialised once and safely published to every thread. That avoids the double-checked-locking
 * bug that hand-written lazy singletons are prone to. The trade-off is the one the report should
 * name: because callers reach it through {@code getInstance()} rather than receiving it, a test
 * cannot substitute a different template - which is exactly why the collaborating
 * {@link NotificationService} is a Spring bean instead.</p>
 */
public final class MailTemplateProvider {

    private static final MailTemplateProvider INSTANCE = new MailTemplateProvider();

    private MailTemplateProvider() {
        // no external instantiation
    }

    public static MailTemplateProvider getInstance() {
        return INSTANCE;
    }

    public String confirmationBody(String patientName,
                                   String appointmentNumber,
                                   String date,
                                   String time,
                                   String dentistName,
                                   String treatment,
                                   ClinicProperties clinic) {
        return """
                Dear %s,

                Your appointment at %s is confirmed.

                  Appointment number : %s
                  Date               : %s
                  Time               : %s
                  Dentist            : %s
                  Treatment          : %s

                Please arrive ten minutes early and bring this appointment number with you.
                To reschedule or cancel, call us on %s.

                %s
                %s
                """.formatted(patientName, clinic.getName(), appointmentNumber, date, time,
                dentistName, treatment, clinic.getPhone(), clinic.getName(), clinic.getAddress());
    }
}
