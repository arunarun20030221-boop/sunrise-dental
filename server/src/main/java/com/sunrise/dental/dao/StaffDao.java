package com.sunrise.dental.dao;

import com.sunrise.dental.domain.Staff;
import java.util.Optional;

public interface StaffDao {

    Optional<Staff> findByUsername(String username);
}
