package com.sunrise.dental.web;

import com.sunrise.dental.domain.Staff;
import com.sunrise.dental.domain.StaffRole;
import java.io.Serializable;

/**
 * The signed-in staff member, as held in the {@code HttpSession}.
 *
 * <p>A deliberately small copy rather than the {@link Staff} object itself: the session must not
 * carry the password hash around, and storing only what the UI needs keeps the session small.
 * {@link Serializable} because a servlet container may persist sessions across a restart.</p>
 */
public class SessionUser implements Serializable {

    /** Session attribute name, used by the controllers and the interceptor alike. */
    public static final String SESSION_KEY = "sunrise.currentUser";

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String fullName;
    private final StaffRole role;

    public SessionUser(Staff staff) {
        this.username = staff.getUsername();
        this.fullName = staff.getFullName();
        this.role = staff.getRole();
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public StaffRole getRole() {
        return role;
    }

    /** Used by the interceptor to gate the reports, and by the JSPs to hide the menu item. */
    public boolean isAdmin() {
        return role == StaffRole.ADMIN;
    }
}
