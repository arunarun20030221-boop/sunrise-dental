package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Patient;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPatientDao implements PatientDao {

    private static final RowMapper<Patient> MAPPER = (rs, rowNum) -> {
        Patient patient = new Patient();
        patient.setId(rs.getLong("id"));
        patient.setName(rs.getString("name"));
        patient.setAddress(rs.getString("address"));
        patient.setContactNumber(rs.getString("contact_number"));
        patient.setEmail(rs.getString("email"));
        return patient;
    };

    private final JdbcTemplate jdbc;

    public JdbcPatientDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Patient insert(Patient patient) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement(
                    "INSERT INTO patient (name, address, contact_number, email) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, patient.getName());
            ps.setString(2, patient.getAddress());
            ps.setString(3, patient.getContactNumber());
            ps.setString(4, patient.getEmail());
            return ps;
        }, keyHolder);

        patient.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        return patient;
    }

    @Override
    public Patient update(Patient patient) {
        jdbc.update("UPDATE patient SET name = ?, address = ?, email = ? WHERE id = ?",
                patient.getName(), patient.getAddress(), patient.getEmail(), patient.getId());
        return patient;
    }

    @Override
    public Optional<Patient> findByContactNumber(String contactNumber) {
        // A contact number should be unique in practice but is not constrained as such in the
        // schema, so read a list and take the first rather than risk an exception on a duplicate.
        List<Patient> results = jdbc.query(
                "SELECT * FROM patient WHERE contact_number = ? ORDER BY id LIMIT 1",
                MAPPER, contactNumber);
        return results.stream().findFirst();
    }
}
