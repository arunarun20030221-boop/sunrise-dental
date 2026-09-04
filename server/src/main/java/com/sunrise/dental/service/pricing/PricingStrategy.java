package com.sunrise.dental.service.pricing;

import com.sunrise.dental.domain.Appointment;
import java.math.BigDecimal;

/**
 * STRATEGY pattern.
 *
 * <p>Every treatment starts from the base cost held in the database, but the rule that turns
 * that base cost into what the patient actually pays differs by treatment: an extraction adds
 * an anaesthesia fee, a root canal is charged per session, cosmetic work adds a materials
 * percentage. Each rule is a separate implementation of this interface, so the billing service
 * never needs to know which rule applies - it asks the factory and applies whatever comes back.</p>
 *
 * <p>The alternative - a {@code switch} on treatment code inside the billing service - was
 * rejected because every new treatment would then require editing shared billing logic that is
 * already covered by tests. Adding a strategy is additive; editing a switch is not.</p>
 */
public interface PricingStrategy {

    /**
     * The treatment code this rule applies to, matching {@code TreatmentType.code}.
     * The factory uses this to build its lookup table.
     */
    String supportedTreatmentCode();

    /**
     * Calculates the adjustment to apply on top of the base treatment cost.
     *
     * @param baseCost    the treatment's base cost from the price list
     * @param appointment the appointment being billed, for rules that depend on it
     * @return the signed adjustment and the reason to print on the bill
     */
    PriceAdjustment calculate(BigDecimal baseCost, Appointment appointment);
}
