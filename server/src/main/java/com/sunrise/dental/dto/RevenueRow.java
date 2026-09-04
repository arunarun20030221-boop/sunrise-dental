package com.sunrise.dental.dto;

import java.math.BigDecimal;

/**
 * One line of the revenue-by-treatment report.
 *
 * <p>Declared here rather than inside {@code ReportDao} on purpose. These rows are part of the
 * contract the web service publishes, and a presentation-tier class must not name a data-tier
 * type - that would let the presentation tier reach past the business tier and collapse the
 * three-tier separation the design depends on.</p>
 */
public record RevenueRow(String treatmentName,
                         long appointmentCount,
                         BigDecimal totalRevenue,
                         BigDecimal averageBill) {

    /** JSP expression language resolves {@code ${row.treatmentName}} through a getter. */
    public String getTreatmentName() {
        return treatmentName;
    }

    public long getAppointmentCount() {
        return appointmentCount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getAverageBill() {
        return averageBill;
    }
}
