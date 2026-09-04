package com.sunrise.dental.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Reads the signed-in staff member for the request in hand.
 *
 * <p>Authentication is handled by {@link AuthInterceptor} rather than by Spring Security, so
 * {@code request.getUserPrincipal()} is never populated and controllers cannot inject a
 * {@code java.security.Principal}. This helper is the single place that knows the user may
 * arrive either in the session (browser) or as a request attribute set by the interceptor
 * (API), so no controller has to care which channel it is serving.</p>
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * @return the signed-in user, never null - the interceptor rejects unauthenticated
     *         requests before any controller runs, so reaching here without a user is a bug
     *         rather than an expected condition
     */
    public static SessionUser require(HttpServletRequest request) {
        SessionUser fromApi = (SessionUser) request.getAttribute(SessionUser.SESSION_KEY);
        if (fromApi != null) {
            return fromApi;
        }
        HttpSession session = request.getSession(false);
        SessionUser fromSession = session == null
                ? null
                : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (fromSession == null) {
            throw new IllegalStateException(
                    "No authenticated user on the request; AuthInterceptor should have rejected it");
        }
        return fromSession;
    }

    /** Convenience for the common case of just needing a username to record on a row. */
    public static String username(HttpServletRequest request) {
        return require(request).getUsername();
    }
}
