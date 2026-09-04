package com.sunrise.dental.dto;

import com.sunrise.dental.domain.Appointment;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Outgoing appointment view. A dedicated response type keeps domain objects out of the HTTP
 * layer, so a change to the persistence model cannot silently alter the published API contract
 * the console client depends on.
 */
public record AppointmentResponse(
        String appointmentNumber,
        String patientName,
        String address,
        String contactNumber,
        String dentistName,
        String treatmentType,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        int sessionCount,
        String status,
        String notes) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getAppointmentNumber(),
                appointment.getPatient().getName(),
                appointment.getPatient().getAddress(),
                appointment.getPatient().getContactNumber(),
                appointment.getDentist().getName(),
                appointment.getTreatmentType().getName(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getSessionCount(),
                appointment.getStatus().name(),
                appointment.getNotes());
    }
}
