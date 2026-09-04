package com.sunrise.dental.service;

import com.sunrise.dental.config.ClinicProperties;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.exception.DoubleBookingException;
import com.sunrise.dental.exception.OutsideOpeningHoursException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The clinic's booking rules, isolated from persistence and from HTTP.
 *
 * <p>Kept as its own class with no repository dependency for two reasons: the rules are the part
 * of the system most likely to change as the clinic's policy changes, and holding them here means
 * they can be unit tested against plain objects, including the boundary cases (back-to-back
 * bookings, a treatment finishing exactly at closing time) that are impractical to cover by
 * clicking through the UI.</p>
 */
@Component
public class AppointmentSchedulingRules {

    private final ClinicProperties clinic;

    public AppointmentSchedulingRules(ClinicProperties clinic) {
        this.clinic = clinic;
    }

    /**
     * Checks a proposed appointment against opening hours and the dentist's existing diary.
     *
     * @param candidate the appointment being booked
     * @param sameDentistSameDay the dentist's other appointments that day, cancelled ones included
     * @throws OutsideOpeningHoursException if the slot falls outside opening hours
     * @throws DoubleBookingException       if the slot overlaps an active appointment
     */
    public void validate(Appointment candidate, List<Appointment> sameDentistSameDay) {
        int minutes = candidate.getTreatmentType().getDurationMinutes() * candidate.getSessionCount();
        if (!isWithinOpeningHours(candidate.getAppointmentTime(), minutes)) {
            throw new OutsideOpeningHoursException(
                    "The clinic is open %s to %s. A %d minute treatment starting at %s would fall outside that, or finish after closing."
                            .formatted(clinic.getOpeningTime(), clinic.getClosingTime(), minutes,
                                    candidate.getAppointmentTime()));
        }

        for (Appointment existing : sameDentistSameDay) {
            if (!existing.isActive()) {
                continue; // a cancelled or no-show slot is free again
            }
            if (existing.getAppointmentNumber().equals(candidate.getAppointmentNumber())) {
                continue; // rescheduling itself is not a clash
            }
            if (overlaps(candidate, existing)) {
                throw new DoubleBookingException(
                        "%s is already booked from %s to %s by appointment %s. Please choose another slot."
                                .formatted(existing.getDentist().getName(),
                                        existing.getAppointmentTime(),
                                        existing.getEndsAt().toLocalTime(),
                                        existing.getAppointmentNumber()));
            }
        }
    }

    /**
     * True when a treatment of the given length both starts at or after opening time and
     * finishes at or before closing time. Opening time is inclusive: 08:00 is bookable.
     */
    public boolean isWithinOpeningHours(LocalTime start, int durationMinutes) {
        if (start.isBefore(clinic.getOpeningTime())) {
            return false;
        }
        LocalTime end = start.plusMinutes(durationMinutes);
        // A treatment ending exactly at closing time is acceptable; one ending after is not.
        return !end.isAfter(clinic.getClosingTime()) && end.isAfter(start);
    }

    /**
     * Half-open interval comparison: two slots overlap only if each starts before the other
     * ends. Back-to-back appointments therefore do not count as a clash.
     */
    private boolean overlaps(Appointment a, Appointment b) {
        LocalDateTime aStart = a.getStartsAt();
        LocalDateTime aEnd = a.getEndsAt();
        LocalDateTime bStart = b.getStartsAt();
        LocalDateTime bEnd = b.getEndsAt();
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
