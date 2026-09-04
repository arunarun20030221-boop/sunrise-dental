package com.sunrise.dental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunrise.dental.config.ClinicProperties;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.Dentist;
import com.sunrise.dental.domain.Patient;
import com.sunrise.dental.domain.TreatmentType;
import com.sunrise.dental.exception.DoubleBookingException;
import com.sunrise.dental.exception.OutsideOpeningHoursException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the scheduling rules that replace the clinic's paper diary: no overlapping
 * bookings for the same dentist, and nothing booked outside opening hours.
 *
 * <p>Written before {@link AppointmentSchedulingRules} existed. These rules are deliberately a
 * separate, dependency-free class rather than logic buried inside the service, precisely so
 * they can be tested exhaustively like this - the boundary cases below (back-to-back bookings,
 * a treatment that runs past closing time) are the ones a manual test would miss.</p>
 */
@DisplayName("AppointmentSchedulingRules")
class AppointmentSchedulingRulesTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);

    private AppointmentSchedulingRules rules;
    private Dentist dentist;
    private TreatmentType thirtyMinuteTreatment;

    @BeforeEach
    void setUp() {
        ClinicProperties clinic = new ClinicProperties(
                "Sunrise Dental Clinic", "Colombo", "+94112345678", "LKR",
                new BigDecimal("2000.00"), LocalTime.of(8, 0), LocalTime.of(18, 0));
        rules = new AppointmentSchedulingRules(clinic);
        dentist = new Dentist("Dr. Silva", "General Dentistry");
        thirtyMinuteTreatment = new TreatmentType("STANDARD", "Check-up", new BigDecimal("3500.00"), 30);
    }

    @Test
    @DisplayName("accepts a slot with no existing appointments")
    void acceptsSlotWithNoConflicts() {
        assertThatCode(() -> rules.validate(candidateAt(LocalTime.of(10, 0)), List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a slot that starts during an existing appointment")
    void rejectsOverlappingStart() {
        Appointment existing = appointmentAt(LocalTime.of(10, 0), thirtyMinuteTreatment, 1);

        assertThatThrownBy(() -> rules.validate(candidateAt(LocalTime.of(10, 15)), List.of(existing)))
                .isInstanceOf(DoubleBookingException.class)
                .hasMessageContaining("Dr. Silva");
    }

    @Test
    @DisplayName("rejects a slot that an existing appointment starts during")
    void rejectsOverlappingEnd() {
        Appointment existing = appointmentAt(LocalTime.of(10, 15), thirtyMinuteTreatment, 1);

        assertThatThrownBy(() -> rules.validate(candidateAt(LocalTime.of(10, 0)), List.of(existing)))
                .isInstanceOf(DoubleBookingException.class);
    }

    @Test
    @DisplayName("accepts back-to-back appointments that touch but do not overlap")
    void acceptsBackToBackBookings() {
        // Existing runs 10:00-10:30; the new one starts exactly at 10:30.
        Appointment existing = appointmentAt(LocalTime.of(10, 0), thirtyMinuteTreatment, 1);

        assertThatCode(() -> rules.validate(candidateAt(LocalTime.of(10, 30)), List.of(existing)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("accounts for multi-session treatments when detecting overlap")
    void multiSessionTreatmentBlocksLongerWindow() {
        // A 60-minute root canal booked for 2 sessions occupies 10:00-12:00.
        TreatmentType rootCanal = new TreatmentType("ROOT_CANAL", "Root Canal", new BigDecimal("15000.00"), 60);
        Appointment existing = appointmentAt(LocalTime.of(10, 0), rootCanal, 2);

        assertThatThrownBy(() -> rules.validate(candidateAt(LocalTime.of(11, 30)), List.of(existing)))
                .isInstanceOf(DoubleBookingException.class);
    }

    @Test
    @DisplayName("rejects a booking before the clinic opens")
    void rejectsBookingBeforeOpening() {
        assertThatThrownBy(() -> rules.validate(candidateAt(LocalTime.of(7, 30)), List.of()))
                .isInstanceOf(OutsideOpeningHoursException.class);
    }

    @Test
    @DisplayName("rejects a treatment that would run past closing time")
    void rejectsTreatmentFinishingAfterClosing() {
        // 17:45 + 30 minutes finishes at 18:15, after the 18:00 close.
        assertThatThrownBy(() -> rules.validate(candidateAt(LocalTime.of(17, 45)), List.of()))
                .isInstanceOf(OutsideOpeningHoursException.class)
                .hasMessageContaining("closing");
    }

    @Test
    @DisplayName("ignores cancelled appointments when detecting overlap")
    void cancelledAppointmentsDoNotBlockTheSlot() {
        Appointment cancelled = appointmentAt(LocalTime.of(10, 0), thirtyMinuteTreatment, 1);
        cancelled.cancel();

        assertThatCode(() -> rules.validate(candidateAt(LocalTime.of(10, 0)), List.of(cancelled)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reports the conflicting appointment number so staff can offer an alternative")
    void conflictMessageNamesTheClashingAppointment() {
        Appointment existing = appointmentAt(LocalTime.of(10, 0), thirtyMinuteTreatment, 1);

        assertThatThrownBy(() -> rules.validate(candidateAt(LocalTime.of(10, 10)), List.of(existing)))
                .hasMessageContaining("APT-2026-000001");
    }

    @Test
    @DisplayName("a slot exactly at opening time is allowed")
    void openingTimeIsInclusive() {
        assertThat(rules.isWithinOpeningHours(LocalTime.of(8, 0), 30)).isTrue();
    }

    private Appointment candidateAt(LocalTime time) {
        return new Appointment("APT-2026-000002", patient(), dentist, thirtyMinuteTreatment,
                DATE, time, 1, "reception1");
    }

    private Appointment appointmentAt(LocalTime time, TreatmentType treatment, int sessions) {
        return new Appointment("APT-2026-000001", patient(), dentist, treatment,
                DATE, time, sessions, "reception1");
    }

    private Patient patient() {
        return new Patient("Nimal Perera", "12 Galle Road, Colombo 03", "0771234567", null);
    }
}
