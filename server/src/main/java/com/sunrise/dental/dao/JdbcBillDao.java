package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Bill;
import java.sql.Statement;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of {@link BillDao}.
 *
 * <p>A bill is always read together with the appointment it belongs to, since a receipt is
 * meaningless without the patient and treatment. The appointment is fetched through
 * {@link AppointmentDao} rather than duplicating its eight-column join here, which keeps one
 * definition of how an appointment is loaded.</p>
 */
@Repository
public class JdbcBillDao implements BillDao {

    private final JdbcTemplate jdbc;
    private final AppointmentDao appointmentDao;

    public JdbcBillDao(JdbcTemplate jdbc, AppointmentDao appointmentDao) {
        this.jdbc = jdbc;
        this.appointmentDao = appointmentDao;
    }

    @Override
    public Bill insert(Bill bill) {
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO bill (bill_number, appointment_id, consultation_fee, treatment_cost,
                                      adjustment, adjustment_reason, total, issued_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, bill.getBillNumber());
            ps.setLong(2, bill.getAppointment().getId());
            ps.setBigDecimal(3, bill.getConsultationFee());
            ps.setBigDecimal(4, bill.getTreatmentCost());
            ps.setBigDecimal(5, bill.getAdjustment());
            ps.setString(6, bill.getAdjustmentReason());
            ps.setBigDecimal(7, bill.getTotal());
            ps.setString(8, bill.getIssuedBy());
            return ps;
        }, keyHolder);

        bill.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        return bill;
    }

    @Override
    public Optional<Bill> findByBillNumber(String billNumber) {
        return jdbc.query("SELECT * FROM bill WHERE bill_number = ?", this::mapRow, billNumber)
                .stream().findFirst();
    }

    @Override
    public Optional<Bill> findByAppointmentNumber(String appointmentNumber) {
        return jdbc.query("""
                SELECT b.* FROM bill b
                         JOIN appointment a ON a.id = b.appointment_id
                WHERE a.appointment_number = ?
                """, this::mapRow, appointmentNumber).stream().findFirst();
    }

    @Override
    public boolean existsForAppointment(String appointmentNumber) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM bill b
                         JOIN appointment a ON a.id = b.appointment_id
                WHERE a.appointment_number = ?
                """, Integer.class, appointmentNumber);
        return count != null && count > 0;
    }

    /** Not static, because it needs the injected AppointmentDao to load the appointment. */
    private Bill mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Bill bill = new Bill();
        bill.setId(rs.getLong("id"));
        bill.setBillNumber(rs.getString("bill_number"));
        bill.setConsultationFee(rs.getBigDecimal("consultation_fee"));
        bill.setTreatmentCost(rs.getBigDecimal("treatment_cost"));
        bill.setAdjustment(rs.getBigDecimal("adjustment"));
        bill.setAdjustmentReason(rs.getString("adjustment_reason"));
        bill.setTotal(rs.getBigDecimal("total"));
        var issuedAt = rs.getTimestamp("issued_at");
        if (issuedAt != null) {
            bill.setIssuedAt(issuedAt.toInstant());
        }
        bill.setIssuedBy(rs.getString("issued_by"));

        String appointmentNumber = jdbc.queryForObject(
                "SELECT appointment_number FROM appointment WHERE id = ?",
                String.class, rs.getLong("appointment_id"));
        appointmentDao.findByAppointmentNumber(appointmentNumber).ifPresent(bill::setAppointment);
        return bill;
    }
}
