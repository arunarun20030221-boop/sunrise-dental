package com.sunrise.dental.dto;

/**
 * One line of the dentist-workload report: chair time booked, broken down by outcome.
 *
 * <p>Lives in the shared model package for the same reason as {@link RevenueRow} - so that
 * neither controller has to name a data-tier class.</p>
 */
public record DentistWorkloadRow(String dentistName,
                                 long booked,
                                 long attended,
                                 long cancelled,
                                 long noShow,
                                 long totalMinutes) {

    public String getDentistName() {
        return dentistName;
    }

    public long getBooked() {
        return booked;
    }

    public long getAttended() {
        return attended;
    }

    public long getCancelled() {
        return cancelled;
    }

    public long getNoShow() {
        return noShow;
    }

    public long getTotalMinutes() {
        return totalMinutes;
    }
}
