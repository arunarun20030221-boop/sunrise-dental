package com.sunrise.dental.exception;

/**
 * Raised when a requested slot overlaps an appointment the dentist already has.
 *
 * <p>This is the defect the clinic's paper diary was causing, so it is enforced in the business
 * tier rather than left to the person at the front desk to notice.</p>
 */
public class DoubleBookingException extends RuntimeException {

    public DoubleBookingException(String message) {
        super(message);
    }
}
