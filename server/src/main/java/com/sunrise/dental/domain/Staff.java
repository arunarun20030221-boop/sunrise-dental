package com.sunrise.dental.domain;

/**
 * A clinic staff member who may log in. The brief restricts the system to authorised staff,
 * so there is no patient self-service account.
 *
 * <p>Only the BCrypt hash of the password is stored - never the password itself. This is the
 * secure-coding requirement called out in the module's ETHICAL skills strand.</p>
 */
public class Staff {
    private Long id;
    private String username;

    /** BCrypt hash, never the plaintext password. */
    private String passwordHash;
    private String fullName;
    private StaffRole role;
    private boolean enabled = true;

    public Staff() {
        // JavaBean constructor, used by the DAO layer when building an object from a ResultSet
    }

    public Staff(String username, String passwordHash, String fullName, StaffRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public StaffRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRole(StaffRole role) {
        this.role = role;
    }
}
