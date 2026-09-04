package com.sunrise.dental.service.pricing;

import com.sunrise.dental.domain.Appointment;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * A root canal treatment is quoted per session. The price list holds the cost of one session,
 * so any session beyond the first is charged again as an adjustment.
 */
@Component
public class RootCanalPricing implements PricingStrategy {

    public static final String CODE = "ROOT_CANAL";

    @Override
    public String supportedTreatmentCode() {
        return CODE;
    }

    @Override
    public PriceAdjustment calculate(BigDecimal baseCost, Appointment appointment) {
        int additionalSessions = Math.max(0, appointment.getSessionCount() - 1);
        if (additionalSessions == 0) {
            return PriceAdjustment.none();
        }
        BigDecimal amount = baseCost.multiply(BigDecimal.valueOf(additionalSessions));
        return new PriceAdjustment(amount, additionalSessions + " additional session(s)");
    }
}
