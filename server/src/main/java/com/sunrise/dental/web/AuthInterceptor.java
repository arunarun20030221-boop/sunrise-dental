package com.sunrise.dental.web;

import com.sunrise.dental.domain.Staff;
import com.sunrise.dental.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces the brief's requirement that only authorised staff can use the system.
 *
 * <p>Runs before every controller method. The two presentation channels need different
 * treatment, so the interceptor handles both:</p>
 * <ul>
 *   <li><b>Browser</b> - looks for a {@link SessionUser} in the {@code HttpSession}. An
 *       unauthenticated request is redirected to the login page. The session is identified by
 *       the container's {@code JSESSIONID} cookie, so the browser stays signed in across
 *       requests without resending credentials.</li>
 *   <li><b>Console client</b> - accepts HTTP Basic credentials on {@code /api/**} and verifies
 *       them per request. A command-line client has nowhere sensible to keep a cookie, and a
 *       401 status is something it can react to, whereas an HTML login page is not.</li>
 * </ul>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Routes reachable without signing in. */
    private static final Set<String> PUBLIC_PATHS = Set.of("/login", "/logout", "/error");

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }

        if (path.startsWith("/api/")) {
            return handleApiRequest(request, response, path);
        }

        return handleBrowserRequest(request, response);
    }

    /** Verifies Basic credentials on each API call and enforces the admin-only report routes. */
    private boolean handleApiRequest(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {

        Optional<Staff> staff = staffFromBasicAuth(request);
        if (staff.isEmpty()) {
            // The WWW-Authenticate header tells the client which scheme to use.
            response.setHeader("WWW-Authenticate", "Basic realm=\"Sunrise Dental API\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Valid staff credentials required");
            return false;
        }

        if (path.startsWith("/api/reports") && !new SessionUser(staff.get()).isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Reports are restricted to administrators");
            return false;
        }

        // Make the caller available to controllers via CurrentUser.
        request.setAttribute(SessionUser.SESSION_KEY, new SessionUser(staff.get()));
        return true;
    }

    /** Requires a session for browser routes, and gates the report pages on the admin role. */
    private boolean handleBrowserRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        HttpSession session = request.getSession(false);
        SessionUser user = session == null
                ? null
                : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/reports") && !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Reports are restricted to administrators");
            return false;
        }

        return true;
    }

    /** Decodes an {@code Authorization: Basic base64(user:pass)} header and verifies it. */
    private Optional<Staff> staffFromBasicAuth(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Basic ")) {
            return Optional.empty();
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(header.substring("Basic ".length()).trim()),
                    StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return Optional.empty();
            }
            return authService.authenticate(
                    decoded.substring(0, separator), decoded.substring(separator + 1));
        } catch (IllegalArgumentException malformedBase64) {
            return Optional.empty();
        }
    }
}
