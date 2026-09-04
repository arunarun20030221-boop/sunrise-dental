package com.sunrise.dental.service;

import com.sunrise.dental.dao.StaffDao;
import com.sunrise.dental.domain.Staff;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Verifies staff credentials.
 *
 * <p>The submitted password is compared against the stored BCrypt hash. BCrypt is deliberately
 * slow and salts every hash individually, so two staff with the same password still have
 * different hashes and a leaked {@code staff} table cannot be attacked with a precomputed
 * rainbow table.</p>
 *
 * <p>A failed login reports only that the credentials were wrong, never whether the username
 * exists. Distinguishing the two would let an attacker enumerate valid staff usernames.</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final StaffDao staffDao;
    private final PasswordEncoder passwordEncoder;

    public AuthService(StaffDao staffDao, PasswordEncoder passwordEncoder) {
        this.staffDao = staffDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @return the authenticated staff member, or empty when the username is unknown, the
     *         account is disabled, or the password does not match
     */
    public Optional<Staff> authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }

        Optional<Staff> found = staffDao.findByUsername(username);
        if (found.isEmpty()) {
            // Still run a hash comparison against a dummy value so that a request for an
            // unknown username takes about as long as one for a known username. Returning
            // immediately would leak which usernames exist through response timing.
            passwordEncoder.matches(rawPassword, "$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinvalidinvalidinv");
            log.info("Failed login attempt for unknown username");
            return Optional.empty();
        }

        Staff staff = found.get();
        if (!staff.isEnabled()) {
            log.info("Login attempt for disabled account: {}", username);
            return Optional.empty();
        }
        if (!passwordEncoder.matches(rawPassword, staff.getPasswordHash())) {
            log.info("Failed login attempt for {}", username);
            return Optional.empty();
        }

        log.info("{} signed in", username);
        return Optional.of(staff);
    }
}
