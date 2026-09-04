package com.sunrise.dental.service;

import com.sunrise.dental.dao.ReportDao;
import com.sunrise.dental.dto.DentistWorkloadRow;
import com.sunrise.dental.dto.RevenueRow;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business tier for the management reports.
 *
 * <p>Owns the date-range defaulting and validation so that neither the web controller nor the
 * API controller has to repeat it, and so a caller cannot ask the database for an inverted
 * range.</p>
 */
@Service
public class ReportService {

    private final ReportDao reportDao;

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    @Transactional(readOnly = true)
    public List<RevenueRow> revenueByTreatment(LocalDate from, LocalDate to) {
        DateRange range = DateRange.of(from, to);
        return reportDao.revenueByTreatment(range.from(), range.to());
    }

    @Transactional(readOnly = true)
    public List<DentistWorkloadRow> dentistWorkload(LocalDate from, LocalDate to) {
        DateRange range = DateRange.of(from, to);
        return reportDao.dentistWorkload(range.from(), range.to());
    }

    /**
     * A reporting period.
     *
     * <p>Missing bounds default to the <em>current calendar month</em> rather than to the last
     * 30 days. A clinic diary is mostly forward-looking: a trailing window shows zeros for
     * every appointment booked for later this month, which reads as though the reports are
     * broken. The month covers both bills already taken and bookings still to come, which is
     * what a clinic manager actually reviews.</p>
     *
     * <p>Inverted bounds are swapped rather than passed through, since a range whose start is
     * after its end would silently return nothing at all.</p>
     */
    public record DateRange(LocalDate from, LocalDate to) {

        public static DateRange of(LocalDate from, LocalDate to) {
            if (from == null && to == null) {
                LocalDate today = LocalDate.now();
                return new DateRange(today.withDayOfMonth(1),
                        today.withDayOfMonth(today.lengthOfMonth()));
            }
            LocalDate end = to != null ? to : from.withDayOfMonth(from.lengthOfMonth());
            LocalDate start = from != null ? from : end.withDayOfMonth(1);
            return start.isAfter(end) ? new DateRange(end, start) : new DateRange(start, end);
        }
    }
}
