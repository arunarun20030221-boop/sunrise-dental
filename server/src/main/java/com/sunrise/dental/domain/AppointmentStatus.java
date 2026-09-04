package com.sunrise.dental.domain;

/**
 * Lifecycle of an appointment. Cancelled appointments are retained rather than deleted so
 * that the no-show and cancellation reports have data to work with.
 */
public enum AppointmentStatus {
    BOOKED,
    ATTENDED,
    CANCELLED,
    NO_SHOW
}
