package com.sunrise.dental.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An issued bill for one appointment.
 *
 * <p>The individual amounts are stored rather than recomputed on read, so that a reprinted
 * receipt always shows what the patient was actually charged even if the clinic later changes
 * its price list.</p>
 */
public class Bill {
    private Long id;
    private String billNumber;
    private Appointment appointment;
    private BigDecimal consultationFee;
    private BigDecimal treatmentCost;

    /** Signed adjustment produced by the treatment's pricing rule: positive adds, negative discounts. */
    private BigDecimal adjustment;
    private String adjustmentReason;
    private BigDecimal total;
    private Instant issuedAt = Instant.now();
    private String issuedBy;

    public Bill() {
        // JavaBean constructor, used by the DAO layer when building an object from a ResultSet
    }

    public Bill(String billNumber,
                Appointment appointment,
                BigDecimal consultationFee,
                BigDecimal treatmentCost,
                BigDecimal adjustment,
                String adjustmentReason,
                BigDecimal total,
                String issuedBy) {
        this.billNumber = billNumber;
        this.appointment = appointment;
        this.consultationFee = consultationFee;
        this.treatmentCost = treatmentCost;
        this.adjustment = adjustment;
        this.adjustmentReason = adjustmentReason;
        this.total = total;
        this.issuedBy = issuedBy;
    }

    public Long getId() {
        return id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public BigDecimal getAdjustment() {
        return adjustment;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public void setTreatmentCost(BigDecimal treatmentCost) {
        this.treatmentCost = treatmentCost;
    }

    public void setAdjustment(BigDecimal adjustment) {
        this.adjustment = adjustment;
    }

    public void setAdjustmentReason(String adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }
}
