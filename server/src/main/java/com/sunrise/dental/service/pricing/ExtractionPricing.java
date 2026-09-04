package com.sunrise.dental.service.pricing;

import com.sunrise.dental.domain.Appointment;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * An extraction always requires local anaesthesia, which the clinic charges as a flat
 * additional fee on top of the procedure itself.
 */
@Component
public class ExtractionPricing implements PricingStrategy {

    public static final String CODE = "EXTRACTION";

    private static final BigDecimal ANAESTHESIA_FEE = new BigDecimal("1500.00");

    @Override
    public String supportedTreatmentCode() {
        return CODE;
    }

    @Override
    public PriceAdjustment calculate(BigDecimal baseCost, Appointment appointment) {
        return new PriceAdjustment(ANAESTHESIA_FEE, "Local anaesthesia");
    }
}
