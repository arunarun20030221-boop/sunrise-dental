package com.sunrise.dental.exception;

/**
 * Raised when staff attempt to issue a second bill for an appointment that has already been
 * billed. Reprinting an existing receipt is allowed; charging twice is not.
 */
public class BillAlreadyIssuedException extends RuntimeException {

    public BillAlreadyIssuedException(String appointmentNumber) {
        super("A bill has already been issued for appointment " + appointmentNumber);
    }
}
