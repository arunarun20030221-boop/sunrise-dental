package com.sunrise.dental.web;

import com.sunrise.dental.domain.Staff;
import com.sunrise.dental.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Login and logout - requirement 1 of the brief, "the system must require a username and
 * password for secure access".
 */
@Controller
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request, Model model) {
        // Already signed in? Go straight to the diary rather than showing the form again.
        HttpSession existing = request.getSession(false);
        if (existing != null && existing.getAttribute(SessionUser.SESSION_KEY) != null) {
            return "redirect:/appointments";
        }
        model.addAttribute("loggedOut", request.getParameter("loggedOut") != null);
        return "login";
    }

    @PostMapping("/login")
    public String submitLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpServletRequest request,
                              Model model) {

        Optional<Staff> authenticated = authService.authenticate(username, password);

        if (authenticated.isEmpty()) {
            // One message for every failure mode, so the form never reveals whether the
            // username exists.
            model.addAttribute("error", "Invalid username or password.");
            model.addAttribute("username", username);
            return "login";
        }

        // Replace any pre-login session with a fresh one. Reusing the existing session id
        // after a privilege change would leave the application open to session fixation,
        // where an attacker plants a known session id and waits for someone to sign in on it.
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionUser.SESSION_KEY, new SessionUser(authenticated.get()));
        // Sign out after 30 minutes idle: a clinic reception desk is a shared machine.
        session.setMaxInactiveInterval(30 * 60);

        return "redirect:/appointments";
    }

    /** Requirement 6, "Exit System" - ends the session so the next person must sign in. */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?loggedOut";
    }
}
