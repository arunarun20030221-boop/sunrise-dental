package com.sunrise.dental.exception;

/**
 * Raised when a lookup by business identifier finds nothing - for example searching for an
 * appointment number that does not exist.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException appointment(String appointmentNumber) {
        return new NotFoundException("No appointment found with number " + appointmentNumber);
    }
}
