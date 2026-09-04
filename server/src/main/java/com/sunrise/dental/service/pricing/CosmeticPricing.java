package com.sunrise.dental.service.pricing;

import com.sunrise.dental.domain.Appointment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Cosmetic work (whitening, veneers) consumes imported materials, which the clinic recovers as
 * a percentage surcharge rather than a flat fee so that it scales with the procedure's cost.
 */
@Component
public class CosmeticPricing implements PricingStrategy {

    public static final String CODE = "COSMETIC";

    private static final BigDecimal MATERIALS_RATE = new BigDecimal("0.15");

    @Override
    public String supportedTreatmentCode() {
        return CODE;
    }

    @Override
    public PriceAdjustment calculate(BigDecimal baseCost, Appointment appointment) {
        BigDecimal amount = baseCost.multiply(MATERIALS_RATE).setScale(2, RoundingMode.HALF_UP);
        return new PriceAdjustment(amount, "Materials surcharge (15%)");
    }
}
