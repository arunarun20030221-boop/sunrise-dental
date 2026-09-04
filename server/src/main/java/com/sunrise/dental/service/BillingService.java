package com.sunrise.dental.service;

import com.sunrise.dental.dao.BillDao;
import com.sunrise.dental.domain.Appointment;
import com.sunrise.dental.domain.Bill;
import com.sunrise.dental.service.pricing.TreatmentPricingFactory;
import java.math.BigDecimal;

/**
 * TDD step 1 of 2 - RED.
 *
 * <p>Deliberately unimplemented. BillingServiceTest is written against this API and fails
 * against this stub; the next commit supplies the behaviour that makes it pass. The stub
 * exists so the failing test compiles and can actually be executed, which is what makes the
 * red state demonstrable rather than merely asserted.</p>
 */
public class BillingService {

    public BillingService(BillDao billDao,
                          TreatmentPricingFactory pricingFactory,
                          BigDecimal consultationFee) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public BillCalculation calculate(Appointment appointment) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Bill issue(Appointment appointment, String issuedBy) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
