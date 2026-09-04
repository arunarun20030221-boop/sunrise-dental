package com.sunrise.dental.service;

import com.sunrise.dental.dao.AppointmentDao;
import com.sunrise.dental.dao.PatientDao;
import com.sunrise.dental.dao.ReferenceDataDao;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.Dentist;
import com.sunrise.dental.domain.Patient;
import com.sunrise.dental.domain.TreatmentType;
import com.sunrise.dental.dto.AppointmentRequest;
import com.sunrise.dental.exception.DoubleBookingException;
import com.sunrise.dental.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business tier for appointments: registration, lookup and status changes.
 *
 * <p>The controllers above hold no rules and the DAOs below hold no rules - everything that
 * decides whether a booking is allowed happens here or in {@link AppointmentSchedulingRules}.
 * That is what makes the three tiers genuinely separate rather than separate in name only.</p>
 */
@Service
public class AppointmentService {

    /**
     * Placeholder carried by a candidate appointment while the scheduling rules examine it,
     * before a real number is drawn from the sequence. It never reaches the database, and it
     * cannot collide with a stored appointment number because those all begin "APT-".
     */
    private static final String UNASSIGNED_NUMBER = "(pending)";

    private final AppointmentDao appointmentDao;
    private final PatientDao patientDao;
    private final ReferenceDataDao referenceDataDao;
    private final AppointmentSchedulingRules schedulingRules;
    private final NotificationService notificationService;

    public AppointmentService(AppointmentDao appointmentDao,
                              PatientDao patientDao,
                              ReferenceDataDao referenceDataDao,
                              AppointmentSchedulingRules schedulingRules,
                              NotificationService notificationService) {
        this.appointmentDao = appointmentDao;
        this.patientDao = patientDao;
        this.referenceDataDao = referenceDataDao;
        this.schedulingRules = schedulingRules;
        this.notificationService = notificationService;
    }

    /**
     * Registers a new appointment.
     *
     * <p>Transactional across both writes: the patient row and the appointment row must either
     * both appear or neither. Without that, a failure between the two would leave an orphaned
     * patient record and the receptionist would have no idea whether to retry.</p>
     */
    @Transactional
    public Appointment register(AppointmentRequest request, String registeredBy) {
        Dentist dentist = referenceDataDao.findDentistById(request.dentistId())
                .orElseThrow(() -> new NotFoundException("Unknown dentist: " + request.dentistId()));
        TreatmentType treatment = referenceDataDao.findTreatmentById(request.treatmentTypeId())
                .orElseThrow(() -> new NotFoundException("Unknown treatment: " + request.treatmentTypeId()));

        Patient patient = findOrCreatePatient(request);

        Appointment appointment = new Appointment(
                UNASSIGNED_NUMBER,
                patient,
                dentist,
                treatment,
                request.appointmentDate(),
                request.appointmentTime(),
                request.sessionCountOrDefault(),
                registeredBy);
        appointment.setNotes(request.notes());

        // Check the proposed slot against the dentist's existing diary for that day.
        List<Appointment> sameDay =
                appointmentDao.findByDentistAndDate(dentist.getId(), request.appointmentDate());
        schedulingRules.validate(appointment, sameDay);

        // Only now draw a real appointment number. Taking it before validation would burn a
        // sequence value on every rejected booking, so the numbers staff see would jump
        // (000001 then 000004) and look as though records had been lost.
        appointment.setAppointmentNumber(appointmentDao.nextAppointmentNumber());

        Appointment saved;
        try {
            saved = appointmentDao.insert(appointment);
        } catch (DuplicateKeyException raceLost) {
            // The unique index on (dentist, date, time) rejected this insert, which means
            // another receptionist took the identical slot between our diary read above and
            // this write. The rules did not fail - they were simply working from a diary that
            // was already stale. Report it as the same clash the user would have seen a moment
            // earlier, rather than as a database error they can do nothing about.
            throw new DoubleBookingException(
                    "%s has just been booked for %s at %s by another member of staff. Please choose another slot."
                            .formatted(dentist.getName(), request.appointmentDate(),
                                    request.appointmentTime()));
        }

        notificationService.appointmentConfirmed(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Appointment findByNumber(String appointmentNumber) {
        return appointmentDao.findByAppointmentNumber(appointmentNumber)
                .orElseThrow(() -> NotFoundException.appointment(appointmentNumber));
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByDate(LocalDate date) {
        return appointmentDao.findByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Appointment> searchByPatientName(String name) {
        return appointmentDao.searchByPatientName(name);
    }

    @Transactional(readOnly = true)
    public List<Dentist> activeDentists() {
        return referenceDataDao.findActiveDentists();
    }

    @Transactional(readOnly = true)
    public List<TreatmentType> treatments() {
        return referenceDataDao.findAllTreatments();
    }

    @Transactional
    public Appointment cancel(String appointmentNumber) {
        Appointment appointment = findByNumber(appointmentNumber);
        appointment.cancel();
        appointmentDao.updateStatus(appointment);
        return appointment;
    }

    @Transactional
    public Appointment markAttended(String appointmentNumber) {
        Appointment appointment = findByNumber(appointmentNumber);
        appointment.markAttended();
        appointmentDao.updateStatus(appointment);
        return appointment;
    }

    @Transactional
    public Appointment markNoShow(String appointmentNumber) {
        Appointment appointment = findByNumber(appointmentNumber);
        appointment.markNoShow();
        appointmentDao.updateStatus(appointment);
        return appointment;
    }

    /**
     * A patient is identified by contact number - the clinic's own convention, since two
     * patients may share a name but not a phone. Details are refreshed on each visit so a
     * change of address is captured rather than a duplicate record created.
     */
    private Patient findOrCreatePatient(AppointmentRequest request) {
        return patientDao.findByContactNumber(request.contactNumber())
                .map(existing -> {
                    existing.setName(request.patientName());
                    existing.setAddress(request.address());
                    if (request.email() != null && !request.email().isBlank()) {
                        existing.setEmail(request.email());
                    }
                    return patientDao.update(existing);
                })
                .orElseGet(() -> patientDao.insert(new Patient(
                        request.patientName(),
                        request.address(),
                        request.contactNumber(),
                        request.email())));
    }
}
