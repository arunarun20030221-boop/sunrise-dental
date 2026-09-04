package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Dentist;
import com.sunrise.dental.domain.TreatmentType;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcReferenceDataDao implements ReferenceDataDao {

    private static final RowMapper<Dentist> DENTIST_MAPPER = (rs, rowNum) -> {
        Dentist dentist = new Dentist();
        dentist.setId(rs.getLong("id"));
        dentist.setName(rs.getString("name"));
        dentist.setSpeciality(rs.getString("speciality"));
        dentist.setActive(rs.getBoolean("active"));
        return dentist;
    };

    private static final RowMapper<TreatmentType> TREATMENT_MAPPER = (rs, rowNum) -> {
        TreatmentType treatment = new TreatmentType();
        treatment.setId(rs.getLong("id"));
        treatment.setCode(rs.getString("code"));
        treatment.setName(rs.getString("name"));
        treatment.setBaseCost(rs.getBigDecimal("base_cost"));
        treatment.setDurationMinutes(rs.getInt("duration_minutes"));
        return treatment;
    };

    private final JdbcTemplate jdbc;

    public JdbcReferenceDataDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Dentist> findActiveDentists() {
        return jdbc.query("SELECT * FROM dentist WHERE active = TRUE ORDER BY name", DENTIST_MAPPER);
    }

    @Override
    public Optional<Dentist> findDentistById(Long id) {
        return jdbc.query("SELECT * FROM dentist WHERE id = ?", DENTIST_MAPPER, id)
                .stream().findFirst();
    }

    @Override
    public List<TreatmentType> findAllTreatments() {
        return jdbc.query("SELECT * FROM treatment_type ORDER BY name", TREATMENT_MAPPER);
    }

    @Override
    public Optional<TreatmentType> findTreatmentById(Long id) {
        return jdbc.query("SELECT * FROM treatment_type WHERE id = ?", TREATMENT_MAPPER, id)
                .stream().findFirst();
    }
}
