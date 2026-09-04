package com.sunrise.dental.dao;

import com.sunrise.dental.dto.DentistWorkloadRow;
import com.sunrise.dental.dto.RevenueRow;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reporting queries, executed against the stored functions declared in {@code db/routines.sql}.
 *
 * <p>A concrete class rather than an interface plus implementation: unlike the other DAOs there
 * is no business logic above this that needs to be tested against a substitute, and the value of
 * these methods is precisely that they delegate to SQL. Pushing the aggregation into the database
 * means the application transfers a handful of summary rows instead of every bill in the period.</p>
 */
@Repository
public class ReportDao {

    private final JdbcTemplate jdbc;

    public ReportDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Which treatments earn the clinic money over a period. */
    public List<RevenueRow> revenueByTreatment(LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT * FROM revenue_by_treatment(?, ?)",
                (rs, rowNum) -> new RevenueRow(
                        rs.getString("treatment_name"),
                        rs.getLong("appointment_count"),
                        rs.getBigDecimal("total_revenue"),
                        rs.getBigDecimal("average_bill")),
                from, to);
    }

    /** Who is overbooked and who has capacity. */
    public List<DentistWorkloadRow> dentistWorkload(LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT * FROM dentist_workload(?, ?)",
                (rs, rowNum) -> new DentistWorkloadRow(
                        rs.getString("dentist_name"),
                        rs.getLong("booked"),
                        rs.getLong("attended"),
                        rs.getLong("cancelled"),
                        rs.getLong("no_show"),
                        rs.getLong("total_minutes")),
                from, to);
    }
}
