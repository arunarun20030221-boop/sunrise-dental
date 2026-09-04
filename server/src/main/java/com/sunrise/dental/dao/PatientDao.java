package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Patient;
import java.util.Optional;

public interface PatientDao {

    Patient insert(Patient patient);

    Patient update(Patient patient);

    Optional<Patient> findByContactNumber(String contactNumber);
}
