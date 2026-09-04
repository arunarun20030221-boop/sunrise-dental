package com.sunrise.dental.service;

import com.sunrise.dental.dao.BillDao;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.Bill;
import com.sunrise.dental.exception.BillAlreadyIssuedException;
import com.sunrise.dental.exception.NotFoundException;
import com.sunrise.dental.service.pricing.PriceAdjustment;
import com.sunrise.dental.service.pricing.PricingStrategy;
import com.sunrise.dental.service.pricing.TreatmentPricingFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business tier: turns an appointment into a payable bill.
 *
 * <p>This class holds no pricing rules of its own. It asks {@link TreatmentPricingFactory} for
 * the rule that applies to the appointment's treatment and applies whatever adjustment comes
 * back, which is why adding a new treatment never requires editing this class.</p>
 *
 * <p>All money is handled as {@link BigDecimal} at a fixed scale of two, never as {@code double}.
 * Binary floating point cannot represent decimal currency amounts exactly, and a clinic bill that
 * is a cent out is a defect - this is the single most important correctness decision in the
 * billing code.</p>
 */
@Service
public class BillingService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final BillDao billDao;
    private final TreatmentPricingFactory pricingFactory;
    private final BigDecimal consultationFee;

    public BillingService(BillDao billDao,
                          TreatmentPricingFactory pricingFactory,
                          @Value("${clinic.consultation-fee}") BigDecimal consultationFee) {
        this.billDao = billDao;
        this.pricingFactory = pricingFactory;
        this.consultationFee = money(consultationFee);
    }

    /**
     * Prices an appointment without writing anything, so that staff can see the total before
     * committing to issue the bill.
     */
    public BillCalculation calculate(Appointment appointment) {
        BigDecimal treatmentCost = money(appointment.getTreatmentType().getBaseCost());

        PricingStrategy strategy = pricingFactory.strategyFor(appointment.getTreatmentType().getCode());
        PriceAdjustment adjustment = strategy.calculate(treatmentCost, appointment);
        BigDecimal adjustmentAmount = money(adjustment.amount());

        BigDecimal total = money(consultationFee.add(treatmentCost).add(adjustmentAmount));

        return new BillCalculation(
                consultationFee,
                treatmentCost,
                adjustmentAmount,
                adjustment.reason(),
                total);
    }

    /**
     * Prices the appointment and persists the result as an immutable record of what the patient
     * was charged.
     *
     * @throws BillAlreadyIssuedException if this appointment has been billed before
     */
    @Transactional
    public Bill issue(Appointment appointment, String issuedBy) {
        if (billDao.existsForAppointment(appointment.getAppointmentNumber())) {
            throw new BillAlreadyIssuedException(appointment.getAppointmentNumber());
        }

        BillCalculation calculation = calculate(appointment);

        Bill bill = new Bill(
                nextBillNumber(appointment),
                appointment,
                calculation.consultationFee(),
                calculation.treatmentCost(),
                calculation.adjustment(),
                calculation.adjustmentReason(),
                calculation.total(),
                issuedBy);

        return billDao.insert(bill);
    }

    /** Retrieves an already-issued bill, for reprinting a receipt. */
    @Transactional(readOnly = true)
    public Bill findByAppointmentNumber(String appointmentNumber) {
        return billDao.findByAppointmentNumber(appointmentNumber)
                .orElseThrow(() -> new NotFoundException(
                        "No bill has been issued for appointment " + appointmentNumber));
    }

    /** Whether this appointment has been billed, so the UI can offer print rather than issue. */
    @Transactional(readOnly = true)
    public boolean hasBill(String appointmentNumber) {
        return billDao.existsForAppointment(appointmentNumber);
    }

    /**
     * Derives the bill number from the appointment number so the two are easy to reconcile on
     * paper, then confirms the result is not already taken before returning it.
     */
    private String nextBillNumber(Appointment appointment) {
        String digits = appointment.getAppointmentNumber().replaceAll("\\D", "");
        String candidate = "BILL-" + digits;
        int reissue = 1;
        while (billDao.findByBillNumber(candidate).isPresent()) {
            candidate = "BILL-" + digits + "-R" + reissue++;
        }
        return candidate;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
