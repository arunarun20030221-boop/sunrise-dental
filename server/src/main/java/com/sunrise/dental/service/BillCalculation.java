package com.sunrise.dental.service;

import java.math.BigDecimal;

/**
 * The itemised result of pricing one appointment, before it is persisted as a {@code Bill}.
 *
 * <p>Kept separate from the {@code Bill} entity so that staff can preview a total on screen
 * without a database write, and so that the calculation can be unit tested without JPA.</p>
 *
 * @param consultationFee  the clinic's standard consultation charge
 * @param treatmentCost    base cost of the treatment from the price list
 * @param adjustment       signed adjustment from the treatment's pricing rule
 * @param adjustmentReason wording to print beside the adjustment, null when there is none
 * @param total            the amount payable
 */
public record BillCalculation(BigDecimal consultationFee,
                              BigDecimal treatmentCost,
                              BigDecimal adjustment,
                              String adjustmentReason,
                              BigDecimal total) {
}
