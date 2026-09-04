package com.sunrise.dental.dto;

import com.sunrise.dental.domain.Bill;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outgoing bill view, used both by the REST API and by the printable receipt page.
 */
public record BillResponse(
        String billNumber,
        String appointmentNumber,
        String patientName,
        String treatmentType,
        String dentistName,
        BigDecimal consultationFee,
        BigDecimal treatmentCost,
        BigDecimal adjustment,
        String adjustmentReason,
        BigDecimal total,
        Instant issuedAt,
        String issuedBy) {

    public static BillResponse from(Bill bill) {
        return new BillResponse(
                bill.getBillNumber(),
                bill.getAppointment().getAppointmentNumber(),
                bill.getAppointment().getPatient().getName(),
                bill.getAppointment().getTreatmentType().getName(),
                bill.getAppointment().getDentist().getName(),
                bill.getConsultationFee(),
                bill.getTreatmentCost(),
                bill.getAdjustment(),
                bill.getAdjustmentReason(),
                bill.getTotal(),
                bill.getIssuedAt(),
                bill.getIssuedBy());
    }
}
