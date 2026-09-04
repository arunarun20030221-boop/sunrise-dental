package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.AppointmentStatus;
import com.sunrise.dental.domain.Dentist;
import com.sunrise.dental.domain.Patient;
import com.sunrise.dental.domain.TreatmentType;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link AppointmentDao}.
 *
 * <p>Every statement is a {@link java.sql.PreparedStatement} with bound parameters, never a
 * concatenated string. That is not a style preference: string concatenation is how SQL
 * injection happens, and a patient name is user input that reaches this class directly.</p>
 *
 * <p>Appointments are read back with their patient, dentist and treatment joined in a single
 * query. Fetching them separately per row would issue one query for the list plus three per
 * appointment - the N+1 problem - which is slow and gets slower as the clinic's diary fills.</p>
 */
@Repository
public class JdbcAppointmentDao implements AppointmentDao {

    /** Shared projection so every read returns identically shaped rows for the mapper. */
    private static final String SELECT_BASE = """
            SELECT a.id, a.appointment_number, a.appointment_date, a.appointment_time,
                   a.status, a.session_count, a.notes, a.created_at, a.created_by,
                   p.id AS patient_id, p.name AS patient_name, p.address AS patient_address,
                   p.contact_number AS patient_contact, p.email AS patient_email,
                   d.id AS dentist_id, d.name AS dentist_name, d.speciality AS dentist_speciality,
                   d.active AS dentist_active,
                   t.id AS treatment_id, t.code AS treatment_code, t.name AS treatment_name,
                   t.base_cost AS treatment_base_cost, t.duration_minutes AS treatment_duration
            FROM appointment a
                     JOIN patient p ON p.id = a.patient_id
                     JOIN dentist d ON d.id = a.dentist_id
                     JOIN treatment_type t ON t.id = a.treatment_type_id
            """;

    private final JdbcTemplate jdbc;

    public JdbcAppointmentDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Appointment insert(Appointment appointment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO appointment (appointment_number, patient_id, dentist_id,
                                             treatment_type_id, appointment_date, appointment_time,
                                             status, session_count, notes, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, appointment.getAppointmentNumber());
            ps.setLong(2, appointment.getPatient().getId());
            ps.setLong(3, appointment.getDentist().getId());
            ps.setLong(4, appointment.getTreatmentType().getId());
            ps.setObject(5, appointment.getAppointmentDate());
            ps.setObject(6, appointment.getAppointmentTime());
            ps.setString(7, appointment.getStatus().name());
            ps.setInt(8, appointment.getSessionCount());
            ps.setString(9, appointment.getNotes());
            ps.setString(10, appointment.getCreatedBy());
            return ps;
        }, keyHolder);

        Number generatedId = (Number) keyHolder.getKeys().get("id");
        appointment.setId(generatedId.longValue());
        return appointment;
    }

    @Override
    public void updateStatus(Appointment appointment) {
        jdbc.update("UPDATE appointment SET status = ? WHERE appointment_number = ?",
                appointment.getStatus().name(), appointment.getAppointmentNumber());
    }

    @Override
    public Optional<Appointment> findByAppointmentNumber(String appointmentNumber) {
        List<Appointment> results = jdbc.query(
                SELECT_BASE + " WHERE a.appointment_number = ?", MAPPER, appointmentNumber);
        return results.stream().findFirst();
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        return jdbc.query(SELECT_BASE + " WHERE a.appointment_date = ? ORDER BY a.appointment_time",
                MAPPER, date);
    }

    @Override
    public List<Appointment> findByDentistAndDate(Long dentistId, LocalDate date) {
        return jdbc.query(
                SELECT_BASE + " WHERE a.dentist_id = ? AND a.appointment_date = ? ORDER BY a.appointment_time",
                MAPPER, dentistId, date);
    }

    @Override
    public List<Appointment> searchByPatientName(String partialName) {
        // The wildcards are added to the bound value, not to the SQL, so the input stays data.
        return jdbc.query(
                SELECT_BASE + " WHERE LOWER(p.name) LIKE LOWER(?) ORDER BY a.appointment_date DESC, a.appointment_time DESC",
                MAPPER, "%" + partialName + "%");
    }

    @Override
    public String nextAppointmentNumber() {
        return jdbc.queryForObject("SELECT next_appointment_number()", String.class);
    }

    /**
     * Rebuilds the object graph from one joined row. Kept as a constant so all the queries
     * above share exactly one mapping, rather than each maintaining its own copy.
     */
    private static final RowMapper<Appointment> MAPPER = (ResultSet rs, int rowNum) -> {
        Patient patient = new Patient();
        patient.setId(rs.getLong("patient_id"));
        patient.setName(rs.getString("patient_name"));
        patient.setAddress(rs.getString("patient_address"));
        patient.setContactNumber(rs.getString("patient_contact"));
        patient.setEmail(rs.getString("patient_email"));

        Dentist dentist = new Dentist();
        dentist.setId(rs.getLong("dentist_id"));
        dentist.setName(rs.getString("dentist_name"));
        dentist.setSpeciality(rs.getString("dentist_speciality"));
        dentist.setActive(rs.getBoolean("dentist_active"));

        TreatmentType treatment = new TreatmentType();
        treatment.setId(rs.getLong("treatment_id"));
        treatment.setCode(rs.getString("treatment_code"));
        treatment.setName(rs.getString("treatment_name"));
        treatment.setBaseCost(rs.getBigDecimal("treatment_base_cost"));
        treatment.setDurationMinutes(rs.getInt("treatment_duration"));

        Appointment appointment = new Appointment();
        appointment.setId(rs.getLong("id"));
        appointment.setAppointmentNumber(rs.getString("appointment_number"));
        appointment.setPatient(patient);
        appointment.setDentist(dentist);
        appointment.setTreatmentType(treatment);
        appointment.setAppointmentDate(rs.getObject("appointment_date", LocalDate.class));
        appointment.setAppointmentTime(rs.getObject("appointment_time", java.time.LocalTime.class));
        appointment.setStatus(AppointmentStatus.valueOf(rs.getString("status")));
        appointment.setSessionCount(rs.getInt("session_count"));
        appointment.setNotes(rs.getString("notes"));
        var createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            appointment.setCreatedAt(createdAt.toInstant());
        }
        appointment.setCreatedBy(rs.getString("created_by"));
        return appointment;
    };
}
