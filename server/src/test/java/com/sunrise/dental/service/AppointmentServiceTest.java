package com.sunrise.dental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sunrise.dental.config.ClinicProperties;
import com.sunrise.dental.dao.AppointmentDao;
import com.sunrise.dental.dao.PatientDao;
import com.sunrise.dental.dao.ReferenceDataDao;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.Dentist;
import com.sunrise.dental.domain.Patient;
import com.sunrise.dental.domain.TreatmentType;
import com.sunrise.dental.dto.AppointmentRequest;
import com.sunrise.dental.exception.DoubleBookingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

/**
 * Tests for {@link AppointmentService} - the orchestration the other unit tests do not reach.
 *
 * <p>{@code AppointmentSchedulingRulesTest} proves the booking rules are correct in isolation;
 * these tests prove the service wires them up correctly: that the number is drawn in the right
 * order, that a returning patient is reused rather than duplicated, and that a lost race against
 * the database's unique index is reported as a clash rather than as a server error.</p>
 *
 * <p>All five collaborators are mocked. The service contains no arithmetic of its own, so there
 * is nothing here a database would make more realistic - only more fragile and slower.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppointmentService")
class AppointmentServiceTest {

    @Mock private AppointmentDao appointmentDao;
    @Mock private PatientDao patientDao;
    @Mock private ReferenceDataDao referenceDataDao;
    @Mock private NotificationService notificationService;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        ClinicProperties clinic = new ClinicProperties(
                "Sunrise Dental Clinic", "Colombo", "+94112345678", "LKR",
                new BigDecimal("2000.00"), LocalTime.of(8, 0), LocalTime.of(18, 0));

        // The real rules, not a mock: their behaviour is part of what registration means, and
        // stubbing them would leave this test asserting only that mocks were called.
        service = new AppointmentService(
                appointmentDao, patientDao, referenceDataDao,
                new AppointmentSchedulingRules(clinic), notificationService);

        when(referenceDataDao.findDentistById(1L))
                .thenReturn(Optional.of(dentistWithId(1L, "Dr. Silva")));
        when(referenceDataDao.findTreatmentById(2L))
                .thenReturn(Optional.of(treatmentWithId(2L)));
        when(appointmentDao.findByDentistAndDate(anyLong(), any())).thenReturn(List.of());
        when(appointmentDao.nextAppointmentNumber()).thenReturn("APT-2026-000001");
        when(patientDao.insert(any())).thenAnswer(call -> {
            Patient p = call.getArgument(0);
            p.setId(42L);
            return p;
        });
        when(appointmentDao.insert(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("registers an appointment and returns it with its allocated number")
    void registersAppointment() {
        Appointment saved = service.register(validRequest(), "reception1");

        assertThat(saved.getAppointmentNumber()).isEqualTo("APT-2026-000001");
        assertThat(saved.getPatient().getName()).isEqualTo("Nimal Perera");
        assertThat(saved.getDentist().getName()).isEqualTo("Dr. Silva");
        assertThat(saved.getCreatedBy()).isEqualTo("reception1");
    }

    @Test
    @DisplayName("reuses an existing patient matched on contact number instead of duplicating")
    void reusesExistingPatient() {
        Patient existing = new Patient("Nimal P.", "old address", "0771234567", null);
        existing.setId(7L);
        when(patientDao.findByContactNumber("0771234567")).thenReturn(Optional.of(existing));
        when(patientDao.update(any())).thenAnswer(call -> call.getArgument(0));

        service.register(validRequest(), "reception1");

        // Updated, never inserted - a second patient row for the same phone number would split
        // one person's history across two records.
        verify(patientDao).update(any(Patient.class));
        verify(patientDao, never()).insert(any(Patient.class));
        assertThat(existing.getName()).isEqualTo("Nimal Perera");
        assertThat(existing.getAddress()).isEqualTo("12 Galle Road, Colombo 03");
    }

    @Test
    @DisplayName("does not consume an appointment number when the booking is rejected")
    void rejectedBookingDoesNotConsumeANumber() {
        // 19:00 is after the 18:00 close, so the rules reject it.
        AppointmentRequest afterHours = new AppointmentRequest(
                "Late Patient", "Colombo", "0759998887", null, 1L, 2L,
                LocalDate.of(2026, 9, 10), LocalTime.of(19, 0), 1, null);

        assertThatThrownBy(() -> service.register(afterHours, "reception1"))
                .isInstanceOf(RuntimeException.class);

        // The sequence must not have been touched: gaps in the numbers look like lost records.
        verify(appointmentDao, never()).nextAppointmentNumber();
        verify(appointmentDao, never()).insert(any());
    }

    @Test
    @DisplayName("reports a lost race against the unique slot index as a double booking")
    void duplicateKeyFromTheRaceIsReportedAsAClash() {
        // Another member of staff inserted the identical slot between our diary read and our
        // write, so the database's unique index rejects this insert.
        when(appointmentDao.insert(any()))
                .thenThrow(new DuplicateKeyException("uq_appointment_dentist_slot violated"));

        assertThatThrownBy(() -> service.register(validRequest(), "reception1"))
                .isInstanceOf(DoubleBookingException.class)
                .hasMessageContaining("Dr. Silva")
                .hasMessageContaining("just been booked")
                // The staff member needs to know what to do next, not what SQL failed.
                .hasMessageNotContainingAny("uq_appointment", "DuplicateKey", "SQL");
    }

    @Test
    @DisplayName("sends a confirmation only after the appointment is safely stored")
    void confirmationIsSentAfterPersistence() {
        service.register(validRequest(), "reception1");

        // Ordering matters: a confirmation for an appointment that failed to save would tell
        // the patient to turn up for a visit the clinic has no record of.
        var order = org.mockito.Mockito.inOrder(appointmentDao, notificationService);
        order.verify(appointmentDao).insert(any());
        order.verify(notificationService).appointmentConfirmed(any());
    }

    @Test
    @DisplayName("a failed insert means no confirmation is sent")
    void noConfirmationWhenTheInsertFails() {
        when(appointmentDao.insert(any()))
                .thenThrow(new DuplicateKeyException("uq_appointment_dentist_slot violated"));

        assertThatThrownBy(() -> service.register(validRequest(), "reception1"))
                .isInstanceOf(DoubleBookingException.class);

        verify(notificationService, never()).appointmentConfirmed(any());
    }

    private AppointmentRequest validRequest() {
        return new AppointmentRequest(
                "Nimal Perera", "12 Galle Road, Colombo 03", "0771234567", null,
                1L, 2L, LocalDate.of(2026, 9, 10), LocalTime.of(10, 0), 1, "Molar pain");
    }

    private Dentist dentistWithId(Long id, String name) {
        Dentist d = new Dentist(name, "General Dentistry");
        d.setId(id);
        return d;
    }

    private TreatmentType treatmentWithId(Long id) {
        TreatmentType t = new TreatmentType("EXTRACTION", "Tooth Extraction",
                new BigDecimal("6000.00"), 45);
        t.setId(id);
        return t;
    }
}
