package com.sunrise.dental.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Incoming appointment registration, carrying the fields the brief lists for a new patient.
 *
 * <p>Validation lives on the DTO rather than on the domain object so that bad input is rejected
 * at the edge of the system, before any business logic or database work happens, and so that the
 * same constraints apply identically to the web form and to the REST endpoint used by the
 * console client.</p>
 */
public record AppointmentRequest(

        @NotBlank(message = "Patient name is required")
        @Size(max = 100, message = "Patient name must be 100 characters or fewer")
        String patientName,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must be 255 characters or fewer")
        String address,

        // Sri Lankan mobile and landline formats, optionally with the +94 country code.
        @NotBlank(message = "Contact number is required")
        @Pattern(regexp = "^(?:\\+94|0)\\d{9}$",
                message = "Contact number must be 10 digits starting 0, or +94 followed by 9 digits")
        String contactNumber,

        @Email(message = "Email must be a valid address")
        @Size(max = 150)
        String email,

        @NotNull(message = "Please select a dentist")
        Long dentistId,

        @NotNull(message = "Please select a treatment type")
        Long treatmentTypeId,

        // ISO format is required in BOTH directions, and the rendering direction is the one
        // that bites. An <input type="date"> accepts only yyyy-MM-dd and an <input type="time">
        // only HH:mm; given anything else the browser silently shows an empty field. Without
        // this annotation Spring rendered the bound values in the JVM's locale format
        // ("11/12/26", "2:00 PM"), so whenever a booking was refused the receptionist got the
        // form back with the date and time blanked and had to retype them.
        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date cannot be in the past")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate appointmentDate,

        @NotNull(message = "Appointment time is required")
        @DateTimeFormat(pattern = "HH:mm")
        LocalTime appointmentTime,

        @Min(value = 1, message = "At least one session is required")
        @Max(value = 10, message = "A treatment cannot exceed 10 sessions")
        int sessionCount,

        @Size(max = 500, message = "Notes must be 500 characters or fewer")
        String notes) {

    /** Defaults the session count to 1 so the web form can leave the field blank. */
    public int sessionCountOrDefault() {
        return sessionCount < 1 ? 1 : sessionCount;
    }
}
