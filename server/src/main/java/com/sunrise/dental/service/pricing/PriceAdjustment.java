package com.sunrise.dental.service.pricing;

import java.math.BigDecimal;

/**
 * The outcome of applying a treatment's pricing rule: a signed amount plus the human-readable
 * reason that gets printed on the patient's bill.
 *
 * @param amount signed adjustment - positive adds to the bill, negative discounts it
 * @param reason wording shown on the receipt, e.g. "Local anaesthesia"
 */
public record PriceAdjustment(BigDecimal amount, String reason) {

    public static PriceAdjustment none() {
        return new PriceAdjustment(BigDecimal.ZERO.setScale(2), null);
    }
}
