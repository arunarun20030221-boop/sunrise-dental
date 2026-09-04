package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Staff;
import com.sunrise.dental.domain.StaffRole;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcStaffDao implements StaffDao {

    private static final RowMapper<Staff> MAPPER = (rs, rowNum) -> {
        Staff staff = new Staff();
        staff.setId(rs.getLong("id"));
        staff.setUsername(rs.getString("username"));
        staff.setPasswordHash(rs.getString("password_hash"));
        staff.setFullName(rs.getString("full_name"));
        staff.setRole(StaffRole.valueOf(rs.getString("role")));
        staff.setEnabled(rs.getBoolean("enabled"));
        return staff;
    };

    private final JdbcTemplate jdbc;

    public JdbcStaffDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Staff> findByUsername(String username) {
        return jdbc.query("SELECT * FROM staff WHERE username = ?", MAPPER, username)
                .stream().findFirst();
    }
}
