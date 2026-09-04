package com.sunrise.dental.domain;

import java.math.BigDecimal;

/**
 * A treatment the clinic offers, with its base cost.
 *
 * <p>The base cost is held in the database so that prices can change without a redeploy.
 * The <em>rules</em> that adjust that base cost differ per treatment (an extraction adds an
 * anaesthesia fee, a root canal is charged per session, cosmetic work adds a materials
 * percentage), and those rules live in the pricing strategies rather than here - see
 * {@code com.sunrise.dental.service.pricing}.</p>
 */
public class TreatmentType {
    private Long id;

    /** Stable code used to select the pricing strategy, e.g. ROOT_CANAL. */
    private String code;
    private String name;
    private BigDecimal baseCost;

    /** Typical chair time, used to detect overlapping bookings for the same dentist. */
    private int durationMinutes = 30;

    public TreatmentType() {
        // JavaBean constructor, used by the DAO layer when building an object from a ResultSet
    }

    public TreatmentType(String code, String name, BigDecimal baseCost, int durationMinutes) {
        this.code = code;
        this.name = name;
        this.baseCost = baseCost;
        this.durationMinutes = durationMinutes;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }
}
