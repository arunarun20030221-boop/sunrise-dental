package com.sunrise.dental.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A patient visit. Each visit carries a unique, human-readable appointment number, which is
 * the identifier clinic staff search by (the brief requires lookup by appointment number).
 */
public class Appointment {
    private Long id;

    /** Unique business identifier, e.g. APT-2026-000001. */
    private String appointmentNumber;
    private Patient patient;
    private Dentist dentist;
    private TreatmentType treatmentType;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    /** How many sessions this treatment needs; used by the root canal pricing rule. */
    private int sessionCount = 1;
    private String notes;
    private Instant createdAt = Instant.now();
    private String createdBy;

    public Appointment() {
        // JavaBean constructor, used by the DAO layer when building an object from a ResultSet
    }

    public Appointment(String appointmentNumber,
                       Patient patient,
                       Dentist dentist,
                       TreatmentType treatmentType,
                       LocalDate appointmentDate,
                       LocalTime appointmentTime,
                       int sessionCount,
                       String createdBy) {
        this.appointmentNumber = appointmentNumber;
        this.patient = patient;
        this.dentist = dentist;
        this.treatmentType = treatmentType;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.sessionCount = sessionCount;
        this.createdBy = createdBy;
    }

    /** Start of the booked slot, used for overlap checks. */
    public LocalDateTime getStartsAt() {
        return LocalDateTime.of(appointmentDate, appointmentTime);
    }

    /**
     * End of the booked slot, derived from the treatment's typical chair time.
     *
     * <p>Named as a JavaBean getter, not {@code endsAt()}, because the appointment details JSP
     * resolves {@code ${appointment.endsAt}} by looking for {@code getEndsAt()}.</p>
     */
    public LocalDateTime getEndsAt() {
        return getStartsAt().plusMinutes((long) treatmentType.getDurationMinutes() * sessionCount);
    }

    public boolean isActive() {
        return status == AppointmentStatus.BOOKED || status == AppointmentStatus.ATTENDED;
    }

    public void markAttended() {
        this.status = AppointmentStatus.ATTENDED;
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
    }

    public void markNoShow() {
        this.status = AppointmentStatus.NO_SHOW;
    }

    public Long getId() {
        return id;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public Patient getPatient() {
        return patient;
    }

    public Dentist getDentist() {
        return dentist;
    }

    public TreatmentType getTreatmentType() {
        return treatmentType;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public void setDentist(Dentist dentist) {
        this.dentist = dentist;
    }

    public void setTreatmentType(TreatmentType treatmentType) {
        this.treatmentType = treatmentType;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
