package com.sunrise.dental.service.pricing;

import com.sunrise.dental.domain.Appointment;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Default rule: the patient pays the base cost with no adjustment. Used for routine
 * treatments such as a scaling or a check-up, and as the factory's fallback when a treatment
 * has no rule of its own.
 */
@Component
public class StandardPricing implements PricingStrategy {

    public static final String CODE = "STANDARD";

    @Override
    public String supportedTreatmentCode() {
        return CODE;
    }

    @Override
    public PriceAdjustment calculate(BigDecimal baseCost, Appointment appointment) {
        return PriceAdjustment.none();
    }
}
