package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Dentist;
import com.sunrise.dental.domain.TreatmentType;
import java.util.List;
import java.util.Optional;

/**
 * Read access to the two lookup tables that populate the booking form.
 *
 * <p>Dentists and treatments share one DAO rather than having one each: both are small,
 * read-mostly reference tables maintained by the clinic rather than by the booking flow, and
 * every caller needs them together to render a form. Splitting them would add an interface
 * without adding a seam anything actually uses.</p>
 */
public interface ReferenceDataDao {

    List<Dentist> findActiveDentists();

    Optional<Dentist> findDentistById(Long id);

    List<TreatmentType> findAllTreatments();

    Optional<TreatmentType> findTreatmentById(Long id);
}
