package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Appointment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DAO pattern - the data-access contract for appointments.
 *
 * <p>Declared as an interface rather than a concrete class so that the business tier depends on
 * the operations it needs, not on JDBC. That is what lets the service tests run against a mock
 * with no database, and it is what would let the clinic move to a different store without
 * touching a line of business logic.</p>
 */
public interface AppointmentDao {

    Appointment insert(Appointment appointment);

    void updateStatus(Appointment appointment);

    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    List<Appointment> findByDate(LocalDate date);

    /**
     * A dentist's appointments on one day, cancelled ones included, so the scheduling rules can
     * decide for themselves which occupy the chair.
     */
    List<Appointment> findByDentistAndDate(Long dentistId, LocalDate date);

    List<Appointment> searchByPatientName(String partialName);

    String nextAppointmentNumber();
}
