package com.sunrise.dental.exception;

/**
 * Raised when a requested appointment time falls outside the clinic's opening hours, or when
 * the treatment would not finish before closing.
 */
public class OutsideOpeningHoursException extends RuntimeException {

    public OutsideOpeningHoursException(String message) {
        super(message);
    }
}
