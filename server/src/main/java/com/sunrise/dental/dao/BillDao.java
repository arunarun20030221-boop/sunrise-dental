package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Bill;
import java.util.Optional;

public interface BillDao {

    Bill insert(Bill bill);

    Optional<Bill> findByBillNumber(String billNumber);

    Optional<Bill> findByAppointmentNumber(String appointmentNumber);

    boolean existsForAppointment(String appointmentNumber);
}
